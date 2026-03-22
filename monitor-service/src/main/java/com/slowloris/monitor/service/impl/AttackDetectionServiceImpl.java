package com.slowloris.monitor.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.slowloris.monitor.service.AttackDetectionService;
import com.slowloris.monitor.vo.AttackDetectionRequest;
import com.slowloris.monitor.vo.AttackDetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class AttackDetectionServiceImpl implements AttackDetectionService {

    // DeepSeek 模型配置（从配置文件读取，避免硬编码）
    @Value("${deepseek.api.key:}")
    private String deepseekApiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String deepseekApiUrl;

    @Value("${deepseek.model.name:deepseek-chat}")
    private String deepseekModelName;

    @Autowired
    private RestTemplate restTemplate;

    // Slowloris 攻击核心特征阈值（可配置化）
    private static final int SLOWLORIS_CONNECTION_THRESHOLD = 50;       // 半开连接数阈值
    private static final long CONNECTION_DURATION_THRESHOLD = 300000;   // 连接持续时间阈值（5分钟，毫秒）
    private static final int REQUEST_RATE_THRESHOLD = 5;                // 低请求频率阈值（次/分钟）
    private static final int PACKET_SIZE_THRESHOLD = 1024;              // 小包阈值（字节）
    private static final double CONFIDENCE_THRESHOLD = 0.7;             // 攻击判定置信度阈值

    /**
     * DeepSeek 模型系统提示词（针对 Slowloris 攻击优化）
     * 核心：明确攻击特征、结构化输出要求、限定响应格式
     */
    private static final String DEEPSEEK_SYSTEM_PROMPT = """
            你是一位资深的网络安全专家，专注于检测DDoS攻击中的Slowloris攻击（慢速攻击）。
            请严格按照以下规则分析网络数据并输出结果：
            1. Slowloris攻击特征：
               - 大量半开HTTP连接（partial connections），连接数≥%d
               - 每个连接持续时间长（≥%d毫秒），且发送少量数据（数据包≤%d字节）
               - 请求频率低（≤%d次/分钟），但连接数持续增加
               - 连接不完整，仅发送部分HTTP头，不发送完整请求体
            2. 输出格式：必须是标准JSON，不包含任何额外文本、注释或换行
            3. JSON字段说明：
               - isAttack: 布尔值，true=检测到攻击，false=正常
               - attackType: 字符串，"slowloris"/"normal"/"other_ddos"
               - severity: 字符串，"low"/"medium"/"high"/"critical"（Slowloris攻击默认high/critical）
               - confidence: 浮点数，0.0-1.0（攻击判定置信度）
               - riskScore: 浮点数，0-100（风险分数，Slowloris攻击≥70）
               - reasoning: 字符串，详细说明判断依据（需列出具体特征匹配情况）
               - recommendations: 数组，给出针对性防御建议（至少2条）
               - details: 对象，包含检测维度的具体数值：
                 - request_rate: 请求频率（次/分钟）
                 - connection_count: 半开连接数
                 - connection_duration: 平均连接持续时间（毫秒）
                 - packet_size: 平均数据包大小（字节）
                 - abnormal_indicators: 数组，列出异常指标（如["连接数超标","数据包过小"]）
            """.formatted(
            SLOWLORIS_CONNECTION_THRESHOLD,
            CONNECTION_DURATION_THRESHOLD,
            PACKET_SIZE_THRESHOLD,
            REQUEST_RATE_THRESHOLD
    );

    @Override
    public AttackDetectionResult detectAttack(AttackDetectionRequest request) {
        // 1. 入参校验
        if (request == null || !StringUtils.hasText(request.getIpAddress())) {
            log.warn("攻击检测请求参数非法：IP地址为空");
            return createDefaultResult(false, "normal", "low", "IP地址为空，无法检测");
        }

        try {
            log.info("开始DeepSeek模型攻击检测，IP: {}", request.getIpAddress());

            // 2. 调用DeepSeek模型检测
            String modelResponse = callDeepSeekModel(request);
            if (!StringUtils.hasText(modelResponse)) {
                log.warn("DeepSeek模型返回空结果，触发降级检测");
                return fallbackDetection(request);
            }

            // 3. 解析模型响应（容错处理）
            AttackDetectionResult result = parseDeepSeekResponse(modelResponse);
            if (result == null) {
                log.warn("解析DeepSeek响应失败，触发降级检测");
                return fallbackDetection(request);
            }

            log.info("DeepSeek攻击检测完成，IP: {}, 是否攻击: {}, 攻击类型: {}",
                    request.getIpAddress(), result.isAttack(), result.getAttackType());
            return result;

        } catch (Exception e) {
            log.error("DeepSeek模型攻击检测异常，IP: {}", request.getIpAddress(), e);
            // 所有异常均触发降级检测（基于规则的硬检测）
            return fallbackDetection(request);
        }
    }

    /**
     * 调用DeepSeek API（适配官方接口规范）
     */
    private String callDeepSeekModel(AttackDetectionRequest request) {
        // 校验API密钥
        if (!StringUtils.hasText(deepseekApiKey)) {
            log.warn("DeepSeek API密钥未配置，跳过模型调用");
            return "";
        }

        // 1. 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + deepseekApiKey);

        // 2. 构建用户提示词（结构化数据）
        String userPrompt = buildUserPrompt(request);

        // 3. 构建DeepSeek请求体（严格遵循官方格式）
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", deepseekModelName);
        requestBody.put("temperature", 0.1);  // 低随机性，保证输出稳定
        requestBody.put("max_tokens", 1000);  // 足够容纳JSON结果
        requestBody.put("stream", false);     // 非流式输出

        // 消息体（system + user）
        List<JSONObject> messages = new ArrayList<>();
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", DEEPSEEK_SYSTEM_PROMPT);
        messages.add(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        // 4. 发送请求
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    deepseekApiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
                // 解析响应体（DeepSeek返回格式：{"choices":[{"message":{"content":"..."}}]}）
                JSONObject responseJson = JSON.parseObject(response.getBody());
                List<JSONObject> choices = responseJson.getJSONArray("choices").toJavaList(JSONObject.class);
                if (!choices.isEmpty()) {
                    return choices.get(0).getJSONObject("message").getString("content");
                }
            } else {
                log.error("DeepSeek API调用失败，状态码: {}, 响应: {}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("DeepSeek API请求异常", e);
        }
        return "";
    }

    /**
     * 构建用户提示词（结构化展示IP和网络数据）
     */
    private String buildUserPrompt(AttackDetectionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("### 待检测IP地址\n");
        prompt.append(request.getIpAddress()).append("\n\n");

        // 流量数据（核心检测维度）
        prompt.append("### 网络流量数据\n");
        Map<String, Object> trafficData = request.getTrafficData() == null ? new HashMap<>() : request.getTrafficData();
        appendDataToPrompt(prompt, trafficData, Arrays.asList(
                "access_count", "traffic_size", "request_rate",
                "connection_count", "connection_duration", "packet_size"
        ));

        // 连接数据（补充维度）
        prompt.append("\n### 连接特征数据\n");
        Map<String, Object> connectionData = request.getConnectionData() == null ? new HashMap<>() : request.getConnectionData();
        appendDataToPrompt(prompt, connectionData, Arrays.asList(
                "status", "create_time", "last_access_time", "abnormal_connections"
        ));

        // 强制要求JSON输出
        prompt.append("\n### 输出要求\n");
        prompt.append("请严格按照指定JSON格式输出检测结果，不要添加任何额外说明文字。");

        return prompt.toString();
    }

    /**
     * 辅助方法：将Map数据结构化拼接到提示词中
     */
    private void appendDataToPrompt(StringBuilder prompt, Map<String, Object> data, List<String> keys) {
        if (data.isEmpty()) {
            prompt.append("无可用数据\n");
            return;
        }
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) {
                prompt.append("- ").append(key).append(": ").append(value).append("\n");
            }
        }
        // 补充未指定的其他字段
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!keys.contains(entry.getKey())) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
    }

    /**
     * 解析DeepSeek响应（容错+字段校验）
     */
    private AttackDetectionResult parseDeepSeekResponse(String jsonResponse) {
        try {
            // 清理可能的多余文本（模型可能返回```json包裹）
            String cleanJson = jsonResponse.replaceAll("```json|```", "").trim();
            AttackDetectionResult result = JSON.parseObject(cleanJson, AttackDetectionResult.class);

            // 字段合法性校验（避免模型返回非法值）
            validateDetectionResult(result);
            return result;
        } catch (Exception e) {
            log.warn("解析DeepSeek响应JSON失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验检测结果字段合法性（兜底）
     */
    private void validateDetectionResult(AttackDetectionResult result) {
        if (result == null) return;

        // 置信度兜底
        if (result.getConfidence() == 0.0 || result.getConfidence() < 0 || result.getConfidence() > 1) {
            result.setConfidence(0.5);
        }
        // 风险分数兜底
        if (result.getRiskScore() == 0.0 || result.getRiskScore() < 0 || result.getRiskScore() > 100) {
            result.setRiskScore(result.isAttack() ? 80.0 : 20.0);
        }
        // 攻击类型兜底
        if (!StringUtils.hasText(result.getAttackType())) {
            result.setAttackType(result.isAttack() ? "slowloris" : "normal");
        }
        // 严重程度兜底
        if (!StringUtils.hasText(result.getSeverity())) {
            result.setSeverity(result.isAttack() ? "high" : "low");
        }
        // 建议兜底
        if (result.getRecommendations() == null || result.getRecommendations().isEmpty()) {
            result.setRecommendations(getDefaultRecommendations(result.isAttack()));
        }
        // 详情兜底
        if (result.getDetails() == null) {
            result.setDetails(new HashMap<>());
        }
    }

    /**
     * 降级检测（纯规则引擎，无模型依赖）
     * 核心：基于Slowloris攻击特征硬规则判断，保证服务可用性
     */
    private AttackDetectionResult fallbackDetection(AttackDetectionRequest request) {
        log.info("执行降级攻击检测，IP: {}", request.getIpAddress());
        AttackDetectionResult result = new AttackDetectionResult();
        Map<String, Object> details = new HashMap<>();
        List<String> abnormalIndicators = new ArrayList<>();
        boolean isSlowlorisAttack = false;

        // 1. 提取核心检测指标
        Map<String, Object> trafficData = request.getTrafficData() == null ? new HashMap<>() : request.getTrafficData();
        int connectionCount = getIntValue(trafficData.get("connection_count"), 0);
        long connectionDuration = getLongValue(trafficData.get("connection_duration"), 0);
        int requestRate = getIntValue(trafficData.get("request_rate"), 0);
        int packetSize = getIntValue(trafficData.get("packet_size"), 0);

        // 2. 填充详情字段
        details.put("request_rate", requestRate);
        details.put("connection_count", connectionCount);
        details.put("connection_duration", connectionDuration);
        details.put("packet_size", packetSize);

        // 3. 规则匹配（Slowloris核心特征）
        if (connectionCount >= SLOWLORIS_CONNECTION_THRESHOLD) {
            abnormalIndicators.add("半开连接数超标（阈值：%d，实际：%d）".formatted(SLOWLORIS_CONNECTION_THRESHOLD, connectionCount));
        }
        if (connectionDuration >= CONNECTION_DURATION_THRESHOLD) {
            abnormalIndicators.add("连接持续时间超标（阈值：%dms，实际：%dms）".formatted(CONNECTION_DURATION_THRESHOLD, connectionDuration));
        }
        if (requestRate <= REQUEST_RATE_THRESHOLD) {
            abnormalIndicators.add("请求频率过低（阈值：%d次/分钟，实际：%d次/分钟）".formatted(REQUEST_RATE_THRESHOLD, requestRate));
        }
        if (packetSize <= PACKET_SIZE_THRESHOLD && packetSize > 0) {
            abnormalIndicators.add("数据包过小（阈值：%d字节，实际：%d字节）".formatted(PACKET_SIZE_THRESHOLD, packetSize));
        }

        // 4. 判定是否为Slowloris攻击（至少匹配2个核心特征）
        if (abnormalIndicators.size() >= 2) {
            isSlowlorisAttack = true;
            result.setAttack(true);
            result.setAttackType("slowloris");
            result.setSeverity("critical");
            result.setConfidence(Math.min(0.5 + (abnormalIndicators.size() * 0.1), 1.0));
            result.setRiskScore(Math.min(70 + (abnormalIndicators.size() * 5), 100.0));
            result.setReasoning("降级检测：匹配Slowloris攻击特征（%d项）：%s".formatted(
                    abnormalIndicators.size(), String.join("；", abnormalIndicators)
            ));
        } else {
            result.setAttack(false);
            result.setAttackType("normal");
            result.setSeverity("low");
            result.setConfidence(0.9);
            result.setRiskScore(10.0 + (abnormalIndicators.size() * 5.0));
            result.setReasoning("降级检测：未匹配Slowloris攻击核心特征（仅匹配%d项）：%s".formatted(
                    abnormalIndicators.size(), abnormalIndicators.isEmpty() ? "无" : String.join("；", abnormalIndicators)
            ));
        }

        // 5. 补充详情和建议
        details.put("abnormal_indicators", abnormalIndicators);
        result.setDetails(details);
        result.setRecommendations(getDefaultRecommendations(isSlowlorisAttack));

        log.info("降级检测完成，IP: {}, 是否Slowloris攻击: {}", request.getIpAddress(), isSlowlorisAttack);
        return result;
    }

    /**
     * 创建默认检测结果（极端异常场景）
     */
    private AttackDetectionResult createDefaultResult(boolean isAttack, String attackType, String severity, String reasoning) {
        AttackDetectionResult result = new AttackDetectionResult();
        result.setAttack(isAttack);
        result.setAttackType(StringUtils.hasText(attackType) ? attackType : "normal");
        result.setSeverity(StringUtils.hasText(severity) ? severity : "low");
        result.setConfidence(0.5);
        result.setRiskScore(isAttack ? 80.0 : 20.0);
        result.setReasoning(StringUtils.hasText(reasoning) ? reasoning : "默认检测结果，无足够数据");
        result.setRecommendations(getDefaultRecommendations(isAttack));
        result.setDetails(new HashMap<>());
        return result;
    }

    /**
     * 获取默认防御建议（按攻击状态区分）
     */
    private List<String> getDefaultRecommendations(boolean isAttack) {
        List<String> recommendations = new ArrayList<>();
        if (isAttack) {
            recommendations.add("立即封禁该IP地址，阻止后续连接");
            recommendations.add("启用HTTP连接超时限制（建议≤60秒），自动关闭半开连接");
            recommendations.add("部署反向代理（如Nginx），限制单IP最大连接数");
            recommendations.add("监控服务器TCP连接状态，排查其他潜在Slowloris攻击源");
        } else {
            recommendations.add("持续监控该IP的连接数和连接时长变化");
            recommendations.add("配置服务器连接超时参数，防范潜在Slowloris攻击");
            recommendations.add("定期审计异常IP列表，提前识别风险");
        }
        return recommendations;
    }

    // 辅助方法：安全获取整数值
    private int getIntValue(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // 辅助方法：安全获取长整型值
    private long getLongValue(Object obj, long defaultValue) {
        if (obj == null) return defaultValue;
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
package com.slowloris.monitor.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.slowloris.monitor.entity.AiAnalysisResult;
import com.slowloris.monitor.entity.MetricsData;
import com.slowloris.monitor.entity.PredictionPoint;
import com.slowloris.monitor.entity.MetricsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeepSeekAnalysisService {

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.api.model.name:deepseek-chat}")
    private String modelName;

    @Autowired
    private RestTemplate restTemplate;

    private static final int CONN_THRESHOLD = 50;
    // 连接时长阈值设为10秒：攻击初期连接只保持几秒~几十秒属正常，无需等到5分钟
    private static final long DURATION_THRESHOLD = 10000;
    private static final int RATE_THRESHOLD = 5;
    private static final int PACKET_THRESHOLD = 1024;
    // 半开连接数极高时单独定性为攻击，无需其他辅助指标
    private static final int CONN_CRITICAL = 200;

    private static final String ANALYSIS_SYSTEM_PROMPT = """
            你是资深网络安全专家，专注检测Slowloris DDoS攻击（慢速HTTP攻击）。

            Slowloris攻击判断逻辑（加权评分，非全项必达）：

            【核心指标 - 必要条件】
            • 半开连接数 ≥ 50：Slowloris本质是占用大量半开TCP/HTTP连接，这是最关键的判断依据。
            • 若半开连接数 ≥ 200，无论其他指标如何，应直接判定为攻击。

            【辅助指标 - 满足任意1项即可配合核心指标定性攻击】
            • 平均包大小 ≤ 1024 字节：Slowloris每次只发送极少量数据（一行HTTP头），包极小。
            • 平均连接时长 > 几秒（哪怕10ms~几十秒也属异常）：连接被故意拖延保持存活的迹象。

            【常见误判陷阱 - 以下情况不能排除Slowloris】
            • 请求频率偏高（如数百次/分钟）：Slowloris使用多线程同时建立大量连接，汇总频率自然偏高。
              示例：350个半开连接 × 每连接约1次/分钟 = 汇总约350次/分钟，这是典型Slowloris模式。
              不能因为请求频率高就排除Slowloris，应关注"高连接数+低包大小"的组合。
            • 连接时长低于300000ms（5分钟）：攻击刚开始时连接只持续了几秒至几分钟，这完全正常。
              不要求连接时长达到5分钟才算Slowloris，只要有持续半开连接迹象即可。

            严格要求：suspiciousIps 只能从用户提供的「候选来源IP列表」中挑选，
            绝对不允许编造、推测或生成任何未在该列表中出现的IP地址；
            若候选列表为空或无可疑IP，则 suspiciousIps 必须为空数组 []。
            请分析输入数据并严格以JSON格式响应，不含任何额外文本：
            {
              "isAttack": boolean,
              "attackType": "slowloris|normal|other_ddos",
              "severity": "low|medium|high|critical",
              "confidence": 0.0-1.0,
              "riskScore": 0-100,
              "reasoning": "分析说明",
              "recommendations": ["建议1","建议2"],
              "suspiciousIps": ["必须来自候选来源IP列表"]
            }
            """;

    private static final String PREDICTION_SYSTEM_PROMPT = """
            你是网络安全分析师。根据历史攻击数据预测未来攻击趋势。
            以JSON数组格式响应，包含12个预测点（每30分钟一个，覆盖未来6小时），不含任何额外文本：
            [{"time":"HH:mm","probability":0.0-1.0}]
            """;

    public AiAnalysisResult analyze(String targetIp, MetricsData metrics) {
        if (!StringUtils.hasText(apiKey)) {
            return fallbackDetection(metrics);
        }
        try {
            String prompt = buildAnalysisPrompt(targetIp, metrics);
            String response = callDeepSeek(ANALYSIS_SYSTEM_PROMPT, prompt);
            if (!StringUtils.hasText(response)) {
                return fallbackDetection(metrics);
            }
            AiAnalysisResult result = parseAnalysisResponse(response);
            if (result == null) {
                return fallbackDetection(metrics);
            }
            // 反幻觉：AI 给出的可疑 IP 必须落在真实候选来源集合内，否则丢弃
            sanitizeSuspiciousIps(result, metrics);
            return result;
        } catch (Exception e) {
            log.error("DeepSeek分析异常，IP: {}", targetIp, e);
            return fallbackDetection(metrics);
        }
    }

    /**
     * 过滤掉 AI 编造的、不在真实候选来源集合中的 IP；
     * 若过滤后为空但确判为攻击，则回退采用真实来源 IP，保证溯源表有据可依。
     */
    private void sanitizeSuspiciousIps(AiAnalysisResult result, MetricsData metrics) {
        Set<String> candidates = new LinkedHashSet<>(
                metrics.getSourceIps() != null ? metrics.getSourceIps() : List.of());

        List<String> aiIps = result.getSuspiciousIps() != null ? result.getSuspiciousIps() : List.of();
        List<String> verified = aiIps.stream()
                .filter(candidates::contains)
                .distinct()
                .collect(Collectors.toList());

        int dropped = aiIps.size() - verified.size();
        if (dropped > 0) {
            log.warn("丢弃 {} 个候选集合外的可疑IP（疑似AI幻觉），原始: {}", dropped, aiIps);
        }

        if (verified.isEmpty() && result.isAttack() && !candidates.isEmpty()) {
            verified = candidates.stream().limit(10).collect(Collectors.toList());
            log.info("AI未给出有效可疑IP，回退采用真实来源IP {} 个", verified.size());
        }
        result.setSuspiciousIps(verified);
    }

    public List<PredictionPoint> predict(String ip, List<MetricsSnapshot> history) {
        if (!StringUtils.hasText(apiKey)) {
            return fallbackPrediction();
        }
        try {
            String prompt = buildPredictionPrompt(ip, history);
            String response = callDeepSeek(PREDICTION_SYSTEM_PROMPT, prompt);
            if (!StringUtils.hasText(response)) {
                return fallbackPrediction();
            }
            List<PredictionPoint> points = parsePredictionResponse(response);
            return (points != null && !points.isEmpty()) ? points : fallbackPrediction();
        } catch (Exception e) {
            log.error("DeepSeek预测异常，IP: {}", ip, e);
            return fallbackPrediction();
        }
    }

    private String callDeepSeek(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        JSONObject body = new JSONObject();
        body.put("model", modelName);
        body.put("temperature", 0.1);
        body.put("max_tokens", 1000);
        body.put("stream", false);

        List<JSONObject> messages = new ArrayList<>();
        JSONObject sys = new JSONObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        messages.add(sys);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", userPrompt);
        messages.add(user);
        body.put("messages", messages);

        try {
            log.debug("请求体: {}", body.toJSONString());
            ResponseEntity<String> resp = restTemplate.exchange(
                    apiUrl, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), headers), String.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.hasBody()) {
                JSONObject json = JSON.parseObject(resp.getBody());
                List<JSONObject> choices = json.getJSONArray("choices").toJavaList(JSONObject.class);
                if (!choices.isEmpty()) {
                    return choices.get(0).getJSONObject("message").getString("content");
                }
            }
        } catch (Exception e) {
            log.error("DeepSeek API调用失败: status={}, message={}", 
                    e.getMessage().split(":")[0].trim(), e.getMessage());
        }
        return "";
    }

    private String buildAnalysisPrompt(String ip, MetricsData m) {
        List<String> candidates = m.getSourceIps() != null ? m.getSourceIps() : List.of();
        String candidateList = candidates.isEmpty() ? "（无）" : String.join("、", candidates);
        return """
                目标IP: %s
                半开连接数: %d
                请求频率: %d 次/分钟
                平均连接时长: %d ms
                平均包大小: %d 字节
                唯一来源IP数: %d
                采集时间: %s
                候选来源IP列表: %s
                请分析是否存在Slowloris攻击。suspiciousIps 只能从上面「候选来源IP列表」中选取，禁止编造列表外的任何IP；若无可疑IP则返回空数组。
                """.formatted(ip, m.getHalfOpenConns(), m.getRequestRate(),
                m.getAvgConnDuration(), m.getAvgPacketSize(), m.getUniqueSourceIps(),
                m.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                candidateList);
    }

    private String buildPredictionPrompt(String ip, List<MetricsSnapshot> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("目标IP: ").append(ip).append("\n过去24小时攻击记录：\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        for (MetricsSnapshot s : history) {
            sb.append(s.getCreateTime().format(fmt))
              .append(" | 攻击=").append(s.getIsAttack() == 1 ? "是" : "否")
              .append(" | 严重度=").append(s.getSeverity() != null ? s.getSeverity() : "N/A")
              .append(" | 连接数=").append(s.getHalfOpenConns())
              .append("\n");
        }
        sb.append("请预测未来6小时（从现在起每30分钟一个点，共12个点）的攻击概率。");
        return sb.toString();
    }

    private AiAnalysisResult parseAnalysisResponse(String raw) {
        try {
            String clean = raw.replaceAll("```json|```", "").trim();
            JSONObject json = JSON.parseObject(clean);
            AiAnalysisResult r = new AiAnalysisResult();
            r.setAttack(json.getBooleanValue("isAttack"));
            r.setAttackType(json.getString("attackType"));
            r.setSeverity(json.getString("severity"));
            r.setConfidence(json.getDoubleValue("confidence"));
            r.setRiskScore(json.getDoubleValue("riskScore"));
            r.setReasoning(json.getString("reasoning"));
            r.setRecommendations(json.getJSONArray("recommendations") != null
                    ? json.getJSONArray("recommendations").toJavaList(String.class) : new ArrayList<>());
            r.setSuspiciousIps(json.getJSONArray("suspiciousIps") != null
                    ? json.getJSONArray("suspiciousIps").toJavaList(String.class) : new ArrayList<>());
            if (r.getConfidence() == 0.0) r.setConfidence(0.5);
            if (r.getRiskScore() == 0.0) r.setRiskScore(r.isAttack() ? 75.0 : 15.0);
            return r;
        } catch (Exception e) {
            log.warn("解析DeepSeek分析响应失败: {}", e.getMessage());
            return null;
        }
    }

    private List<PredictionPoint> parsePredictionResponse(String raw) {
        try {
            String clean = raw.replaceAll("```json|```", "").trim();
            return JSON.parseArray(clean, PredictionPoint.class);
        } catch (Exception e) {
            log.warn("解析DeepSeek预测响应失败: {}", e.getMessage());
            return null;
        }
    }

    private AiAnalysisResult fallbackDetection(MetricsData m) {
        AiAnalysisResult r = new AiAnalysisResult();
        List<String> indicators = new ArrayList<>();

        boolean connCritical = m.getHalfOpenConns() >= CONN_CRITICAL;
        boolean connHigh     = m.getHalfOpenConns() >= CONN_THRESHOLD;
        boolean smallPacket  = m.getAvgPacketSize() > 0 && m.getAvgPacketSize() <= PACKET_THRESHOLD;
        // 连接时长阈值降低：攻击初期连接只保持几秒属正常，远比300秒宽松
        boolean longConn     = m.getAvgConnDuration() >= DURATION_THRESHOLD;
        // 注意：请求频率高不排除Slowloris（多线程并发建连导致汇总频率高）
        // 低频才是辅助证据，高频不计入排除理由
        boolean lowRate      = m.getRequestRate() <= RATE_THRESHOLD;

        if (connHigh)    indicators.add("半开连接数超标(" + m.getHalfOpenConns() + "≥" + CONN_THRESHOLD + ")");
        if (smallPacket) indicators.add("数据包过小(" + m.getAvgPacketSize() + "B≤" + PACKET_THRESHOLD + "B)");
        if (longConn)    indicators.add("连接保持时间异常(" + m.getAvgConnDuration() + "ms≥" + DURATION_THRESHOLD + "ms)");
        if (lowRate)     indicators.add("请求频率异常低(" + m.getRequestRate() + "≤" + RATE_THRESHOLD + "/min)");

        // 判定逻辑：
        // 1. 半开连接数极高（≥200）—— 直接判定攻击
        // 2. 半开连接数超标（≥50）且 包大小异常小 —— 核心Slowloris特征组合
        // 3. 半开连接数超标 且 连接时长异常 —— 同样定性
        // 4. 其余情况保留原来的"≥2项指标"兜底
        boolean attack = connCritical
                || (connHigh && smallPacket)
                || (connHigh && longConn)
                || indicators.size() >= 2;

        r.setAttack(attack);
        r.setAttackType(attack ? "slowloris" : "normal");

        int score = indicators.size();
        String severity;
        if (connCritical || score >= 3) severity = "critical";
        else if (score == 2)            severity = "high";
        else if (score == 1)            severity = "medium";
        else                            severity = "low";
        r.setSeverity(attack ? severity : "low");

        r.setConfidence(Math.min(0.5 + score * 0.12, 1.0));
        r.setRiskScore(attack ? Math.min(65 + score * 8.0, 100) : 15.0);
        r.setReasoning("规则检测（API不可用）：" + (indicators.isEmpty() ? "无异常指标" : String.join("，", indicators))
                + (attack && m.getRequestRate() > RATE_THRESHOLD
                   ? "（注：请求频率偏高是多线程并发建连的正常表现，不影响判定）" : ""));
        r.setRecommendations(attack
                ? List.of("立即封禁异常IP", "启用HTTP连接超时限制（建议≤30秒）", "部署反向代理限制单IP最大并发连接数")
                : List.of("继续监控连接数和连接时长变化", "配置连接超时参数防范潜在攻击"));

        List<String> sources = m.getSourceIps() != null ? m.getSourceIps() : List.of();
        r.setSuspiciousIps(attack
                ? sources.stream().distinct().limit(10).collect(Collectors.toList())
                : new ArrayList<>());
        return r;
    }

    private List<PredictionPoint> fallbackPrediction() {
        List<PredictionPoint> points = new ArrayList<>();
        Random rand = new Random();
        java.time.LocalTime t = java.time.LocalTime.now();
        for (int i = 1; i <= 12; i++) {
            t = t.plusMinutes(30);
            PredictionPoint p = new PredictionPoint();
            p.setTime(t.format(DateTimeFormatter.ofPattern("HH:mm")));
            p.setProbability(0.1 + rand.nextDouble() * 0.5);
            points.add(p);
        }
        return points;
    }
}

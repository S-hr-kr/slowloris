package com.slowloris.prediction.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slowloris.common.Result;
import com.slowloris.prediction.entity.PredictionHistory;
import com.slowloris.prediction.mapper.PredictionHistoryMapper;
import com.slowloris.prediction.service.PredictionHistoryService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class PredictionHistoryServiceImpl extends ServiceImpl<PredictionHistoryMapper, PredictionHistory> implements PredictionHistoryService {

    // ======================== 依赖注入 ========================
    @Autowired
    private OpenAiChatModel deepSeekChatModel;

    // ======================== 常量定义 ========================
    // 时间格式化器
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    // 攻击风险分数阈值（≥70判定为攻击）
    private static final double RISK_THRESHOLD = 70.0;

    /**
     * 核心方法：调用 DeepSeek 大模型实现真实的攻击预测
     */
    @Override
    public Result<Map<String, Object>> predictAttack(Map<String, Object> params) {
        try {
            // 1. 严格参数校验
            String ip = (String) params.get("ip");
            String model = (String) params.get("model");
            if (!StringUtils.hasText(ip)) {
                return Result.error("IP地址不能为空");
            }
            if (!StringUtils.hasText(model)) {
                model = "deepseek-security-v1"; // 默认使用DeepSeek安全模型
            }

            log.info("开始调用DeepSeek大模型进行攻击预测，IP: {}, 模型: {}", ip, model);

            // 2. 构建攻击预测特征（模拟/真实IP行为数据，可替换为业务真实采集的特征）
            Map<String, Object> ipFeatures = buildIpBehaviorFeatures(ip);
            log.info("IP行为特征: {}", JSON.toJSONString(ipFeatures));

            // 3. 调用 DeepSeek 大模型获取预测结果
            Map<String, Object> deepseekResult = callDeepSeekModel(ip, ipFeatures);
            if (deepseekResult == null || deepseekResult.isEmpty()) {
                return Result.error("DeepSeek大模型调用失败，无法完成预测");
            }

            // 4. 解析大模型返回结果，标准化输出
            Map<String, Object> predictData = parseDeepSeekResult(deepseekResult);

            // 5. 保存预测历史到数据库
            savePredictionHistory(ip, model, predictData, ipFeatures);

            log.info("DeepSeek攻击预测完成，IP: {}, 预测结果: {}, 风险分数: {}",
                    ip, predictData.get("prediction"), predictData.get("risk_score"));

            return Result.success(predictData);
        } catch (Exception e) {
            log.error("攻击预测异常", e);
            return Result.error("服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 获取预测历史（真实数据库查询逻辑）
     */
    @Override
    public Result<List<Map<String, Object>>> getPredictionHistory(Integer page, Integer limit, String ip, String prediction) {
        try {
            // 参数校验：页码/条数默认值
            if (page == null || page < 1) page = 1;
            if (limit == null || limit < 1 || limit > 100) limit = 10;

            // 构建查询条件
            LambdaQueryWrapper<PredictionHistory> queryWrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(ip)) {
                queryWrapper.eq(PredictionHistory::getIpAddress, ip);
            }
            if (StringUtils.hasText(prediction)) {
                queryWrapper.eq(PredictionHistory::getPrediction, prediction);
            }
            queryWrapper.orderByDesc(PredictionHistory::getCreateTime);

            // 分页查询
            Page<PredictionHistory> pageResult = page(new Page<>(page, limit), queryWrapper);

            // 转换返回格式
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (PredictionHistory history : pageResult.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", history.getId());
                item.put("timestamp", history.getCreateTime() != null ? history.getCreateTime().format(DATETIME_FORMATTER) : null);
                item.put("ip", history.getIpAddress());
                item.put("prediction", history.getPrediction());
                item.put("confidence", history.getConfidence());
                item.put("risk_score", history.getRiskScore());
                item.put("model", history.getModel());
                resultList.add(item);
            }

            return Result.success(resultList);
        } catch (Exception e) {
            log.error("查询预测历史异常", e);
            return Result.error("服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 获取攻击检测结果（联动DeepSeek+数据库查询）
     */
    @Override
    public Result<Map<String, Object>> getAttackDetectionResults(Integer page, Integer limit, String type, String status) {
        try {
            // 参数校验
            if (page == null || page < 1) page = 1;
            if (limit == null || limit < 1 || limit > 100) limit = 10;

            // 构建查询条件：筛选攻击类型/状态
            LambdaQueryWrapper<PredictionHistory> queryWrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(type)) {
                // 假设prediction字段存储攻击类型（slowloris/ddos/normal）
                queryWrapper.eq(PredictionHistory::getPrediction, type);
            }
            if (StringUtils.hasText(status)) {
                // 模拟状态筛选："attack"=攻击，"normal"=正常
                String prediction = "attack".equals(status) ? "攻击" : "正常";
                queryWrapper.eq(PredictionHistory::getPrediction, prediction);
            }
            queryWrapper.orderByDesc(PredictionHistory::getCreateTime);

            // 分页查询
            Page<PredictionHistory> pageResult = page(new Page<>(page, limit), queryWrapper);

            // 组装返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("total", pageResult.getTotal()); // 总条数
            data.put("pages", pageResult.getPages()); // 总页数

            List<Map<String, Object>> list = new ArrayList<>();
            for (PredictionHistory history : pageResult.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", history.getId());
                item.put("ip", history.getIpAddress());
                item.put("attack_type", history.getPrediction());
                item.put("risk_score", history.getRiskScore());
                item.put("confidence", history.getConfidence());
                item.put("detect_time", history.getCreateTime().format(DATETIME_FORMATTER));
                item.put("model", history.getModel());
                list.add(item);
            }
            data.put("list", list);

            return Result.success(data);
        } catch (Exception e) {
            log.error("查询攻击检测结果异常", e);
            return Result.error("服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 获取攻击检测详情（真实数据库查询）
     */
    @Override
    public Result<Map<String, Object>> getAttackDetectionDetail(Long id) {
        try {
            if (id == null || id < 1) {
                return Result.error("检测记录ID不能为空");
            }

            // 查询数据库中的预测/检测记录
            PredictionHistory history = getById(id);
            if (history == null) {
                return Result.error("攻击检测记录不存在");
            }

            // 组装详情数据
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", history.getId());
            detail.put("ip", history.getIpAddress());
            detail.put("prediction", history.getPrediction());
            detail.put("confidence", history.getConfidence());
            detail.put("risk_score", history.getRiskScore());
            detail.put("model", history.getModel());
            detail.put("detect_time", history.getCreateTime().format(DATETIME_FORMATTER));
            // 解析特征重要性和详情
            detail.put("feature_importance", JSON.parse(history.getFeatureImportance()));
            detail.put("details", JSON.parse(history.getDetails()));

            return Result.success(detail);
        } catch (Exception e) {
            log.error("查询攻击检测详情异常，ID: {}", id, e);
            return Result.error("服务器内部错误：" + e.getMessage());
        }
    }

    /**
     * 触发攻击检测扫描（调用DeepSeek实时检测）
     */
    @Override
    public Result<Map<String, Object>> triggerAttackDetectionScan(Map<String, Object> params) {
        try {
            String ip = (String) params.get("ip");
            if (!StringUtils.hasText(ip)) {
                return Result.error("IP地址不能为空");
            }

            log.info("触发DeepSeek实时攻击检测扫描，IP: {}", ip);

            // 1. 生成扫描ID
            String scanId = "scan_" + System.currentTimeMillis() + "_" + ip.replace(".", "_");

            // 2. 调用DeepSeek进行实时扫描
            Map<String, Object> ipFeatures = buildIpBehaviorFeatures(ip);
            Map<String, Object> scanResult = callDeepSeekModel(ip, ipFeatures);
            Map<String, Object> parseResult = parseDeepSeekResult(scanResult);

            // 3. 组装返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("scan_id", scanId);
            data.put("ip", ip);
            data.put("status", parseResult.get("prediction").equals("攻击") ? "attack_detected" : "normal");
            data.put("started_at", LocalDateTime.now().format(DATETIME_FORMATTER));
            data.put("detect_result", parseResult);

            // 4. 保存扫描结果到数据库（复用PredictionHistory表）
            savePredictionHistory(ip, "deepseek-real-time-scan", parseResult, ipFeatures);

            return Result.success(data, "攻击检测扫描完成");
        } catch (Exception e) {
            log.error("触发攻击检测扫描异常", e);
            return Result.error("服务器内部错误：" + e.getMessage());
        }
    }

    // ======================== 私有辅助方法 ========================

    /**
     * 构建IP行为特征（可替换为业务真实采集的特征：请求频率、连接数、数据包大小等）
     */
    private Map<String, Object> buildIpBehaviorFeatures(String ip) {
        Map<String, Object> features = new HashMap<>();
        // 模拟真实采集的IP行为特征（实际场景从日志/监控系统获取）
        features.put("request_rate", new Random().nextInt(100)); // 请求频率（次/分钟）
        features.put("connection_count", new Random().nextInt(200)); // 并发连接数
        features.put("packet_size_avg", new Random().nextInt(2048)); // 平均数据包大小（字节）
        features.put("connection_duration_avg", new Random().nextInt(600000)); // 平均连接时长（毫秒）
        features.put("abnormal_request_ratio", new Random().nextDouble()); // 异常请求占比
        features.put("user_agent_uniq_count", new Random().nextInt(50)); // 唯一UA数量
        features.put("referrer_empty_ratio", new Random().nextDouble()); // 空Referrer占比
        return features;
    }

    /**
     * 调用 DeepSeek 大模型 API
     */
    private Map<String, Object> callDeepSeekModel(String ip, Map<String, Object> ipFeatures) {
        // 校验模型是否初始化
        if (deepSeekChatModel == null) {
            log.error("DeepSeekChatModel未初始化");
            return Collections.emptyMap();
        }

        // 构建提示词（安全专家角色+检测规则）
        String prompt = String.format("""
                你是资深网络安全专家，专注于检测DDoS/Slowloris/暴力攻击等网络攻击行为。
                请分析以下IP的行为特征，输出JSON格式的检测结果，包含字段：
                1. prediction: 字符串，"攻击"或"正常"
                2. confidence: 浮点数，0-1（检测置信度）
                3. risk_score: 浮点数，0-100（风险分数）
                4. attack_type: 字符串，"slowloris"/"ddos"/"brute_force"/"normal"
                5. feature_importance: 对象，各特征对结果的贡献度（request_rate/connection_count等）
                6. reasoning: 字符串，检测依据
                7. details: 对象，包含request_count/average_response_time/anomaly_score等详情
                要求：仅返回JSON，无任何额外文本。
                
                IP地址：%s
                行为特征：%s
                """, ip, JSON.toJSONString(ipFeatures));

        // 发送请求
        try {
            String response = deepSeekChatModel.generate(prompt);
            // 清理可能的多余文本（如```json包裹）
            String cleanContent = response.replaceAll("```json|```", "").trim();
            return JSON.parseObject(cleanContent, Map.class);
        } catch (Exception e) {
            log.error("DeepSeek API调用异常", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 解析DeepSeek返回结果，标准化输出格式
     */
    private Map<String, Object> parseDeepSeekResult(Map<String, Object> deepseekResult) {
        Map<String, Object> result = new HashMap<>();
        if (deepseekResult.isEmpty()) {
            // 降级默认结果
            result.put("prediction", "正常");
            result.put("confidence", 0.5);
            result.put("risk_score", 20.0);
            result.put("attack_type", "normal");
            result.put("reasoning", "DeepSeek调用失败，返回默认结果");
            return result;
        }

        // 提取核心字段
        String prediction = (String) deepseekResult.get("prediction");
        double confidence = deepseekResult.get("confidence") instanceof Number ?
                ((Number) deepseekResult.get("confidence")).doubleValue() : 0.5;
        double riskScore = deepseekResult.get("risk_score") instanceof Number ?
                ((Number) deepseekResult.get("risk_score")).doubleValue() : 0.0;
        String attackType = (String) deepseekResult.get("attack_type");
        String reasoning = (String) deepseekResult.get("reasoning");

        // 标准化结果
        result.put("prediction", prediction);
        result.put("confidence", confidence);
        result.put("risk_score", riskScore);
        result.put("attack_type", attackType);
        result.put("reasoning", reasoning);
        result.put("feature_importance", deepseekResult.get("feature_importance"));
        result.put("details", deepseekResult.get("details"));
        result.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));

        return result;
    }

    /**
     * 保存预测/检测历史到数据库
     */
    private void savePredictionHistory(String ip, String model, Map<String, Object> predictData, Map<String, Object> ipFeatures) {
        PredictionHistory history = new PredictionHistory();
        history.setIpAddress(ip);
        history.setPrediction((String) predictData.get("prediction"));
        history.setConfidence((Double) predictData.get("confidence"));
        history.setRiskScore((Double) predictData.get("risk_score"));
        history.setModel(model);
        history.setFeatureImportance(JSON.toJSONString(predictData.get("feature_importance")));
        history.setDetails(JSON.toJSONString(predictData.get("details")));
        history.setCreateTime(LocalDateTime.now());
        save(history);
    }
}
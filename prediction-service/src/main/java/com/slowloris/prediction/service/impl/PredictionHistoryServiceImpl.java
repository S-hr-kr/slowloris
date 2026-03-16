package com.slowloris.prediction.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slowloris.common.Result;
import com.slowloris.prediction.entity.PredictionHistory;
import com.slowloris.prediction.mapper.PredictionHistoryMapper;
import com.slowloris.prediction.service.PredictionHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class PredictionHistoryServiceImpl extends ServiceImpl<PredictionHistoryMapper, PredictionHistory> implements PredictionHistoryService {

    @Override
    public Result<Map<String, Object>> predictAttack(Map<String, Object> params) {
        try {
            String ip = (String) params.get("ip");
            String model = (String) params.get("model");
            if (ip == null || model == null) {
                return Result.error("请求参数错误");
            }

            // 这里应该调用真实的预测模型进行预测
            // 暂时使用基于规则的简单预测
            Map<String, Object> data = new HashMap<>();
            
            // 简单规则：如果IP地址以"192.168.1."开头，预测为正常，否则预测为攻击
            String prediction = ip.startsWith("192.168.1.") ? "正常" : "攻击";
            double confidence = 0.85; // 固定置信度
            double riskScore = prediction.equals("攻击") ? 80.0 : 20.0; // 固定风险分数
            
            data.put("prediction", prediction);
            data.put("confidence", confidence);
            data.put("risk_score", riskScore);
            
            // 特征重要性
            Map<String, Object> featureImportance = new HashMap<>();
            featureImportance.put("request_rate", 0.45);
            featureImportance.put("connection_time", 0.25);
            featureImportance.put("user_agent", 0.20);
            featureImportance.put("referrer", 0.10);
            data.put("feature_importance", featureImportance);
            
            // 预测详情
            Map<String, Object> details = new HashMap<>();
            details.put("request_count", 100); // 固定值
            details.put("average_response_time", 150.0); // 固定值
            details.put("anomaly_score", prediction.equals("攻击") ? 0.8 : 0.2); // 固定异常分数
            data.put("details", details);
            data.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            // 保存预测历史
            PredictionHistory history = new PredictionHistory();
            history.setIpAddress(ip);
            history.setPrediction(prediction);
            history.setConfidence(confidence);
            history.setRiskScore(riskScore);
            history.setModel(model);
            history.setFeatureImportance(JSON.toJSONString(featureImportance));
            history.setDetails(JSON.toJSONString(details));
            save(history);

            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to predict attack: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getPredictionHistory(Integer page, Integer limit, String ip, String prediction) {
        try {
            LambdaQueryWrapper<PredictionHistory> queryWrapper = new LambdaQueryWrapper<>();
            if (ip != null) {
                queryWrapper.eq(PredictionHistory::getIpAddress, ip);
            }
            if (prediction != null) {
                queryWrapper.eq(PredictionHistory::getPrediction, prediction);
            }
            queryWrapper.orderByDesc(PredictionHistory::getCreateTime);
            Page<PredictionHistory> pageResult = page(new Page<>(page, limit), queryWrapper);

            List<Map<String, Object>> list = new ArrayList<>();
            for (PredictionHistory history : pageResult.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", history.getId());
                item.put("timestamp", history.getCreateTime() != null ? history.getCreateTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
                item.put("ip", history.getIpAddress());
                item.put("prediction", history.getPrediction());
                item.put("confidence", history.getConfidence());
                item.put("risk_score", history.getRiskScore());
                list.add(item);
            }
            return Result.success(list);
        } catch (Exception e) {
            log.error("Failed to get prediction history: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> getAttackDetectionResults(Integer page, Integer limit, String type, String status) {
        try {
            Map<String, Object> data = new HashMap<>();
            // 实际项目中应该从数据库查询攻击检测结果
            // 这里暂时返回空列表
            data.put("total", 0);
            List<Map<String, Object>> list = new ArrayList<>();
            data.put("list", list);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get attack detection results: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> getAttackDetectionDetail(Long id) {
        try {
            // 实际项目中应该根据id从数据库查询攻击检测详情
            // 这里暂时返回错误信息
            return Result.error("攻击检测详情不存在");
        } catch (Exception e) {
            log.error("Failed to get attack detection detail: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> triggerAttackDetectionScan(Map<String, Object> params) {
        try {
            String ip = (String) params.get("ip");
            Map<String, Object> data = new HashMap<>();
            data.put("scan_id", "scan_" + System.currentTimeMillis());
            data.put("ip", ip);
            data.put("status", "scanning");
            data.put("started_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            return Result.success(data, "检测已开始");
        } catch (Exception e) {
            log.error("Failed to trigger attack detection scan: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

}

package com.slowloris.monitor.service.impl;

import com.alibaba.fastjson2.JSON;
import com.slowloris.monitor.service.AttackDetectionService;
import com.slowloris.monitor.vo.AttackDetectionRequest;
import com.slowloris.monitor.vo.AttackDetectionResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AttackDetectionServiceImpl implements AttackDetectionService {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    private static final String SYSTEM_PROMPT = "你是一个专业的网络安全专家，专门负责检测DDoS攻击，特别是Slowloris攻击。请分析提供的网络数据，判断是否存在攻击行为。" +
            "请按照以下JSON格式返回结果，不要返回任何其他文本：\n" +
            "{\n" +
            "  \"isAttack\": true/false,\n" +
            "  \"attackType\": \"slowloris/normal/other\",\n" +
            "  \"severity\": \"low/medium/high/critical\",\n" +
            "  \"confidence\": 0.0-1.0,\n" +
            "  \"riskScore\": 0-100,\n" +
            "  \"reasoning\": \"判断依据的详细说明\",\n" +
            "  \"recommendations\": [\"建议1\", \"建议2\", ...],\n" +
            "  \"details\": {\n" +
            "    \"request_rate\": 0,\n" +
            "    \"connection_time\": 0,\n" +
            "    \"packet_size\": 0,\n" +
            "    \"abnormal_indicators\": []\n" +
            "  }\n" +
            "}";

    @Override
    public AttackDetectionResult detectAttack(AttackDetectionRequest request) {
        try {
            log.info("开始大模型攻击检测，IP: {}", request.getIpAddress());
            
            String userPrompt = buildDetectionPrompt(request);
            String fullPrompt = SYSTEM_PROMPT + "\n\n" + userPrompt;
            
            String response = chatLanguageModel.generate(fullPrompt);
            log.info("大模型返回结果: {}", response);
            
            AttackDetectionResult result = parseResponse(response);
            log.info("攻击检测完成，结果: {}", result.isAttack());
            
            return result;
        } catch (Exception e) {
            log.error("大模型攻击检测失败", e);
            return fallbackDetection(request);
        }
    }

    private String buildDetectionPrompt(AttackDetectionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下IP地址的网络数据，判断是否存在DDoS攻击（特别是Slowloris攻击）：\n\n");
        prompt.append("IP地址: ").append(request.getIpAddress()).append("\n\n");
        
        if (request.getTrafficData() != null && !request.getTrafficData().isEmpty()) {
            prompt.append("流量数据:\n");
            for (Map.Entry<String, Object> entry : request.getTrafficData().entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        if (request.getConnectionData() != null && !request.getConnectionData().isEmpty()) {
            prompt.append("\n连接数据:\n");
            for (Map.Entry<String, Object> entry : request.getConnectionData().entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        return prompt.toString();
    }

    private AttackDetectionResult parseResponse(String jsonResponse) {
        try {
            return JSON.parseObject(jsonResponse, AttackDetectionResult.class);
        } catch (Exception e) {
            log.warn("解析大模型响应失败，使用默认结果", e);
            return createDefaultResult();
        }
    }

    private AttackDetectionResult fallbackDetection(AttackDetectionRequest request) {
        log.warn("使用降级方案进行攻击检测");
        AttackDetectionResult result = new AttackDetectionResult();
        
        result.setAttack(false);
        result.setAttackType("normal");
        result.setSeverity("low");
        result.setConfidence(0.5);
        result.setRiskScore(20.0);
        result.setReasoning("大模型检测服务不可用，使用默认判断");
        
        List<String> recommendations = new ArrayList<>();
        recommendations.add("建议监控该IP的后续活动");
        recommendations.add("建议启用大模型检测服务以获得更准确的结果");
        result.setRecommendations(recommendations);
        
        Map<String, Object> details = new HashMap<>();
        details.put("request_rate", 0);
        details.put("connection_time", 0);
        details.put("packet_size", 0);
        details.put("abnormal_indicators", new ArrayList<>());
        result.setDetails(details);
        
        return result;
    }

    private AttackDetectionResult createDefaultResult() {
        AttackDetectionResult result = new AttackDetectionResult();
        result.setAttack(false);
        result.setAttackType("normal");
        result.setSeverity("low");
        result.setConfidence(0.5);
        result.setRiskScore(20.0);
        result.setReasoning("无法解析检测结果，使用默认判断");
        
        List<String> recommendations = new ArrayList<>();
        recommendations.add("建议监控该IP的后续活动");
        result.setRecommendations(recommendations);
        
        Map<String, Object> details = new HashMap<>();
        details.put("request_rate", 0);
        details.put("connection_time", 0);
        details.put("packet_size", 0);
        details.put("abnormal_indicators", new ArrayList<>());
        result.setDetails(details);
        
        return result;
    }
}

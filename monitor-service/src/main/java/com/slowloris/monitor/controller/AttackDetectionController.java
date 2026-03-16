package com.slowloris.monitor.controller;

import com.slowloris.common.Result;
import com.slowloris.monitor.service.AttackDetectionService;
import com.slowloris.monitor.vo.AttackDetectionRequest;
import com.slowloris.monitor.vo.AttackDetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai-attack-detection")
public class AttackDetectionController {

    @Autowired
    private AttackDetectionService attackDetectionService;

    @PostMapping("/detect")
    public Result<Map<String, Object>> detectAttack(@RequestBody AttackDetectionRequest request) {
        try {
            log.info("收到AI攻击检测请求，IP: {}", request.getIpAddress());
            
            AttackDetectionResult result = attackDetectionService.detectAttack(request);
            
            Map<String, Object> data = new HashMap<>();
            data.put("detection_result", result);
            
            return Result.success(data, "检测完成");
        } catch (Exception e) {
            log.error("AI攻击检测失败", e);
            return Result.error("检测失败: " + e.getMessage());
        }
    }

    @PostMapping("/quick-detect")
    public Result<Map<String, Object>> quickDetectAttack(@RequestBody Map<String, Object> params) {
        try {
            String ipAddress = (String) params.get("ip");
            if (ipAddress == null || ipAddress.isEmpty()) {
                return Result.error("IP地址不能为空");
            }

            AttackDetectionRequest request = new AttackDetectionRequest();
            request.setIpAddress(ipAddress);
            
            if (params.containsKey("trafficData")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> trafficData = (Map<String, Object>) params.get("trafficData");
                request.setTrafficData(trafficData);
            }
            
            if (params.containsKey("connectionData")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> connectionData = (Map<String, Object>) params.get("connectionData");
                request.setConnectionData(connectionData);
            }

            log.info("收到快速AI攻击检测请求，IP: {}", ipAddress);
            AttackDetectionResult result = attackDetectionService.detectAttack(request);
            
            Map<String, Object> data = new HashMap<>();
            data.put("detection_result", result);
            
            return Result.success(data, "检测完成");
        } catch (Exception e) {
            log.error("快速AI攻击检测失败", e);
            return Result.error("检测失败: " + e.getMessage());
        }
    }
}

package com.slowloris.monitor.controller;

import com.slowloris.common.Result;
import com.slowloris.monitor.service.IpMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping()
public class IpMonitorController {

    @Autowired
    private IpMonitorService ipMonitorService;

    @GetMapping("/ip-monitor/list")
    public Result<Map<String, Object>> getIpList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String status) {
        return ipMonitorService.getIpList(page, limit, status);
    }

    @GetMapping("/ip-monitor/detail/{ip}")
    public Result<Map<String, Object>> getIpDetail(@PathVariable String ip) {
        return ipMonitorService.getIpDetail(ip);
    }

    @PostMapping("/ip-monitor/add")
    public Result<Map<String, Object>> addIpMonitor(@RequestBody Map<String, Object> params) {
        return ipMonitorService.addIpMonitor(params);
    }

    @GetMapping("/traffic/stats")
    public Result<Map<String, Object>> getTrafficStats(
            @RequestParam String timeRange,
            @RequestParam(required = false) String ip) {
        return ipMonitorService.getTrafficStats(timeRange, ip);
    }

    @GetMapping("/chart")
    public Result<Map<String, Object>> getChart(
            @RequestParam String ip,
            @RequestParam String timeRange) {
        return ipMonitorService.getChart(ip, timeRange);
    }

    @GetMapping("/attack-detection/results")
    public Result<Map<String, Object>> getAttackDetectionResults(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ipMonitorService.getAttackDetectionResults(page, limit, type, status);
    }

    @GetMapping("/attack-detection/detail/{id}")
    public Result<Map<String, Object>> getAttackDetectionDetail(@PathVariable Long id) {
        return ipMonitorService.getAttackDetectionDetail(id);
    }

    @PostMapping("/attack-detection/scan")
    public Result<Map<String, Object>> triggerAttackDetectionScan(@RequestBody Map<String, Object> params) {
        return ipMonitorService.triggerAttackDetectionScan(params);
    }

    @PostMapping("/predict")
    public Result<Map<String, Object>> predictAttack(@RequestBody Map<String, Object> params) {
        return ipMonitorService.predictAttack(params);
    }

    @GetMapping("/prediction/history")
    public Result<List<Map<String, Object>>> getPredictionHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String prediction) {
        return ipMonitorService.getPredictionHistory(page, limit, ip, prediction);
    }

}
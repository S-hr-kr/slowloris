package com.slowloris.prediction.controller;

import com.slowloris.common.Result;
import com.slowloris.prediction.service.PredictionHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/")
public class PredictionController {

    @Autowired
    private PredictionHistoryService predictionHistoryService;

    @PostMapping("/predict")
    public Result<Map<String, Object>> predictAttack(@RequestBody Map<String, Object> params) {
        return predictionHistoryService.predictAttack(params);
    }

    @GetMapping("/prediction/history")
    public Result<List<Map<String, Object>>> getPredictionHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String prediction) {
        return predictionHistoryService.getPredictionHistory(page, limit, ip, prediction);
    }

    @GetMapping("/attack-detection/results")
    public Result<Map<String, Object>> getAttackDetectionResults(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return predictionHistoryService.getAttackDetectionResults(page, limit, type, status);
    }

    @GetMapping("/attack-detection/detail/{id}")
    public Result<Map<String, Object>> getAttackDetectionDetail(@PathVariable Long id) {
        return predictionHistoryService.getAttackDetectionDetail(id);
    }

    @PostMapping("/attack-detection/scan")
    public Result<Map<String, Object>> triggerAttackDetectionScan(@RequestBody Map<String, Object> params) {
        return predictionHistoryService.triggerAttackDetectionScan(params);
    }

}

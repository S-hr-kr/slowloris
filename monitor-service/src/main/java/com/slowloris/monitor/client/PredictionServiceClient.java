package com.slowloris.monitor.client;

import com.slowloris.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

// 远程调用prediction-service
@FeignClient(name = "prediction-service")
public interface PredictionServiceClient {

    @PostMapping("/api/v1/prediction/predict")
    Result<Map<String, Object>> predictAttack(@RequestBody Map<String, Object> params);

    @GetMapping("/api/v1/prediction/history")
    Result<List<Map<String, Object>>> getPredictionHistory(
            @RequestParam Integer page,
            @RequestParam Integer limit,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String prediction
    );

}

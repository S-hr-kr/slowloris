package com.slowloris.monitor.client;

import com.slowloris.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "alert-log-service")
public interface AlertLogClient {

    @PostMapping("/internal/alerts")
    Result<String> createAlert(@RequestBody Map<String, Object> alert);

    @PostMapping("/internal/logs")
    Result<String> createLog(@RequestBody Map<String, Object> log);
}

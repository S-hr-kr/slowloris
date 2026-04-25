package com.slowloris.prediction.client;

import com.slowloris.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "monitor-service")
public interface MonitorServiceClient {

    @GetMapping("/ip-monitor")
    Result<Map<String, Object>> getActiveIps(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer limit
    );

    @GetMapping("/ip-monitor")
    Result<Map<String, Object>> getIpDetail(@RequestParam("ip") String ip);
}

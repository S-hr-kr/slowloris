package com.slowloris.monitor.controller;

import com.slowloris.common.Result;
import com.slowloris.monitor.service.TrafficService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TrafficController {

    private final TrafficService trafficService;

    // GET /traffic?timeRange=24h&type=all
    @GetMapping("/traffic")
    public Result<Map<String, Object>> getTraffic(
            @RequestParam(defaultValue = "24h") String timeRange,
            @RequestParam(defaultValue = "all") String type) {
        return trafficService.getTraffic(timeRange, type);
    }

    // GET /traffic/export?timeRange=24h
    @GetMapping("/traffic/export")
    public void exportTraffic(
            @RequestParam(defaultValue = "24h") String timeRange,
            HttpServletResponse response) {
        trafficService.exportTrafficCsv(timeRange, response);
    }
}

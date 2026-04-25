package com.slowloris.monitor.service;

import com.slowloris.common.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface TrafficService {
    Result<Map<String, Object>> getTraffic(String timeRange, String type);
    void exportTrafficCsv(String timeRange, HttpServletResponse response);
}

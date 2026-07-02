package com.slowloris.monitor.controller;

import com.slowloris.monitor.service.impl.TrafficStatsService;
import com.slowloris.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 流量采集上报入口。网关 {@code TrafficCollectFilter} 对每个经过的请求异步上报来源 IP，
 * 在此聚合进 Redis 滑动窗口，供 MetricsCollector 形成真实流量驱动的指标。
 * 该接口位于根路径（非 /monitor），与网关过滤器中的 URI 保持一致；网关对其跳过 JWT。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TrafficController {

    private final TrafficStatsService trafficStats;

    @PostMapping("/traffic/record")
    public Result<Void> record(@RequestBody Map<String, Object> body) {
        String source = str(body.get("ip"));
        // target 缺省时退化为全局桶 "gateway"，兼容旧网关只上报 ip 的情况
        String target = body.get("target") != null ? str(body.get("target")) : "gateway";
        long size = toLong(body.get("size"));
        trafficStats.record(target, source, size);
        return Result.success();
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try {
            return o != null ? Long.parseLong(o.toString()) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}

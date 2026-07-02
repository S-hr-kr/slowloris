package com.slowloris.monitor.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 真实流量感知：网关 {@code TrafficCollectFilter} 通过 {@code POST /traffic/record}
 * 把每次请求的来源 IP 上报到这里，按目标 IP 聚合到 Redis 滑动窗口。
 * MetricsCollector 读取这些计数，使采集结果由真实流量驱动（无流量时降级为模拟）。
 */
@Slf4j
@Service
public class TrafficStatsService {

    @Autowired
    private StringRedisTemplate redis;

    /** 统计窗口（秒），与采集周期（30s）相近 */
    private static final long WINDOW_SECONDS = 60;

    private String reqKey(String target)      { return "traffic:req:" + target; }
    private String sizeKey(String target)     { return "traffic:size:" + target; }
    private String srcSetKey(String target)   { return "traffic:src:" + target; }

    /**
     * 记录一次到达「目标」的请求。target 为被监控的目标 IP（请求宿主），source 为客户端来源 IP。
     */
    public void record(String target, String source, long size) {
        if (target == null || target.isBlank()) return;
        try {
            redis.opsForValue().increment(reqKey(target));
            redis.expire(reqKey(target), WINDOW_SECONDS, TimeUnit.SECONDS);

            if (size > 0) {
                redis.opsForValue().increment(sizeKey(target), size);
                redis.expire(sizeKey(target), WINDOW_SECONDS, TimeUnit.SECONDS);
            }
            if (source != null && !source.isBlank()) {
                redis.opsForSet().add(srcSetKey(target), source);
                redis.expire(srcSetKey(target), WINDOW_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.debug("流量统计写入失败: {}", e.getMessage());
        }
    }

    /** 当前窗口内是否有真实流量数据 */
    public boolean hasTraffic(String target) {
        try {
            String v = redis.opsForValue().get(reqKey(target));
            return v != null && Long.parseLong(v) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public long requestCount(String target) {
        return readLong(reqKey(target));
    }

    public long totalBytes(String target) {
        return readLong(sizeKey(target));
    }

    public int uniqueSources(String target) {
        try {
            Long n = redis.opsForSet().size(srcSetKey(target));
            return n != null ? n.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** 返回当前窗口内的来源 IP 集合，供可疑 IP 溯源使用 */
    public Set<String> sources(String target) {
        try {
            return redis.opsForSet().members(srcSetKey(target));
        } catch (Exception e) {
            return Set.of();
        }
    }

    private long readLong(String key) {
        try {
            String v = redis.opsForValue().get(key);
            return v != null ? Long.parseLong(v) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}

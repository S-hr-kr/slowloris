package com.slowloris.monitor.service.impl;

import com.alibaba.fastjson2.JSON;
import com.slowloris.monitor.entity.MetricsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 主机探针(Agent)上报数据的接收与缓存。
 * <p>
 * 目标服务器上运行的采集探针通过 {@code POST /internal/agent/report} 上报本机真实网络指标
 * （半开连接、真实来源 IP、唯一来源数等），在此按目标 IP 缓存到 Redis（短 TTL 滑动窗口）。
 * {@link MetricsCollector} 以最高优先级读取这些数据——因为只有运行在目标机上的探针才能拿到
 * 真实的来源 IP，是可疑 IP 溯源的事实依据。
 * </p>
 */
@Slf4j
@Service
public class AgentReportService {

    @Autowired
    private StringRedisTemplate redis;

    /** 指标新鲜度窗口（秒）：探针每 30s 上报一次，超过 90s 未上报视为离线/数据过期 */
    private static final long METRICS_TTL_SECONDS = 90;

    /** 最近上报时间保留窗口（秒）：用于前端展示「探针在线/最后心跳」 */
    private static final long LASTSEEN_TTL_SECONDS = 600;

    private String metricsKey(String ip)  { return "agent:metrics:" + ip; }
    private String lastSeenKey(String ip) { return "agent:lastseen:" + ip; }

    /**
     * 接收一次探针上报：缓存最新指标 + 刷新心跳时间。
     */
    public void record(String ip, MetricsData data) {
        if (ip == null || ip.isBlank() || data == null) return;
        try {
            redis.opsForValue().set(metricsKey(ip), JSON.toJSONString(data),
                    METRICS_TTL_SECONDS, TimeUnit.SECONDS);
            redis.opsForValue().set(lastSeenKey(ip), String.valueOf(System.currentTimeMillis()),
                    LASTSEEN_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Agent 上报数据缓存失败 ip={}: {}", ip, e.getMessage());
        }
    }

    /**
     * 读取该 IP 最新的探针指标；无新鲜数据（TTL 过期或从未上报）时返回 null。
     */
    public MetricsData latest(String ip) {
        try {
            String json = redis.opsForValue().get(metricsKey(ip));
            if (json == null || json.isBlank()) return null;
            return JSON.parseObject(json, MetricsData.class);
        } catch (Exception e) {
            log.debug("读取 Agent 指标失败 ip={}: {}", ip, e.getMessage());
            return null;
        }
    }

    /** 该 IP 是否有新鲜的探针数据（即探针在线） */
    public boolean isOnline(String ip) {
        try {
            return redis.hasKey(metricsKey(ip));
        } catch (Exception e) {
            return false;
        }
    }

    /** 探针最后一次上报的时间戳（毫秒），从未上报返回 null */
    public Long lastSeen(String ip) {
        try {
            String v = redis.opsForValue().get(lastSeenKey(ip));
            return v != null ? Long.parseLong(v) : null;
        } catch (Exception e) {
            return null;
        }
    }
}

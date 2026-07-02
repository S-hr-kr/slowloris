package com.slowloris.monitor.service.impl;

import com.slowloris.monitor.entity.MetricsData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class MetricsCollector {

    @Autowired
    private TrafficStatsService trafficStats;

    @Autowired
    private SnmpMetricsCollector snmpCollector;

    @Autowired
    private AgentReportService agentReportService;

    /**
     * 采集目标 IP 的网络指标，按优先级依次尝试：
     * <ol>
     *   <li>主机探针(Agent)上报（运行在目标机上，唯一能拿到真实来源 IP，最准确）</li>
     *   <li>SNMP 网络层采集（路由器/交换机，最权威的网络层数据）</li>
     *   <li>网关流量上报（应用层 Redis 滑动窗口）</li>
     *   <li>返回仅有 IP 和时间戳的空数据（上层自行降级）</li>
     * </ol>
     */
    public MetricsData collect(String ip) {
        // 优先级 0：主机探针上报——只有它能提供真实来源 IP，直接采用
        MetricsData agentData = agentReportService.latest(ip);
        if (agentData != null) {
            return agentData;
        }

        // 优先级 1：SNMP 网络层采集
        MetricsData snmpData = snmpCollector.collect(ip);
        if (snmpData != null) {
            // SNMP 无法获取来源 IP，尝试从网关流量补充
            enrichWithGatewaySources(ip, snmpData);
            return snmpData;
        }

        // 优先级 2：网关流量上报
        MetricsData data = new MetricsData();
        data.setIpAddress(ip);
        data.setTimestamp(LocalDateTime.now());

        if (trafficStats.hasTraffic(ip)) {
            long requests = trafficStats.requestCount(ip);
            long bytes = trafficStats.totalBytes(ip);
            int sources = Math.max(trafficStats.uniqueSources(ip), 1);

            // 请求速率（次/分钟）—— 窗口为 60s，计数即近似每分钟值
            data.setRequestRate((int) Math.min(requests, Integer.MAX_VALUE));
            // 半开连接数近似：来源数与请求数的组合估计
            data.setHalfOpenConns((int) Math.min(requests, 5000));
            // 平均包大小
            data.setAvgPacketSize(requests > 0 ? (int) Math.min(bytes / requests, 65535) : 0);
            data.setUniqueSourceIps(sources);
            // 连接时长暂无真实来源，给出于来源数相关的估计值
            data.setAvgConnDuration(60000L + (long) sources * 2000L);
            // 真实来源 IP（排除目标自身），作为可疑 IP 溯源的事实依据
            Set<String> raw = trafficStats.sources(ip);
            List<String> realSources = new ArrayList<>();
            if (raw != null) {
                raw.stream()
                        .filter(s -> s != null && !s.isBlank() && !s.equals(ip))
                        .limit(20)
                        .forEach(realSources::add);
            }
            data.setSourceIps(realSources);
        }
        return data;
    }

    /**
     * SNMP 采集不包含来源 IP，通过网关流量数据补充。
     */
    private void enrichWithGatewaySources(String ip, MetricsData data) {
        if (trafficStats.hasTraffic(ip)) {
            data.setUniqueSourceIps(Math.max(trafficStats.uniqueSources(ip), 1));
            Set<String> raw = trafficStats.sources(ip);
            List<String> realSources = new ArrayList<>();
            if (raw != null) {
                raw.stream()
                        .filter(s -> s != null && !s.isBlank() && !s.equals(ip))
                        .limit(20)
                        .forEach(realSources::add);
            }
            data.setSourceIps(realSources);
        }
    }
}

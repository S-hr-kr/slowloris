package com.slowloris.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slowloris.common.Result;
import com.slowloris.monitor.entity.AttackDetection;
import com.slowloris.monitor.entity.IpMonitor;
import com.slowloris.monitor.mapper.AttackDetectionMapper;
import com.slowloris.monitor.mapper.IpMonitorMapper;
import com.slowloris.monitor.service.TrafficService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficServiceImpl implements TrafficService {

    private final IpMonitorMapper ipMonitorMapper;
    private final AttackDetectionMapper attackDetectionMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getTraffic(String timeRange, String type) {
        String cacheKey = "traffic:stats:" + timeRange + ":" + type;
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return Result.success(cached);

        // 查询所有 IP 监控数据
        List<IpMonitor> allMonitors = ipMonitorMapper.selectList(null);

        // 获取攻击 IP 集合
        Set<String> attackIps = attackDetectionMapper.selectList(
                new LambdaQueryWrapper<AttackDetection>()
                        .select(AttackDetection::getIpAddress)
                        .in(AttackDetection::getStatus, "active", "blocked"))
                .stream().map(AttackDetection::getIpAddress)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        // 按 type 过滤
        List<IpMonitor> filtered = allMonitors;
        if ("attack".equals(type)) {
            filtered = allMonitors.stream()
                    .filter(m -> attackIps.contains(m.getIpAddress()))
                    .collect(Collectors.toList());
        } else if ("normal".equals(type)) {
            filtered = allMonitors.stream()
                    .filter(m -> !attackIps.contains(m.getIpAddress()))
                    .collect(Collectors.toList());
        }

        // 统计
        double total = filtered.stream()
                .mapToLong(m -> m.getTrafficSize() != null ? m.getTrafficSize() : 0L).sum() / 1024.0 / 1024.0;
        double normalTraffic = filtered.stream()
                .filter(m -> !attackIps.contains(m.getIpAddress()))
                .mapToLong(m -> m.getTrafficSize() != null ? m.getTrafficSize() : 0L).sum() / 1024.0 / 1024.0;
        double attackTraffic = filtered.stream()
                .filter(m -> attackIps.contains(m.getIpAddress()))
                .mapToLong(m -> m.getTrafficSize() != null ? m.getTrafficSize() : 0L).sum() / 1024.0 / 1024.0;
        long peak = filtered.stream()
                .mapToLong(m -> m.getAccessCount() != null ? m.getAccessCount() : 0L).max().orElse(0L);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", Math.round(total * 100.0) / 100.0);
        stats.put("normal", Math.round(normalTraffic * 100.0) / 100.0);
        stats.put("attack", Math.round(attackTraffic * 100.0) / 100.0);
        stats.put("peak", peak);

        // Top IPs（按流量降序）
        List<Map<String, Object>> topIps = filtered.stream()
                .sorted(Comparator.comparingLong((IpMonitor m) ->
                        m.getTrafficSize() != null ? m.getTrafficSize() : 0L).reversed())
                .limit(10)
                .map(m -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("ip", m.getIpAddress());
                    item.put("traffic", Math.round((m.getTrafficSize() != null ? m.getTrafficSize() : 0L) / 1024.0 / 1024.0 * 100.0) / 100.0);
                    item.put("type", attackIps.contains(m.getIpAddress()) ? "attack" : "normal");
                    item.put("protocol", "HTTP");
                    return item;
                }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stats", stats);
        data.put("topIps", topIps);

        redisTemplate.opsForValue().set(cacheKey, data, 60, TimeUnit.SECONDS);
        return Result.success(data);
    }

    @Override
    public void exportTrafficCsv(String timeRange, HttpServletResponse response) {
        try {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=traffic_" + timeRange + ".csv");
            response.setCharacterEncoding("UTF-8");

            List<IpMonitor> monitors = ipMonitorMapper.selectList(
                    new LambdaQueryWrapper<IpMonitor>().orderByDesc(IpMonitor::getTrafficSize));

            Set<String> attackIps = attackDetectionMapper.selectList(
                    new LambdaQueryWrapper<AttackDetection>()
                            .select(AttackDetection::getIpAddress)
                            .in(AttackDetection::getStatus, "active", "blocked"))
                    .stream().map(AttackDetection::getIpAddress)
                    .filter(Objects::nonNull).collect(Collectors.toSet());

            PrintWriter writer = response.getWriter();
            writer.println("ip,traffic_mb,type,protocol,lastActivity");
            for (IpMonitor m : monitors) {
                double trafficMb = (m.getTrafficSize() != null ? m.getTrafficSize() : 0L) / 1024.0 / 1024.0;
                String ipType = attackIps.contains(m.getIpAddress()) ? "attack" : "normal";
                String lastActivity = m.getLastAccessTime() != null ? m.getLastAccessTime().format(FMT) : "";
                writer.printf("%s,%.2f,%s,HTTP,%s%n",
                        m.getIpAddress(), trafficMb, ipType, lastActivity);
            }
            writer.flush();
        } catch (Exception e) {
            log.error("导出流量数据失败", e);
        }
    }
}

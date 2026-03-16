package com.slowloris.monitor.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slowloris.common.Result;
import com.slowloris.monitor.entity.AttackDetection;
import com.slowloris.monitor.entity.IpMonitor;
import com.slowloris.monitor.mapper.AttackDetectionMapper;
import com.slowloris.monitor.mapper.IpMonitorMapper;
import com.slowloris.monitor.service.AttackDetectionService;
import com.slowloris.monitor.service.IpMonitorService;
import com.slowloris.monitor.vo.AttackDetectionRequest;
import com.slowloris.monitor.vo.AttackDetectionResult;
import com.slowloris.monitor.vo.TrafficAnalysisVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class IpMonitorServiceImpl extends ServiceImpl<IpMonitorMapper, IpMonitor> implements IpMonitorService {

    @Autowired
    private IpMonitorMapper ipMonitorMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AttackDetectionService attackDetectionService;

    @Autowired
    private AttackDetectionMapper attackDetectionMapper;

    private static final String REDIS_KEY_TRAFFIC_HOURLY = "traffic:hourly:";
    private static final String REDIS_KEY_IP_MONITOR = "ip:monitor:";

    @Override
    public IpMonitor getByIpAddress(String ipAddress) {
        // Get directly from database, avoid Redis deserialization issue
        return ipMonitorMapper.selectByIpAddress(ipAddress);
    }

    @Override
    public void updateAccessRecord(String ipAddress, long trafficSize) {
        IpMonitor monitor = getByIpAddress(ipAddress);
        LocalDateTime now = LocalDateTime.now();

        if (monitor == null) {
            // Create new record
            monitor = new IpMonitor();
            monitor.setIpAddress(ipAddress);
            monitor.setAccessCount(1);
            monitor.setLastAccessTime(now);
            monitor.setTrafficSize(trafficSize);
            monitor.setStatus(0); // Normal by default
            save(monitor);
        } else {
            // Update existing record
            monitor.setAccessCount((monitor.getAccessCount() != null ? monitor.getAccessCount() : 0) + 1);
            monitor.setLastAccessTime(now);
            monitor.setTrafficSize((monitor.getTrafficSize() != null ? monitor.getTrafficSize() : 0) + trafficSize);

            // Simple anomaly detection: if access count > 100 in recent time, mark as suspicious
            if (monitor.getAccessCount() > 100) {
                monitor.setStatus(1); // Suspicious
            }
            updateById(monitor);
        }

        // Update Redis cache
        redisTemplate.opsForValue().set(
                REDIS_KEY_IP_MONITOR + ipAddress,
                monitor,
                30L,
                TimeUnit.MINUTES
        );

        // Update hourly traffic statistics
        String hourlyKey = REDIS_KEY_TRAFFIC_HOURLY + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH"));
        redisTemplate.opsForValue().increment(hourlyKey, trafficSize);
        redisTemplate.expire(hourlyKey, 24L, TimeUnit.HOURS);
    }

    @Override
    public List<IpMonitor> getAbnormalIps() {
        return ipMonitorMapper.selectAbnormalIps();
    }

    @Override
    public List<IpMonitor> getRecentRecords(int limit) {
        return ipMonitorMapper.selectRecentRecords(limit);
    }

    @Override
    public TrafficAnalysisVo getTrafficAnalysis() {
        TrafficAnalysisVo vo = new TrafficAnalysisVo();

        // Calculate total traffic
        Long totalTraffic = count() > 0 ? 
                list().stream().mapToLong(IpMonitor::getTrafficSize).sum() : 0L;
        vo.setTotalTraffic(totalTraffic);

        // Calculate today's and yesterday's traffic
        Long todayTraffic = ipMonitorMapper.getTodayTraffic();
        Long yesterdayTraffic = ipMonitorMapper.getYesterdayTraffic();
        vo.setTodayTraffic(todayTraffic);
        vo.setYesterdayTraffic(yesterdayTraffic);
        
        // Calculate growth rate
        double growthRate = 0.0;
        if (yesterdayTraffic != null && yesterdayTraffic > 0) {
            growthRate = ((double)(todayTraffic - yesterdayTraffic) / yesterdayTraffic) * 100;
        }
        vo.setGrowthRate(growthRate);

        // Get hourly traffic data
        List<Map<String, Object>> dbHourlyTraffic = ipMonitorMapper.countHourlyTraffic();
        List<Map<String, Object>> hourlyTraffic = new ArrayList<>();
        Map<Integer, Long> hourTrafficMap = new HashMap<>();
        for (Map<String, Object> item : dbHourlyTraffic) {
            hourTrafficMap.put(((Number) item.get("hour")).intValue(), ((Number) item.get("traffic")).longValue());
        }
        LocalDateTime now = LocalDateTime.now();
        for (int i = 23; i >= 0; i--) {
            LocalDateTime hour = now.minusHours(i);
            int hourInt = hour.getHour();
            String hourStr = String.format("%02d:00", hourInt);
            Map<String, Object> hourData = new HashMap<>();
            hourData.put("hour", hourStr);
            hourData.put("traffic", hourTrafficMap.getOrDefault(hourInt, 0L));
            hourlyTraffic.add(hourData);
        }
        vo.setHourlyTraffic(hourlyTraffic);

        // Get IP segment traffic data
        List<Map<String, Object>> ipSegmentTraffic = ipMonitorMapper.countTrafficByIpSegment();
        vo.setIpSegmentTraffic(ipSegmentTraffic);

        // Get IP status count
        List<Map<String, Object>> statusStats = ipMonitorMapper.countByStatus();
        Map<String, Integer> ipStatusCount = new HashMap<>();
        ipStatusCount.put("normal", 0);
        ipStatusCount.put("suspicious", 0);
        ipStatusCount.put("abnormal", 0);
        for (Map<String, Object> stat : statusStats) {
            int status = ((Number) stat.get("status")).intValue();
            int count = ((Number) stat.get("count")).intValue();
            if (status == 0) {
                ipStatusCount.put("normal", count);
            } else if (status == 1) {
                ipStatusCount.put("suspicious", count);
            } else if (status == 2) {
                ipStatusCount.put("abnormal", count);
            }
        }
        vo.setIpStatusCount(ipStatusCount);

        return vo;
    }

    @Override
    public List<IpMonitor> getIpsByStatus(Integer status) {
        if (status == null) {
            return list();
        }
        return lambdaQuery().eq(IpMonitor::getStatus, status).list();
    }

    @Override
    public boolean setIpStatus(String ipAddress, Integer status) {
        IpMonitor monitor = getByIpAddress(ipAddress);
        if (monitor == null) {
            return false;
        }

        monitor.setStatus(status);
        boolean success = updateById(monitor);
        if (success) {
            // Update Redis cache
            redisTemplate.opsForValue().set(
                    REDIS_KEY_IP_MONITOR + ipAddress,
                    monitor,
                    30L,
                    TimeUnit.MINUTES
            );
        }
        return success;
    }

    @Override
    public Result<Map<String, Object>> getIpList(Integer page, Integer limit, String status) {
        try {
            LambdaQueryWrapper<IpMonitor> queryWrapper = new LambdaQueryWrapper<>();
            if (status != null) {
                Integer statusInt = "active".equals(status) ? 0 : "suspicious".equals(status) ? 1 : 2;
                queryWrapper.eq(IpMonitor::getStatus, statusInt);
            }
            queryWrapper.orderByDesc(IpMonitor::getLastAccessTime);

            Page<IpMonitor> pageResult = page(new Page<>(page, limit), queryWrapper);
            Map<String, Object> data = new HashMap<>();
            data.put("total", pageResult.getTotal());
            List<Map<String, Object>> list = new ArrayList<>();
            for (IpMonitor monitor : pageResult.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", monitor.getId());
                item.put("ip", monitor.getIpAddress());
                item.put("status", monitor.getStatus() == 0 ? "active" : monitor.getStatus() == 1 ? "suspicious" : "abnormal");
                item.put("last_seen", monitor.getLastAccessTime() != null ? monitor.getLastAccessTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
                // 基于状态和访问次数计算风险分数
                double riskScore = calculateRiskScore(monitor);
                item.put("risk_score", riskScore);
                item.put("country", monitor.getCountry());
                list.add(item);
            }
            data.put("list", list);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get IP list: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> getIpDetail(String ip) {
        try {
            IpMonitor monitor = getByIpAddress(ip);
            if (monitor == null) {
                return Result.error("IP不存在");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("ip", monitor.getIpAddress());
            data.put("status", monitor.getStatus() == 0 ? "active" : monitor.getStatus() == 1 ? "suspicious" : "abnormal");
            // 基于状态和访问次数计算风险分数
            double riskScore = calculateRiskScore(monitor);
            data.put("risk_score", riskScore);
            data.put("country", monitor.getCountry());
            data.put("isp", monitor.getIsp());
            data.put("last_seen", monitor.getLastAccessTime() != null ? monitor.getLastAccessTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
            data.put("first_seen", monitor.getCreateTime() != null ? monitor.getCreateTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
            data.put("connections", monitor.getAccessCount());
            data.put("requests", monitor.getAccessCount() != null ? monitor.getAccessCount() * 5 : 0);
            // 从数据库查询实际告警数量
            long alertCount = attackDetectionMapper.selectCount(new LambdaQueryWrapper<AttackDetection>()
                    .eq(AttackDetection::getIpAddress, ip)
                    .eq(AttackDetection::getIsAlerted, true));
            data.put("alerts", alertCount);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get IP detail: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    /**
     * 基于IP状态和访问次数计算风险分数
     */
    private double calculateRiskScore(IpMonitor monitor) {
        double baseScore = 0.0;
        // 基于状态计算基础分数
        if (monitor.getStatus() == 2) {
            baseScore = 75.0; // 异常状态基础分数
        } else if (monitor.getStatus() == 1) {
            baseScore = 40.0; // 可疑状态基础分数
        } else {
            baseScore = 10.0; // 正常状态基础分数
        }
        
        // 基于访问次数调整分数
        int accessCount = monitor.getAccessCount() != null ? monitor.getAccessCount() : 0;
        double accessScore = Math.min(accessCount / 1000.0 * 30.0, 30.0);
        
        return Math.min(baseScore + accessScore, 100.0);
    }

    @Override
    public Result<Map<String, Object>> addIpMonitor(Map<String, Object> params) {
        try {
            String ip = (String) params.get("ip");
            if (ip == null || ip.isEmpty()) {
                return Result.error("请求参数错误");
            }
            IpMonitor existing = getByIpAddress(ip);
            if (existing != null) {
                return Result.error("IP已存在");
            }
            IpMonitor monitor = new IpMonitor();
            monitor.setIpAddress(ip);
            monitor.setAccessCount(0);
            monitor.setTrafficSize(0L);
            monitor.setStatus(0);
            monitor.setLastAccessTime(LocalDateTime.now());
            if (params.containsKey("description")) {
                monitor.setCity((String) params.get("description"));
            }
            save(monitor);
            Map<String, Object> data = new HashMap<>();
            data.put("id", monitor.getId());
            data.put("ip", monitor.getIpAddress());
            data.put("status", "active");
            data.put("description", params.get("description"));
            return Result.success(data, "IP添加成功");
        } catch (Exception e) {
            log.error("Failed to add IP monitor: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> getTrafficStats(String timeRange, String ip) {
        try {
            Map<String, Object> data = new HashMap<>();
            
            // 统计总请求数和连接数
            LambdaQueryWrapper<IpMonitor> queryWrapper = new LambdaQueryWrapper<>();
            if (ip != null && !ip.isEmpty()) {
                queryWrapper.eq(IpMonitor::getIpAddress, ip);
            }
            
            List<IpMonitor> monitors = list(queryWrapper);
            int totalRequests = monitors.stream()
                    .mapToInt(monitor -> monitor.getAccessCount() != null ? monitor.getAccessCount() * 5 : 0)
                    .sum();
            int totalConnections = monitors.stream()
                    .mapToInt(monitor -> monitor.getAccessCount() != null ? monitor.getAccessCount() : 0)
                    .sum();
            
            data.put("total_requests", totalRequests);
            data.put("total_connections", totalConnections);
            data.put("average_response_time", 0.0); // 暂未实现响应时间统计
            
            // 计算峰值流量时间
            LocalDateTime peakTrafficTime = monitors.stream()
                    .filter(monitor -> monitor.getLastAccessTime() != null)
                    .map(IpMonitor::getLastAccessTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
            data.put("peak_traffic", peakTrafficTime.format(DateTimeFormatter.ISO_DATE_TIME));
            
            // 获取Top IP列表
            List<Map<String, Object>> topIps = monitors.stream()
                    .sorted((m1, m2) -> {
                        int count1 = m1.getAccessCount() != null ? m1.getAccessCount() : 0;
                        int count2 = m2.getAccessCount() != null ? m2.getAccessCount() : 0;
                        return Integer.compare(count2, count1);
                    })
                    .limit(5)
                    .map(monitor -> {
                        Map<String, Object> topIp = new HashMap<>();
                        topIp.put("ip", monitor.getIpAddress());
                        int requests = monitor.getAccessCount() != null ? monitor.getAccessCount() * 5 : 0;
                        topIp.put("requests", requests);
                        double percentage = totalRequests > 0 ? (double) requests / totalRequests * 100 : 0.0;
                        topIp.put("percentage", Math.round(percentage * 100.0) / 100.0);
                        return topIp;
                    })
                    .toList();
            
            data.put("top_ips", topIps);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get traffic stats: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> getChart(String ip, String timeRange) {
        try {
            Map<String, Object> data = new HashMap<>();
            List<String> labels = new ArrayList<>();
            List<Map<String, Object>> datasets = new ArrayList<>();
            
            // 生成时间标签（最近4个时间点）
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < 4; i++) {
                labels.add(now.minusMinutes((3 - i) * 5).format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            
            // 初始化数据集
            Map<String, Object> requestDataset = new HashMap<>();
            requestDataset.put("label", "请求数");
            requestDataset.put("borderColor", "#3b82f6");
            requestDataset.put("backgroundColor", "rgba(59, 130, 246, 0.1)");
            
            Map<String, Object> responseDataset = new HashMap<>();
            responseDataset.put("label", "响应时间");
            responseDataset.put("borderColor", "#10b981");
            responseDataset.put("backgroundColor", "rgba(16, 185, 129, 0.1)");
            
            // 从数据库查询实际数据，这里暂时返回固定值
            // 实际项目中应该根据ip和timeRange查询数据库获取历史数据
            List<Integer> requestData = Arrays.asList(0, 0, 0, 0);
            List<Double> responseData = Arrays.asList(0.0, 0.0, 0.0, 0.0);
            
            requestDataset.put("data", requestData);
            responseDataset.put("data", responseData);
            
            datasets.add(requestDataset);
            datasets.add(responseDataset);
            
            data.put("labels", labels);
            data.put("datasets", datasets);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get chart data: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> getAttackDetectionResults(Integer page, Integer limit, String type, String status) {
        try {
            LambdaQueryWrapper<AttackDetection> queryWrapper = new LambdaQueryWrapper<>();
            if (type != null && !type.isEmpty()) {
                queryWrapper.eq(AttackDetection::getType, type);
            }
            if (status != null && !status.isEmpty()) {
                queryWrapper.eq(AttackDetection::getStatus, status);
            }
            queryWrapper.orderByDesc(AttackDetection::getDetectedAt);

            Page<AttackDetection> pageResult = attackDetectionMapper.selectPage(new Page<>(page, limit), queryWrapper);
            Map<String, Object> data = new HashMap<>();
            data.put("total", pageResult.getTotal());
            
            List<Map<String, Object>> list = new ArrayList<>();
            for (AttackDetection detection : pageResult.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", detection.getId());
                item.put("ip", detection.getIpAddress());
                item.put("type", detection.getType());
                item.put("status", detection.getStatus());
                item.put("severity", detection.getSeverity());
                item.put("detected_at", detection.getDetectedAt() != null ? detection.getDetectedAt().format(DateTimeFormatter.ISO_DATE_TIME) : null);
                if (detection.getDetails() != null) {
                    item.put("details", JSON.parse(detection.getDetails()));
                }
                list.add(item);
            }
            data.put("list", list);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get attack detection results: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> getAttackDetectionDetail(Long id) {
        try {
            AttackDetection detection = attackDetectionMapper.selectById(id);
            if (detection == null) {
                return Result.error("攻击检测详情不存在");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", detection.getId());
            data.put("ip", detection.getIpAddress());
            data.put("type", detection.getType());
            data.put("status", detection.getStatus());
            data.put("severity", detection.getSeverity());
            data.put("detected_at", detection.getDetectedAt() != null ? detection.getDetectedAt().format(DateTimeFormatter.ISO_DATE_TIME) : null);
            data.put("resolved_at", detection.getResolvedAt() != null ? detection.getResolvedAt().format(DateTimeFormatter.ISO_DATE_TIME) : null);
            
            if (detection.getDetails() != null) {
                data.put("details", JSON.parse(detection.getDetails()));
            }
            
            data.put("actions", new ArrayList<>());
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get attack detection detail: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> triggerAttackDetectionScan(Map<String, Object> params) {
        try {
            String ip = (String) params.get("ip");
            if (ip == null || ip.isEmpty()) {
                return Result.error("IP地址不能为空");
            }
            
            log.info("开始攻击检测扫描，IP: {}", ip);
            
            AttackDetectionRequest request = new AttackDetectionRequest();
            request.setIpAddress(ip);
            
            IpMonitor monitor = getByIpAddress(ip);
            if (monitor != null) {
                Map<String, Object> trafficData = new HashMap<>();
                trafficData.put("access_count", monitor.getAccessCount());
                trafficData.put("traffic_size", monitor.getTrafficSize());
                trafficData.put("last_access_time", monitor.getLastAccessTime() != null ? monitor.getLastAccessTime().toString() : null);
                request.setTrafficData(trafficData);
                
                Map<String, Object> connectionData = new HashMap<>();
                connectionData.put("status", monitor.getStatus());
                connectionData.put("create_time", monitor.getCreateTime() != null ? monitor.getCreateTime().toString() : null);
                request.setConnectionData(connectionData);
            }
            
            AttackDetectionResult detectionResult = attackDetectionService.detectAttack(request);
            
            AttackDetection attackDetection = new AttackDetection();
            attackDetection.setIpAddress(ip);
            attackDetection.setType(detectionResult.getAttackType());
            attackDetection.setStatus(detectionResult.isAttack() ? "detected" : "normal");
            attackDetection.setSeverity(detectionResult.getSeverity());
            attackDetection.setConfidence(detectionResult.getConfidence());
            attackDetection.setRiskScore(detectionResult.getRiskScore());
            attackDetection.setReasoning(detectionResult.getReasoning());
            attackDetection.setRecommendations(JSON.toJSONString(detectionResult.getRecommendations()));
            attackDetection.setDetails(JSON.toJSONString(detectionResult.getDetails()));
            attackDetection.setDetectedAt(LocalDateTime.now());
            attackDetectionMapper.insert(attackDetection);
            
            Map<String, Object> data = new HashMap<>();
            data.put("scan_id", "scan_" + System.currentTimeMillis());
            data.put("detection_id", attackDetection.getId());
            data.put("ip", ip);
            data.put("status", "completed");
            data.put("started_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            data.put("detection_result", detectionResult);
            
            return Result.success(data, "检测完成");
        } catch (Exception e) {
            log.error("Failed to trigger attack detection scan: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> predictAttack(Map<String, Object> params) {
        try {
            String ip = (String) params.get("ip");
            // 实际项目中应该调用prediction-service来获取真实的预测结果
            // 这里暂时返回基于IP状态的简单预测
            Map<String, Object> data = new HashMap<>();
            
            IpMonitor monitor = getByIpAddress(ip);
            if (monitor != null && monitor.getStatus() == 2) {
                // 如果IP状态为异常，预测为攻击
                data.put("prediction", "攻击");
                data.put("confidence", 0.85);
                data.put("risk_score", calculateRiskScore(monitor));
            } else {
                // 否则预测为正常
                data.put("prediction", "正常");
                data.put("confidence", 0.75);
                data.put("risk_score", monitor != null ? calculateRiskScore(monitor) : 10.0);
            }
            
            // 特征重要性
            Map<String, Object> featureImportance = new HashMap<>();
            featureImportance.put("request_rate", 0.45);
            featureImportance.put("connection_time", 0.25);
            featureImportance.put("user_agent", 0.20);
            featureImportance.put("referrer", 0.10);
            data.put("feature_importance", featureImportance);
            
            // 详情信息
            Map<String, Object> details = new HashMap<>();
            details.put("request_count", monitor != null && monitor.getAccessCount() != null ? monitor.getAccessCount() * 5 : 0);
            details.put("average_response_time", 0.0); // 暂未实现
            details.put("anomaly_score", monitor != null && monitor.getStatus() > 0 ? 0.7 : 0.3);
            data.put("details", details);
            
            data.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to predict attack: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getPredictionHistory(Integer page, Integer limit, String ip, String prediction) {
        try {
            // 实际项目中应该调用prediction-service来获取真实的预测历史记录
            // 这里暂时返回空列表
            List<Map<String, Object>> list = new ArrayList<>();
            return Result.success(list);
        } catch (Exception e) {
            log.error("Failed to get prediction history: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

}
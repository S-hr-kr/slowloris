package com.slowloris.monitor.engine;

import com.alibaba.fastjson2.JSON;
import com.slowloris.monitor.client.AlertLogClient;
import com.slowloris.monitor.entity.*;
import com.slowloris.monitor.mapper.MetricsSnapshotMapper;
import com.slowloris.monitor.mapper.MonitoredTargetMapper;
import com.slowloris.monitor.service.impl.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MonitoringEngine {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    /** 活跃监控任务，键为「ip|userId」复合键：同一台 IP 可被多个用户各自独立监控。 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    @Autowired private MetricsCollector metricsCollector;
    @Autowired private DeepSeekAnalysisService aiService;
    @Autowired private GeoLookupService geoService;
    @Autowired private AlertLogClient alertLogClient;
    @Autowired private MonitoredTargetMapper targetMapper;
    @Autowired private MetricsSnapshotMapper snapshotMapper;
    @Autowired private DispositionService dispositionService;
    @Autowired private AgentEventPublisher eventPublisher;

    /** 复合键：区分「同一 IP、不同归属用户」的监控任务。userId 为空时以 "_" 占位。 */
    private static String taskKey(String ip, String userId) {
        return ip + "|" + (userId != null ? userId : "_");
    }

    public void startMonitoring(String ip, String userId) {
        String key = taskKey(ip, userId);
        if (activeTasks.containsKey(key)) {
            log.info("IP {} (user={}) 已在监控中，跳过重复启动", ip, userId);
            return;
        }

        MonitoredTarget existing = targetMapper.findByIpAndUser(ip, userId);
        if (existing == null) {
            MonitoredTarget target = new MonitoredTarget();
            target.setIpAddress(ip);
            target.setStatus(1);
            target.setUserId(userId);
            target.setAgentToken(UUID.randomUUID().toString().replace("-", ""));
            targetMapper.insert(target);
        } else {
            existing.setStatus(1);
            // 补发 token：兼容升级前已存在、尚无 token 的历史目标
            if (existing.getAgentToken() == null || existing.getAgentToken().isBlank()) {
                existing.setAgentToken(UUID.randomUUID().toString().replace("-", ""));
            }
            targetMapper.updateById(existing);
        }

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> runPollCycle(ip, userId), 0, 30, TimeUnit.SECONDS);
        activeTasks.put(key, future);
        log.info("已启动监控 IP: {} (user={})", ip, userId);
    }

    public void stopMonitoring(String ip, String userId) {
        ScheduledFuture<?> future = activeTasks.remove(taskKey(ip, userId));
        if (future != null) {
            future.cancel(false);
        }
        targetMapper.deactivate(ip, userId);
        log.info("已停止监控 IP: {} (user={})", ip, userId);
    }

    public boolean isMonitoring(String ip, String userId) {
        return activeTasks.containsKey(taskKey(ip, userId));
    }

    /** 是否有任意用户在监控该 IP（管理员视角的活跃判断）。 */
    public boolean isMonitoringAny(String ip) {
        String prefix = ip + "|";
        return activeTasks.keySet().stream().anyMatch(k -> k.startsWith(prefix));
    }

    @PostConstruct
    public void resumeFromDatabase() {
        try {
            List<MonitoredTarget> active = targetMapper.findAllActive();
            log.info("恢复 {} 个活跃监控任务", active.size());
            active.forEach(t -> {
                String key = taskKey(t.getIpAddress(), t.getUserId());
                ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                        () -> runPollCycle(t.getIpAddress(), t.getUserId()), 5, 30, TimeUnit.SECONDS);
                activeTasks.put(key, future);
            });
        } catch (Exception e) {
            log.warn("从数据库恢复监控任务失败（可能数据表未初始化），跳过自动恢复: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void runPollCycle(String ip, String userId) {
        try {
            MetricsData metrics = metricsCollector.collect(ip);
            AiAnalysisResult aiResult = aiService.analyze(ip, metrics);

            // 攻击时解析并地理富化可疑来源 IP；快照与告警共用同一份富化结果
            List<SuspiciousIp> enrichedIps = aiResult.isAttack()
                    ? resolveSuspiciousIps(ip, aiResult)
                    : Collections.emptyList();

            MetricsSnapshot snapshot = buildSnapshot(ip, userId, metrics, aiResult, enrichedIps);
            snapshotMapper.insert(snapshot);

            // 实时推送本轮分析结果（无论是否攻击），仅推送给数据归属用户与管理员
            broadcastAnalysis(ip, userId, metrics, aiResult, enrichedIps);

            if (aiResult.isAttack()) {
                // 决策→处置闭环（记录检测、按配置自动软封禁或生成建议、推送事件）
                dispositionService.decideAndDispose(ip, userId, aiResult, enrichedIps);

                createAlert(ip, userId, aiResult, enrichedIps);
                createLog(ip, userId, aiResult, enrichedIps);
            }

            // 处理到期的自动封禁解封
            dispositionService.autoUnblockExpired();
        } catch (Exception e) {
            log.error("轮询周期异常，IP: {} (user={})", ip, userId, e);
        }
    }

    /**
     * 解析可疑来源 IP：可疑 IP 已在 {@link DeepSeekAnalysisService} 中完成反幻觉校验
     * （仅保留真实来源集合内的 IP），此处仅做地理富化。
     */
    private List<SuspiciousIp> resolveSuspiciousIps(String ip, AiAnalysisResult aiResult) {
        List<String> rawIps = aiResult.getSuspiciousIps();
        if (rawIps == null || rawIps.isEmpty()) {
            return Collections.emptyList();
        }
        return rawIps.stream()
                .filter(s -> s != null && !s.isBlank() && !s.equals(ip))
                .distinct()
                .map(geoService::enrich)
                .collect(Collectors.toList());
    }

    private void broadcastAnalysis(String ip, String userId, MetricsData m, AiAnalysisResult ai, List<SuspiciousIp> enrichedIps) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("ip", ip);
            event.put("isAttack", ai.isAttack());
            event.put("severity", ai.getSeverity());
            event.put("riskScore", ai.getRiskScore());
            event.put("confidence", ai.getConfidence());
            event.put("reasoning", ai.getReasoning());
            event.put("halfOpenConns", m.getHalfOpenConns());
            event.put("requestRate", m.getRequestRate());
            event.put("avgPacketSize", m.getAvgPacketSize());
            event.put("uniqueSourceIps", m.getUniqueSourceIps());
            event.put("suspiciousIps", enrichedIps);
            event.put("timestamp", System.currentTimeMillis());
            eventPublisher.broadcastToOwner("analysis", event, userId);
        } catch (Exception e) {
            log.debug("推送分析事件失败: {}", e.getMessage());
        }
    }

    private MetricsSnapshot buildSnapshot(String ip, String userId, MetricsData m, AiAnalysisResult ai, List<SuspiciousIp> enrichedIps) {
        MetricsSnapshot s = new MetricsSnapshot();
        s.setIpAddress(ip);
        s.setUserId(userId);
        s.setHalfOpenConns(m.getHalfOpenConns());
        s.setRequestRate(m.getRequestRate());
        s.setAvgConnDuration(m.getAvgConnDuration());
        s.setAvgPacketSize(m.getAvgPacketSize());
        s.setUniqueSourceIps(m.getUniqueSourceIps());
        s.setIsAttack(ai.isAttack() ? 1 : 0);
        s.setSeverity(ai.getSeverity());
        // 快照内的 aiVerdict 存储「地理富化后的可疑 IP 对象」，与前端溯源表字段一致
        Map<String, Object> verdict = new HashMap<>();
        verdict.put("isAttack", ai.isAttack());
        verdict.put("attackType", ai.getAttackType());
        ipDetail(ai, enrichedIps, verdict);
        s.setAiVerdict(JSON.toJSONString(verdict));
        return s;
    }

    private void createAlert(String ip, String userId, AiAnalysisResult ai, List<SuspiciousIp> suspiciousIps) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "attack");
            alert.put("level", severityToLevel(ai.getSeverity()));
            alert.put("status", 0);
            alert.put("userId", userId);
            // 攻击来源 IP：可疑来源（逗号分隔），SNMP 无来源时退回目标 IP
            String attackerIp = attackIpAddress(ip, suspiciousIps);
            String msg = "检测到" + ai.getSeverity() + "级别Slowloris攻击，攻击来源IP: " + attackerIp;
            alert.put("description", msg);
            alert.put("message", msg);
            alert.put("ipAddress", attackerIp);
            Map<String, Object> details = new HashMap<>();
            ipDetail(ai, suspiciousIps, details);
            alert.put("details", JSON.toJSONString(details));
            alertLogClient.createAlert(alert);
        } catch (Exception e) {
            log.warn("创建告警失败: {}", e.getMessage());
        }
    }

    private void ipDetail(AiAnalysisResult ai, List<SuspiciousIp> suspiciousIps, Map<String, Object> details) {
        details.put("severity", ai.getSeverity());
        details.put("confidence", ai.getConfidence());
        details.put("riskScore", ai.getRiskScore());
        details.put("reasoning", ai.getReasoning());
        details.put("recommendations", ai.getRecommendations());
        details.put("suspiciousIps", suspiciousIps);
    }

    private void createLog(String ip, String userId, AiAnalysisResult ai, List<SuspiciousIp> suspiciousIps) {
        try {
            Map<String, Object> log2 = new HashMap<>();
            log2.put("type", 2);
            log2.put("operator", "system");
            log2.put("userId", userId);
            // ipAddress 是 String 字段：用可疑来源 IP（逗号分隔），无来源时退回目标 IP
            log2.put("ipAddress", attackIpAddress(ip, suspiciousIps));
            log2.put("module", "攻击监控");
            log2.put("description", "DeepSeek检测到攻击：" + ai.getReasoning());
            log2.put("method", "SCHEDULED");
            log2.put("requestUrl", "/monitor/poll");
            log2.put("statusCode", 200);
            alertLogClient.createLog(log2);
        } catch (Exception e) {
            log.warn("创建日志失败: {}", e.getMessage());
        }
    }

    private int severityToLevel(String severity) {
        return switch (severity != null ? severity : "low") {
            case "critical" -> 4;
            case "high" -> 3;
            case "medium" -> 2;
            default -> 1;
        };
    }

    /**
     * 计算告警/日志的 ipAddress 字段值（String）。
     * 优先使用解析到的可疑来源 IP（逗号分隔）；SNMP 采集无法获取来源 IP 时，
     * 退回被监控的目标 IP，保证该字段始终为合法字符串，避免下游反序列化失败。
     */
    private String attackIpAddress(String targetIp, List<SuspiciousIp> suspiciousIps) {
        if (suspiciousIps != null && !suspiciousIps.isEmpty()) {
            String joined = suspiciousIps.stream()
                    .map(SuspiciousIp::getIpAddress)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .collect(Collectors.joining(","));
            if (!joined.isBlank()) {
                return joined;
            }
        }
        return targetIp;
    }
}
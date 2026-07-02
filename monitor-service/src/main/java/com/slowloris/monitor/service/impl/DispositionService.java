package com.slowloris.monitor.service.impl;

import com.alibaba.fastjson2.JSON;
import com.slowloris.monitor.engine.AgentEventPublisher;
import com.slowloris.monitor.entity.*;
import com.slowloris.monitor.mapper.AgentConfigMapper;
import com.slowloris.monitor.mapper.AttackDetectionMapper;
import com.slowloris.monitor.mapper.BlockedIpMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 决策与处置服务（agent 闭环的「决策→处置」环节）。
 * 输入 AI 分析结论，依据 {@link AgentConfig} 自动模式与阈值，决定：
 *   - 记录攻击检测；
 *   - 自动模式开启且达阈值 → 软封禁可疑来源 IP（仅落库，不操作真实防火墙，可解封）；
 *   - 否则 → 记录为「建议处置」，等待人工确认。
 * 所有决策通过 {@link AgentEventPublisher} 实时推送。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispositionService {

    private final AgentConfigMapper configMapper;
    private final AttackDetectionMapper detectionMapper;
    private final BlockedIpMapper blockedIpMapper;
    private final AgentEventPublisher eventPublisher;

    private static final Map<String, Integer> SEVERITY_RANK = Map.of(
            "low", 1, "medium", 2, "high", 3, "critical", 4);

    /** 获取配置（始终返回 id=1 单行，缺失时返回内存默认值）。 */
    public AgentConfig getConfig() {
        AgentConfig cfg = configMapper.selectById(1L);
        if (cfg == null) {
            cfg = new AgentConfig();
            cfg.setId(1L);
            cfg.setAutoMode(0);
            cfg.setBlockRiskScore(80);
            cfg.setBlockSeverity("high");
            cfg.setBlockMinConfidence(0.7);
            cfg.setAutoUnblockMinutes(0);
        }
        return cfg;
    }

    public AgentConfig updateConfig(AgentConfig patch, String operator) {
        AgentConfig cfg = configMapper.selectById(1L);
        boolean isNew = (cfg == null);
        if (isNew) {
            cfg = getConfig();
        }
        if (patch.getAutoMode() != null)           cfg.setAutoMode(patch.getAutoMode());
        if (patch.getBlockRiskScore() != null)     cfg.setBlockRiskScore(patch.getBlockRiskScore());
        if (patch.getBlockSeverity() != null)      cfg.setBlockSeverity(patch.getBlockSeverity());
        if (patch.getBlockMinConfidence() != null) cfg.setBlockMinConfidence(patch.getBlockMinConfidence());
        if (patch.getAutoUnblockMinutes() != null) cfg.setAutoUnblockMinutes(patch.getAutoUnblockMinutes());
        cfg.setUpdatedBy(operator);

        if (isNew) {
            configMapper.insert(cfg);
        } else {
            configMapper.updateById(cfg);
        }
        eventPublisher.broadcast("config", toMap(cfg));
        return cfg;
    }

    /**
     * 闭环决策入口。仅在 AI 判定为攻击时调用。
     *
     * @param targetIp      监控目标 IP
     * @param ownerUserId   监控目标归属用户ID（决策/封禁记录随此归属，用于多用户隔离）
     * @param ai            AI 分析结论
     * @param suspiciousIps 经地理富化的可疑来源 IP
     * @return 本次决策摘要（已落库 + 已推送）
     */
    public Map<String, Object> decideAndDispose(String targetIp, String ownerUserId, AiAnalysisResult ai,
                                                List<SuspiciousIp> suspiciousIps) {
        AgentConfig cfg = getConfig();

        // 1. 记录攻击检测（ipAddress 记录攻击来源 IP；无来源时退回目标 IP）
        AttackDetection det = new AttackDetection();
        det.setIpAddress(attackerIpAddress(suspiciousIps, targetIp));
        det.setUserId(ownerUserId);
        det.setType(ai.getAttackType() != null ? ai.getAttackType() : "slowloris");
        det.setSeverity(ai.getSeverity());
        det.setConfidence(ai.getConfidence());
        det.setRiskScore(ai.getRiskScore());
        det.setReasoning(ai.getReasoning());
        det.setRecommendations(JSON.toJSONString(ai.getRecommendations()));
        det.setDetectedAt(LocalDateTime.now());

        boolean shouldAutoBlock = cfg.getAutoMode() != null && cfg.getAutoMode() == 1
                && meetsThreshold(ai, cfg);

        List<String> blockedNow = new ArrayList<>();
        String action;

        if (shouldAutoBlock) {
            // 2a. 自动软封禁可疑来源 IP（无来源时退化为封禁目标 IP 本身的告警占位）
            List<String> candidates = collectCandidateIps(suspiciousIps, targetIp);
            for (String ip : candidates) {
                if (softBlock(ip, targetIp, ownerUserId, ai, "agent", true)) {
                    blockedNow.add(ip);
                }
            }
            det.setStatus("blocked");
            action = blockedNow.isEmpty() ? "auto_no_target" : "auto_block";
        } else {
            // 2b. 仅建议，等待人工处置
            det.setStatus("active");
            action = "advise";
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", action);
        details.put("autoMode", cfg.getAutoMode());
        details.put("blockedIps", blockedNow);
        details.put("suspiciousIps", suspiciousIps);
        det.setDetails(JSON.toJSONString(details));
        detectionMapper.insert(det);

        // 3. 实时推送决策事件（仅归属用户与管理员可见）
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("id", det.getId());
        event.put("targetIp", targetIp);
        event.put("action", action);
        event.put("severity", ai.getSeverity());
        event.put("riskScore", ai.getRiskScore());
        event.put("confidence", ai.getConfidence());
        event.put("blockedIps", blockedNow);
        event.put("reasoning", ai.getReasoning());
        event.put("recommendations", ai.getRecommendations());
        event.put("timestamp", System.currentTimeMillis());
        eventPublisher.broadcastToOwner("disposition", event, ownerUserId);

        log.info("[决策] 目标={} 归属={} 动作={} 自动模式={} 封禁数={}",
                targetIp, ownerUserId, action, cfg.getAutoMode(), blockedNow.size());
        return event;
    }

    /** 人工/自动软封禁单个 IP。该用户范围内已在封禁中则跳过。返回是否新增封禁。 */
    public boolean softBlock(String ip, String targetIp, String ownerUserId, AiAnalysisResult ai,
                             String operator, boolean auto) {
        if (ip == null || ip.isBlank()) return false;
        BlockedIp existing = blockedIpMapper.findActiveByIpAndUser(ip, ownerUserId);
        if (existing != null) return false;

        BlockedIp b = new BlockedIp();
        b.setIpAddress(ip);
        b.setTargetIp(targetIp);
        b.setUserId(ownerUserId);
        b.setStatus(1);
        b.setAuto(auto ? 1 : 0);
        b.setOperator(operator);
        b.setBlockTime(LocalDateTime.now());
        if (ai != null) {
            b.setRiskScore(ai.getRiskScore());
            b.setSeverity(ai.getSeverity());
            b.setReason((auto ? "自动封禁：" : "人工封禁：")
                    + (ai.getReasoning() != null ? ai.getReasoning() : "检出攻击"));
        } else {
            b.setReason(auto ? "自动封禁" : "人工封禁");
        }
        blockedIpMapper.insert(b);
        eventPublisher.broadcastToOwner("block", toMap(b), ownerUserId);
        return true;
    }

    /** 人工封禁入口（前端调用）。封禁记录归属操作用户。 */
    public boolean manualBlock(String ip, String targetIp, String ownerUserId, String operator, String reason) {
        if (ip == null || ip.isBlank()) return false;
        if (blockedIpMapper.findActiveByIpAndUser(ip, ownerUserId) != null) return false;
        BlockedIp b = new BlockedIp();
        b.setIpAddress(ip);
        b.setTargetIp(targetIp);
        b.setUserId(ownerUserId);
        b.setStatus(1);
        b.setAuto(0);
        b.setOperator(operator != null ? operator : "manual");
        b.setReason(reason != null && !reason.isBlank() ? reason : "人工封禁");
        b.setBlockTime(LocalDateTime.now());
        blockedIpMapper.insert(b);
        eventPublisher.broadcastToOwner("block", toMap(b), ownerUserId);
        return true;
    }

    /** 解封。仅解封该用户范围内的封禁记录（admin 可解封任意记录）。返回是否成功解封一条。 */
    public boolean unblock(String ip, String ownerUserId, boolean admin, String operator) {
        BlockedIp b = admin ? blockedIpMapper.findActiveByIp(ip)
                            : blockedIpMapper.findActiveByIpAndUser(ip, ownerUserId);
        if (b == null) return false;
        b.setStatus(0);
        b.setUnblockTime(LocalDateTime.now());
        b.setUnblockBy(operator != null ? operator : "manual");
        blockedIpMapper.updateById(b);
        eventPublisher.broadcastToOwner("unblock", toMap(b), b.getUserId());
        return true;
    }

    public List<BlockedIp> listBlocked(String userId, boolean admin) {
        return blockedIpMapper.findActiveScoped(userId, admin);
    }

    public int activeBlockCount(String userId, boolean admin) {
        return blockedIpMapper.countActiveScoped(userId, admin);
    }

    public List<AttackDetection> recentDetections(String userId, boolean admin, int limit) {
        return detectionMapper.findRecentScoped(userId, admin, limit);
    }

    public int activeDetectionCount(String userId, boolean admin) {
        return detectionMapper.countActiveScoped(userId, admin);
    }

    /** 删除单条决策记录。返回是否删除成功。 */
    public boolean deleteDetection(Long id) {
        if (id == null) return false;
        return detectionMapper.deleteById(id) > 0;
    }

    /** 批量删除决策记录。返回删除条数。 */
    public int deleteDetections(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        return detectionMapper.deleteBatchIds(ids);
    }

    public boolean isBlocked(String ip) {
        return blockedIpMapper.findActiveByIp(ip) != null;
    }

    /** 该用户范围内某 IP 是否封禁中（admin 视全局）。 */
    public boolean isBlocked(String ip, String userId, boolean admin) {
        return admin ? blockedIpMapper.findActiveByIp(ip) != null
                     : blockedIpMapper.findActiveByIpAndUser(ip, userId) != null;
    }

    /**
     * 自动解封到期记录（由 MonitoringEngine 周期触发）。返回解封数量。
     */
    public int autoUnblockExpired() {
        AgentConfig cfg = getConfig();
        int minutes = cfg.getAutoUnblockMinutes() != null ? cfg.getAutoUnblockMinutes() : 0;
        if (minutes <= 0) return 0;

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(minutes);
        List<BlockedIp> active = blockedIpMapper.findAllActive();
        int count = 0;
        for (BlockedIp b : active) {
            if (b.getAuto() != null && b.getAuto() == 1
                    && b.getBlockTime() != null && b.getBlockTime().isBefore(threshold)) {
                b.setStatus(0);
                b.setUnblockTime(LocalDateTime.now());
                b.setUnblockBy("agent-auto");
                blockedIpMapper.updateById(b);
                eventPublisher.broadcastToOwner("unblock", toMap(b), b.getUserId());
                count++;
            }
        }
        if (count > 0) log.info("[自动解封] 解封 {} 个到期 IP", count);
        return count;
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private boolean meetsThreshold(AiAnalysisResult ai, AgentConfig cfg) {
        int riskOk = ai.getRiskScore() >= (cfg.getBlockRiskScore() != null ? cfg.getBlockRiskScore() : 80) ? 1 : 0;
        int sevRank = SEVERITY_RANK.getOrDefault(ai.getSeverity() != null ? ai.getSeverity() : "low", 1);
        int needRank = SEVERITY_RANK.getOrDefault(cfg.getBlockSeverity() != null ? cfg.getBlockSeverity() : "high", 3);
        boolean sevOk = sevRank >= needRank;
        double minConf = cfg.getBlockMinConfidence() != null ? cfg.getBlockMinConfidence() : 0.7;
        boolean confOk = ai.getConfidence() >= minConf;
        // 风险分达标 或（严重级别达标 且 置信度达标）
        return (riskOk == 1 && confOk) || (sevOk && confOk);
    }

    /**
     * 计算决策记录的攻击来源 IP（String）。
     * 优先使用解析到的可疑来源 IP（逗号分隔）；SNMP 采集无法获取来源 IP 时，
     * 退回被监控的目标 IP，保证该字段始终为合法字符串。
     */
    private String attackerIpAddress(List<SuspiciousIp> suspiciousIps, String targetIp) {
        if (suspiciousIps != null && !suspiciousIps.isEmpty()) {
            String joined = suspiciousIps.stream()
                    .map(SuspiciousIp::getIpAddress)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(","));
            if (!joined.isBlank()) {
                return joined;
            }
        }
        return targetIp;
    }

    private List<String> collectCandidateIps(List<SuspiciousIp> suspiciousIps, String targetIp) {
        List<String> ips = new ArrayList<>();
        if (suspiciousIps != null) {
            for (SuspiciousIp s : suspiciousIps) {
                if (s.getIpAddress() != null && !s.getIpAddress().isBlank()) {
                    ips.add(s.getIpAddress());
                }
            }
        }
        return ips;
    }

    private Map<String, Object> toMap(Object o) {
        return JSON.parseObject(JSON.toJSONString(o));
    }
}

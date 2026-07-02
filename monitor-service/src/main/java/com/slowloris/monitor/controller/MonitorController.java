package com.slowloris.monitor.controller;

import com.slowloris.monitor.engine.*;
import com.slowloris.monitor.entity.*;
import com.slowloris.monitor.mapper.MetricsSnapshotMapper;
import com.slowloris.monitor.mapper.MonitoredTargetMapper;
import com.slowloris.common.Result;
import com.slowloris.monitor.service.impl.DeepSeekAnalysisService;
import com.slowloris.monitor.service.impl.DispositionService;
import com.slowloris.monitor.service.impl.AgentReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitoringEngine engine;
    private final MetricsSnapshotMapper snapshotMapper;
    private final MonitoredTargetMapper targetMapper;
    private final DeepSeekAnalysisService aiService;
    private final DispositionService dispositionService;
    private final AgentEventPublisher eventPublisher;
    private final AgentReportService agentReportService;

    private static final String IP_REGEX = "^\\d{1,3}(\\.\\d{1,3}){3}$";

    /** 角色头是否包含管理员。管理员可见全部用户的数据。 */
    private boolean isAdmin(String roles) {
        return roles != null && roles.toUpperCase().contains("ADMIN");
    }

    @PostMapping("/start")
    public Result<String> start(@RequestBody Map<String, String> body,
                                @RequestHeader(value = "user-id", required = false) String userId) {
        String ip = body.get("ip");
        if (ip == null || !ip.matches(IP_REGEX)) {
            return Result.error("无效的IP地址格式");
        }
        engine.startMonitoring(ip, userId);
        return Result.success("已开始监控 " + ip);
    }

    @PostMapping("/stop")
    public Result<String> stop(@RequestBody Map<String, String> body,
                               @RequestHeader(value = "user-id", required = false) String userId) {
        String ip = body.get("ip");
        if (ip == null) return Result.error("IP不能为空");
        engine.stopMonitoring(ip, userId);
        return Result.success("已停止监控 " + ip);
    }

    @GetMapping("/status/{ip}")
    public Result<Map<String, Object>> status(@PathVariable String ip,
                                              @RequestHeader(value = "user-id", required = false) String userId,
                                              @RequestHeader(value = "roles", required = false) String roles) {
        boolean admin = isAdmin(roles);
        MetricsSnapshot latest = snapshotMapper.findLatestByIpScoped(ip, userId, admin);
        boolean active = engine.isMonitoring(ip, userId) || (admin && engine.isMonitoringAny(ip));
        Map<String, Object> data = new HashMap<>();
        data.put("ip", ip);
        data.put("active", active);
        data.put("latestSnapshot", latest);
        data.put("blocked", dispositionService.isBlocked(ip, userId, admin));
        data.put("agentOnline", agentReportService.isOnline(ip));
        data.put("agentLastSeen", agentReportService.lastSeen(ip));
        return Result.success(data);
    }

    @GetMapping("/predict/{ip}")
    public Result<List<PredictionPoint>> predict(@PathVariable String ip,
                                                 @RequestHeader(value = "user-id", required = false) String userId,
                                                 @RequestHeader(value = "roles", required = false) String roles) {
        List<MetricsSnapshot> history = snapshotMapper.findLast24hScoped(ip, userId, isAdmin(roles));
        if (history.isEmpty()) {
            return Result.error("没有足够的历史数据进行预测，请先开始监控并等待数据积累");
        }
        List<PredictionPoint> prediction = aiService.predict(ip, history);
        return Result.success(prediction);
    }

    @GetMapping("/targets")
    public Result<List<MonitoredTarget>> listTargets(@RequestHeader(value = "user-id", required = false) String userId,
                                                     @RequestHeader(value = "roles", required = false) String roles) {
        return Result.success(isAdmin(roles)
                ? targetMapper.findAllActive()
                : targetMapper.findActiveByUser(userId));
    }

    // ── Agent 自动处置闭环 ────────────────────────────────────────────────

    /** 获取自动处置配置 */
    @GetMapping("/config")
    public Result<AgentConfig> getConfig() {
        return Result.success(dispositionService.getConfig());
    }

    /** 更新自动处置配置（自动模式开关、阈值等） */
    @PutMapping("/config")
    public Result<AgentConfig> updateConfig(@RequestBody AgentConfig patch,
                                            @RequestHeader(value = "username", required = false) String username) {
        AgentConfig updated = dispositionService.updateConfig(patch, username != null ? username : "unknown");
        return Result.success(updated, "配置已更新");
    }

    /** 封禁中的 IP 列表（普通用户仅见自己范围，管理员见全部） */
    @GetMapping("/blocked")
    public Result<List<BlockedIp>> listBlocked(@RequestHeader(value = "user-id", required = false) String userId,
                                               @RequestHeader(value = "roles", required = false) String roles) {
        return Result.success(dispositionService.listBlocked(userId, isAdmin(roles)));
    }

    /** 人工封禁 IP（归属当前用户） */
    @PostMapping("/block")
    public Result<String> block(@RequestBody Map<String, String> body,
                                @RequestHeader(value = "user-id", required = false) String userId,
                                @RequestHeader(value = "username", required = false) String username) {
        String ip = body.get("ip");
        if (ip == null || !ip.matches(IP_REGEX)) return Result.error("无效的IP地址格式");
        boolean ok = dispositionService.manualBlock(ip, body.get("targetIp"), userId,
                username != null ? username : "manual", body.get("reason"));
        return ok ? Result.success("已封禁 " + ip) : Result.error("该IP已在封禁中");
    }

    /** 解封 IP（普通用户仅可解封自己范围内的封禁，管理员可解封任意） */
    @PostMapping("/unblock")
    public Result<String> unblock(@RequestBody Map<String, String> body,
                                  @RequestHeader(value = "user-id", required = false) String userId,
                                  @RequestHeader(value = "roles", required = false) String roles,
                                  @RequestHeader(value = "username", required = false) String username) {
        String ip = body.get("ip");
        if (ip == null) return Result.error("IP不能为空");
        boolean ok = dispositionService.unblock(ip, userId, isAdmin(roles),
                username != null ? username : "manual");
        return ok ? Result.success("已解封 " + ip) : Result.error("未找到封禁中的该IP");
    }

    /** 近期攻击检测/决策记录（普通用户仅见自己范围，管理员见全部） */
    @GetMapping("/detections")
    public Result<List<AttackDetection>> detections(@RequestParam(defaultValue = "50") int limit,
                                                    @RequestHeader(value = "user-id", required = false) String userId,
                                                    @RequestHeader(value = "roles", required = false) String roles) {
        return Result.success(dispositionService.recentDetections(userId, isAdmin(roles), Math.min(limit, 200)));
    }

    /** 删除单条决策记录（仅管理员） */
    @DeleteMapping("/detections/{id}")
    public Result<String> deleteDetection(@PathVariable Long id,
                                          @RequestHeader(value = "roles", required = false) String roles) {
        if (notAdmin(roles)) return Result.error("无权限，仅管理员可删除");
        return dispositionService.deleteDetection(id)
                ? Result.success("决策记录已删除") : Result.error("记录不存在");
    }

    /** 批量删除决策记录（仅管理员） */
    @DeleteMapping("/detections")
    public Result<String> deleteDetections(@RequestBody Map<String, Object> body,
                                           @RequestHeader(value = "roles", required = false) String roles) {
        if (notAdmin(roles)) return Result.error("无权限，仅管理员可删除");
        List<Long> ids = new java.util.ArrayList<>();
        Object raw = body.get("ids");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (o != null) {
                    try { ids.add(Long.parseLong(o.toString())); } catch (NumberFormatException ignored) { }
                }
            }
        }
        if (ids.isEmpty()) return Result.error("未指定要删除的记录");
        int n = dispositionService.deleteDetections(ids);
        return Result.success("已删除 " + n + " 条决策记录");
    }

    private boolean notAdmin(String roles) {
        return roles == null || !roles.toUpperCase().contains("ADMIN");
    }

    /** Agent 闭环总览统计（普通用户仅统计自己范围，管理员统计全部） */
    @GetMapping("/agent/overview")
    public Result<Map<String, Object>> overview(@RequestHeader(value = "user-id", required = false) String userId,
                                                @RequestHeader(value = "roles", required = false) String roles) {
        boolean admin = isAdmin(roles);
        Map<String, Object> data = new HashMap<>();
        AgentConfig cfg = dispositionService.getConfig();
        data.put("autoMode", cfg.getAutoMode());
        data.put("config", cfg);
        data.put("activeTargets", admin
                ? targetMapper.findAllActive().size()
                : targetMapper.findActiveByUser(userId).size());
        data.put("blockedCount", dispositionService.activeBlockCount(userId, admin));
        data.put("sseConnections", eventPublisher.connectionCount());
        return Result.success(data);
    }

    /** SSE 实时事件订阅：analysis / disposition / block / unblock / config（按归属用户隔离推送） */
    @GetMapping("/stream")
    public SseEmitter stream(@RequestHeader(value = "user-id", required = false) String userId,
                             @RequestHeader(value = "roles", required = false) String roles) {
        return eventPublisher.subscribe(userId, isAdmin(roles));
    }
}

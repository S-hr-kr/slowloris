package com.slowloris.alertlog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slowloris.alertlog.entity.Alert;
import com.slowloris.alertlog.entity.SysLog;
import com.slowloris.alertlog.mapper.AlertMapper;
import com.slowloris.alertlog.mapper.SysLogMapper;
import com.slowloris.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AlertLogController {

    private final AlertMapper alertMapper;
    private final SysLogMapper sysLogMapper;


    @GetMapping("/alerts")
    public Result<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer level,
            @RequestHeader(value = "user-id", required = false) String userId,
            @RequestHeader(value = "roles", required = false) String roles) {

        boolean admin = isAdmin(roles);
        LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<Alert>()
                .orderByDesc(Alert::getCreateTime);
        if (status != null) wrapper.eq(Alert::getStatus, status);
        if (level != null) wrapper.eq(Alert::getLevel, level);
        // 多用户隔离：普通用户仅见自己归属的告警；管理员见全部
        if (!admin) wrapper.eq(Alert::getUserId, userId);

        Page<Alert> pageResult = alertMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", pageResult.getRecords());
        data.put("total", pageResult.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("unreadCount", alertMapper.countUnprocessedScoped(userId, admin));
        return Result.success(data);
    }

    @PutMapping("/alerts/{id}")
    public Result<String> updateAlert(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Alert alert = alertMapper.selectById(id);
        if (alert == null) return Result.error("告警不存在");
        Object statusVal = body.get("status");
        if (statusVal != null) {
            alert.setStatus(Integer.parseInt(statusVal.toString()));
            if (alert.getStatus() == 1) {
                alert.setHandleTime(LocalDateTime.now());
                Object handler = body.get("handler");
                if (handler != null) alert.setHandler(handler.toString());
            }
        }
        alertMapper.updateById(alert);
        return Result.success("更新成功");
    }

    @GetMapping("/logs")
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer type,
            @RequestHeader(value = "user-id", required = false) String userId,
            @RequestHeader(value = "roles", required = false) String roles) {

        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .orderByDesc(SysLog::getCreateTime);
        if (type != null) wrapper.eq(SysLog::getType, type);
        // 多用户隔离：普通用户仅见自己归属的日志；管理员见全部
        if (!isAdmin(roles)) wrapper.eq(SysLog::getUserId, userId);

        Page<SysLog> pageResult = sysLogMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", pageResult.getRecords());
        data.put("total", pageResult.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return Result.success(data);
    }

    /** 角色头是否包含管理员。管理员可见/可删全部用户数据。 */
    private boolean isAdmin(String roles) {
        return roles != null && roles.toUpperCase().contains("ADMIN");
    }

    // ── 删除（仅管理员）──────────────────────────────────────────────

    /** 删除单条告警 */
    @DeleteMapping("/alerts/{id}")
    public Result<String> deleteAlert(@PathVariable Long id,
                                      @RequestHeader(value = "roles", required = false) String roles) {
        if (notAdmin(roles)) return Result.error("无权限，仅管理员可删除");
        if (alertMapper.selectById(id) == null) return Result.error("告警不存在");
        alertMapper.deleteById(id);
        return Result.success("告警已删除");
    }

    /** 批量删除告警（ids 为逗号分隔或 JSON 数组传入的 List） */
    @DeleteMapping("/alerts")
    public Result<String> deleteAlerts(@RequestBody Map<String, Object> body,
                                       @RequestHeader(value = "roles", required = false) String roles) {
        if (notAdmin(roles)) return Result.error("无权限，仅管理员可删除");
        List<Long> ids = extractIds(body.get("ids"));
        if (ids.isEmpty()) return Result.error("未指定要删除的告警");
        alertMapper.deleteBatchIds(ids);
        return Result.success("已删除 " + ids.size() + " 条告警");
    }

    /** 删除单条日志 */
    @DeleteMapping("/logs/{id}")
    public Result<String> deleteLog(@PathVariable Long id,
                                    @RequestHeader(value = "roles", required = false) String roles) {
        if (notAdmin(roles)) return Result.error("无权限，仅管理员可删除");
        if (sysLogMapper.selectById(id) == null) return Result.error("日志不存在");
        sysLogMapper.deleteById(id);
        return Result.success("日志已删除");
    }

    /** 批量删除日志 */
    @DeleteMapping("/logs")
    public Result<String> deleteLogs(@RequestBody Map<String, Object> body,
                                     @RequestHeader(value = "roles", required = false) String roles) {
        if (notAdmin(roles)) return Result.error("无权限，仅管理员可删除");
        List<Long> ids = extractIds(body.get("ids"));
        if (ids.isEmpty()) return Result.error("未指定要删除的日志");
        sysLogMapper.deleteBatchIds(ids);
        return Result.success("已删除 " + ids.size() + " 条日志");
    }

    private boolean notAdmin(String roles) {
        return roles == null || !roles.toUpperCase().contains("ADMIN");
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractIds(Object raw) {
        List<Long> ids = new java.util.ArrayList<>();
        if (raw instanceof List) {
            for (Object o : (List<Object>) raw) {
                if (o != null) {
                    try { ids.add(Long.parseLong(o.toString())); } catch (NumberFormatException ignored) { }
                }
            }
        } else if (raw instanceof String) {
            for (String s : ((String) raw).split(",")) {
                if (!s.isBlank()) {
                    try { ids.add(Long.parseLong(s.trim())); } catch (NumberFormatException ignored) { }
                }
            }
        }
        return ids;
    }


    @PostMapping("/internal/alerts")
    public Result<String> createAlert(@RequestBody Alert alert) {
        alertMapper.insert(alert);
        return Result.success("告警已记录");
    }

    @PostMapping("/internal/logs")
    public Result<String> createLog(@RequestBody SysLog sysLog) {
        sysLogMapper.insert(sysLog);
        return Result.success("日志已记录");
    }
}

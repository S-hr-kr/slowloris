package com.slowloris.alert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slowloris.alert.entity.Alert;
import com.slowloris.alert.mapper.AlertMapper;
import com.slowloris.alert.service.AlertService;
import com.slowloris.alert.vo.AlertStatsVo;
import com.slowloris.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AlertServiceImpl extends ServiceImpl<AlertMapper, Alert> implements AlertService {

    @Autowired
    private AlertMapper alertMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_ALERT_UNPROCESSED_COUNT = "alert:unprocessed:count";
    private static final String REDIS_KEY_ALERT_RECENT = "alert:recent:";
    private static final String REDIS_KEY_ALERT_STATS = "alert:stats";

    @Override
    public Long createAlert(Alert alert) {
        if (alert.getStatus() == null) {
            alert.setStatus(0);
        }
        if (alert.getCreateTime() == null) {
            alert.setCreateTime(LocalDateTime.now());
        }

        save(alert);

        redisTemplate.delete(REDIS_KEY_ALERT_UNPROCESSED_COUNT);
        redisTemplate.delete(REDIS_KEY_ALERT_STATS);

        return alert.getId();
    }

    @Override
    public Integer getUnprocessedCount() {
        Integer count = (Integer) redisTemplate.opsForValue().get(REDIS_KEY_ALERT_UNPROCESSED_COUNT);
        if (count != null) {
            return count;
        }

        count = alertMapper.countUnprocessed();

        redisTemplate.opsForValue().set(
                REDIS_KEY_ALERT_UNPROCESSED_COUNT,
                count,
                5L,
                TimeUnit.MINUTES
        );

        return count;
    }

    @Override
    public List<Alert> getRecentAlerts(int limit) {
        List<Alert> alerts = (List<Alert>) redisTemplate.opsForValue().get(REDIS_KEY_ALERT_RECENT + limit);
        if (alerts != null) {
            return alerts;
        }

        alerts = alertMapper.selectRecentAlerts(limit);

        redisTemplate.opsForValue().set(
                REDIS_KEY_ALERT_RECENT + limit,
                alerts,
                5L,
                TimeUnit.MINUTES
        );

        return alerts;
    }

    @Override
    public AlertStatsVo getAlertStats() {
        AlertStatsVo stats = (AlertStatsVo) redisTemplate.opsForValue().get(REDIS_KEY_ALERT_STATS);
        if (stats != null) {
            return stats;
        }

        stats = new AlertStatsVo();
        Long totalCount = count();
        stats.setTotalCount(totalCount);

        Integer unprocessed = getUnprocessedCount();
        stats.setUnprocessedCount(unprocessed);
        stats.setProcessedCount(count("status", 1));
        stats.setIgnoredCount(count("status", 2));

        stats.setTodayCount(alertMapper.countTodayAlerts());
        stats.setTypeStats(countByType());
        stats.setLevelStats(countByLevel());

        List<Map<String, Object>> dbDailyTrend = alertMapper.countDailyAlerts(7);
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        Map<String, Integer> dateCountMap = new HashMap<>();
        for (Map<String, Object> item : dbDailyTrend) {
            dateCountMap.put(item.get("date").toString(), ((Number) item.get("count")).intValue());
        }
        for (int i = 6; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);
            dayData.put("count", dateCountMap.getOrDefault(dateStr, 0));
            dailyTrend.add(dayData);
        }
        stats.setDailyTrend(dailyTrend);

        redisTemplate.opsForValue().set(
                REDIS_KEY_ALERT_STATS,
                stats,
                5L,
                TimeUnit.MINUTES
        );

        return stats;
    }

    @Override
    public boolean updateAlertStatus(Long id, Integer status, String handler, String remark) {
        Alert alert = getById(id);
        if (alert == null) {
            return false;
        }

        alert.setStatus(status);
        alert.setHandler(handler);
        alert.setHandleRemark(remark);
        alert.setHandleTime(LocalDateTime.now());

        boolean success = updateById(alert);

        if (success) {
            deleteKeysByPattern(REDIS_KEY_ALERT_RECENT + "*");
            redisTemplate.delete(REDIS_KEY_ALERT_UNPROCESSED_COUNT);
            redisTemplate.delete(REDIS_KEY_ALERT_STATS);
        }

        return success;
    }

    @Override
    public List<Alert> searchAlerts(Map<String, Object> params) {
        LambdaQueryWrapper<Alert> queryWrapper = new LambdaQueryWrapper<>();
        
        // 根据告警级别过滤
        if (params.containsKey("level")) {
            queryWrapper.eq(Alert::getLevel, params.get("level"));
        }
        
        // 根据告警状态过滤
        if (params.containsKey("status")) {
            queryWrapper.eq(Alert::getStatus, params.get("status"));
        }
        
        // 根据IP地址过滤
        if (params.containsKey("ipAddress")) {
            queryWrapper.eq(Alert::getIpAddress, params.get("ipAddress"));
        }
        
        // 根据攻击类型过滤
        if (params.containsKey("type")) {
            queryWrapper.eq(Alert::getType, params.get("type"));
        }
        
        // 根据时间范围过滤
        if (params.containsKey("startTime")) {
            queryWrapper.ge(Alert::getCreateTime, params.get("startTime"));
        }
        if (params.containsKey("endTime")) {
            queryWrapper.le(Alert::getCreateTime, params.get("endTime"));
        }
        
        queryWrapper.orderByDesc(Alert::getCreateTime);
        
        return list(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> countByType() {
        return alertMapper.countByType();
    }

    @Override
    public List<Map<String, Object>> countByLevel() {
        return alertMapper.countByLevel();
    }

    private void deleteKeysByPattern(String pattern) {
        try {
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to delete keys with pattern: {}", pattern, e);
        }
    }

    private int count(String column, Object value) {
        LambdaQueryWrapper<Alert> queryWrapper = new LambdaQueryWrapper<>();
        if ("status".equals(column)) {
            queryWrapper.eq(Alert::getStatus, value);
        }
        return Math.toIntExact(count(queryWrapper));
    }

    @Override
    public Result<Map<String, Object>> getAlertList(Integer page, Integer limit, String severity, String status) {
        try {
            LambdaQueryWrapper<Alert> queryWrapper = new LambdaQueryWrapper<>();
            if (severity != null) {
                Integer levelInt = "info".equals(severity) ? 1 : "warning".equals(severity) ? 2 : "error".equals(severity) ? 3 : 4;
                queryWrapper.eq(Alert::getLevel, levelInt);
            }
            if (status != null) {
                Integer statusInt = "unhandled".equals(status) ? 0 : "handled".equals(status) ? 1 : 2;
                queryWrapper.eq(Alert::getStatus, statusInt);
            }
            queryWrapper.orderByDesc(Alert::getCreateTime);

            Page<Alert> pageResult = page(new Page<>(page, limit), queryWrapper);
            Map<String, Object> data = new HashMap<>();
            data.put("total", pageResult.getTotal());
            List<Map<String, Object>> list = new ArrayList<>();
            for (Alert alert : pageResult.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", alert.getId());
                item.put("title", alert.getDescription());
                String levelStr = alert.getLevel() == 1 ? "info" : alert.getLevel() == 2 ? "warning" : alert.getLevel() == 3 ? "error" : "high";
                item.put("severity", levelStr);
                String statusStr = alert.getStatus() == 0 ? "unhandled" : alert.getStatus() == 1 ? "handled" : "ignored";
                item.put("status", statusStr);
                item.put("ip", alert.getIpAddress());
                item.put("created_at", alert.getCreateTime() != null ? alert.getCreateTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
                item.put("message", alert.getDescription());
                list.add(item);
            }
            data.put("list", list);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get alert list: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> getAlertDetail(Long id) {
        try {
            Alert alert = getById(id);
            if (alert == null) {
                return Result.error("告警不存在");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("id", alert.getId());
            data.put("title", alert.getDescription());
            String levelStr = alert.getLevel() == 1 ? "info" : alert.getLevel() == 2 ? "warning" : alert.getLevel() == 3 ? "error" : "high";
            data.put("severity", levelStr);
            String statusStr = alert.getStatus() == 0 ? "unhandled" : alert.getStatus() == 1 ? "handled" : "ignored";
            data.put("status", statusStr);
            data.put("ip", alert.getIpAddress());
            data.put("created_at", alert.getCreateTime() != null ? alert.getCreateTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
            data.put("updated_at", alert.getUpdateTime() != null ? alert.getUpdateTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
            data.put("message", alert.getDescription());
            Map<String, Object> details = new HashMap<>();
            details.put("attack_type", alert.getType());
            details.put("port", alert.getPort());
            details.put("raw_details", alert.getDetails());
            data.put("details", details);
            data.put("actions", new ArrayList<>());
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get alert detail: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> handleAlert(Long id, Map<String, Object> params) {
        try {
            Alert alert = getById(id);
            if (alert == null) {
                return Result.error("告警不存在");
            }
            String status = (String) params.get("status");
            if (status == null) {
                return Result.error("请求参数错误");
            }
            Integer statusInt = "handled".equals(status) ? 1 : "ignored".equals(status) ? 2 : 0;
            alert.setStatus(statusInt);
            if (params.containsKey("action")) {
                alert.setHandleRemark((String) params.get("action") + (params.containsKey("notes") ? ": " + params.get("notes") : ""));
            }
            alert.setHandleTime(LocalDateTime.now());
            updateById(alert);
            Map<String, Object> data = new HashMap<>();
            data.put("id", alert.getId());
            data.put("status", status);
            data.put("updated_at", alert.getHandleTime() != null ? alert.getHandleTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
            return Result.success(data, "告警处理成功");
        } catch (Exception e) {
            log.error("Failed to handle alert: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

}
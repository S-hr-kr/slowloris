package com.slowloris.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slowloris.common.Result;
import com.slowloris.log.entity.Log;
import com.slowloris.log.mapper.LogMapper;
import com.slowloris.log.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {

    @Autowired
    private LogMapper logMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_LOG_RECENT = "log:recent:";
    private static final String REDIS_KEY_LOG_TODAY_ACCESS = "log:today:access";
    private static final String REDIS_KEY_LOG_TODAY_ERROR = "log:today:error";

    @Override
    public boolean saveLog(Log log) {
        // Set default values
        if (log.getType() == null) {
            log.setType(1); // 1-操作日志
        }
        if (log.getCreateTime() == null) {
            log.setCreateTime(LocalDateTime.now());
        }

        // Save to database
        boolean success = save(log);

        // Invalidate cache
        deleteKeysByPattern(REDIS_KEY_LOG_RECENT + "*");
        redisTemplate.delete(REDIS_KEY_LOG_TODAY_ACCESS);
        redisTemplate.delete(REDIS_KEY_LOG_TODAY_ERROR);

        return success;
    }

    @Override
    public List<Log> getRecentLogs(int limit) {
        // Check Redis cache first
        List<Log> logs = (List<Log>) redisTemplate.opsForValue().get(REDIS_KEY_LOG_RECENT + limit);
        if (logs != null) {
            return logs;
        }

        // Get from database
        logs = logMapper.selectRecentLogs(limit);

        // Save to Redis cache
        redisTemplate.opsForValue().set(
                REDIS_KEY_LOG_RECENT + limit,
                logs,
                5L,
                TimeUnit.MINUTES
        );

        return logs;
    }

    @Override
    public List<Map<String, Object>> countByType() {
        return logMapper.countByType();
    }

    @Override
    public List<Map<String, Object>> countByModule() {
        return logMapper.countByModule();
    }

    @Override
    public Integer getTodayAccessCount() {
        // Check Redis cache first
        Integer count = (Integer) redisTemplate.opsForValue().get(REDIS_KEY_LOG_TODAY_ACCESS);
        if (count != null) {
            return count;
        }

        // Get from database
        count = logMapper.countTodayAccess();

        // Save to Redis cache
        redisTemplate.opsForValue().set(
                REDIS_KEY_LOG_TODAY_ACCESS,
                count,
                5L,
                TimeUnit.MINUTES
        );

        return count;
    }

    @Override
    public Integer getTodayErrorCount() {
        // Check Redis cache first
        Integer count = (Integer) redisTemplate.opsForValue().get(REDIS_KEY_LOG_TODAY_ERROR);
        if (count != null) {
            return count;
        }

        // Get from database
        count = logMapper.countTodayErrors();

        // Save to Redis cache
        redisTemplate.opsForValue().set(
                REDIS_KEY_LOG_TODAY_ERROR,
                count,
                5L,
                TimeUnit.MINUTES
        );

        return count;
    }

    @Override
    public List<Log> searchLogs(Map<String, Object> params) {
        LambdaQueryWrapper<Log> queryWrapper = new LambdaQueryWrapper<>();
        
        // 根据日志级别过滤
        if (params.containsKey("level")) {
            String level = (String) params.get("level");
            Integer levelInt = "info".equals(level) ? 1 : "warning".equals(level) ? 2 : "error".equals(level) ? 3 : 1;
            queryWrapper.eq(Log::getType, levelInt);
        }
        
        // 根据模块过滤
        if (params.containsKey("module")) {
            queryWrapper.eq(Log::getModule, params.get("module"));
        }
        
        // 根据关键词搜索
        if (params.containsKey("keyword")) {
            String keyword = (String) params.get("keyword");
            queryWrapper.like(Log::getDescription, keyword)
                    .or().like(Log::getErrorInfo, keyword);
        }
        
        // 根据时间范围过滤
        if (params.containsKey("startTime")) {
            queryWrapper.ge(Log::getCreateTime, params.get("startTime"));
        }
        if (params.containsKey("endTime")) {
            queryWrapper.le(Log::getCreateTime, params.get("endTime"));
        }
        
        queryWrapper.orderByDesc(Log::getCreateTime);
        
        return list(queryWrapper);
    }

    @Override
    public boolean deleteLogs(List<Long> ids) {
        // Delete from database
        boolean success = removeByIds(ids);

        // Invalidate cache
        if (success) {
            deleteKeysByPattern(REDIS_KEY_LOG_RECENT + "*");
        }

        return success;
    }

    @Override
    public boolean cleanExpiredLogs(int days) {
        // Calculate cutoff date
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        // Delete expired logs
        boolean deleted = this.remove(
                lambdaQuery()
                        .lt(Log::getCreateTime, cutoffDate)
        );

        // Invalidate cache
        deleteKeysByPattern(REDIS_KEY_LOG_RECENT + "*");

        log.info("Cleaned {} expired logs older than {} days", deleted, days);
        return deleted ;
    }

    /**
     * Delete keys by pattern
     */
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

    @Override
    public Result<Map<String, Object>> getLogList(Integer page, Integer limit, String level, String module, String startTime, String endTime) {
        try {
            LambdaQueryWrapper<Log> queryWrapper = new LambdaQueryWrapper<>();
            if (level != null) {
                Integer levelInt = "info".equals(level) ? 1 : "warning".equals(level) ? 2 : "error".equals(level) ? 3 : 1;
                queryWrapper.eq(Log::getType, levelInt);
            }
            if (module != null) {
                queryWrapper.eq(Log::getModule, module);
            }
            queryWrapper.orderByDesc(Log::getCreateTime);

            Page<Log> pageResult = page(new Page<>(page, limit), queryWrapper);
            Map<String, Object> data = new HashMap<>();
            data.put("total", pageResult.getTotal());
            List<Map<String, Object>> list = new ArrayList<>();
            for (Log logItem : pageResult.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", logItem.getId());
                item.put("timestamp", logItem.getCreateTime() != null ? logItem.getCreateTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
                String levelStr = logItem.getType() == 1 ? "info" : logItem.getType() == 2 ? "warning" : "error";
                item.put("level", levelStr);
                item.put("module", logItem.getModule());
                item.put("message", logItem.getDescription());
                item.put("details", logItem.getErrorInfo());
                list.add(item);
            }
            data.put("list", list);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get log list: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> searchLogsWithPage(Map<String, Object> params) {
        try {
            String query = (String) params.get("query");
            Integer page = params.get("page") != null ? (Integer) params.get("page") : 1;
            Integer limit = params.get("limit") != null ? (Integer) params.get("limit") : 50;
            LambdaQueryWrapper<Log> queryWrapper = new LambdaQueryWrapper<>();
            if (query != null && !query.isEmpty()) {
                queryWrapper.like(Log::getDescription, query).or().like(Log::getErrorInfo, query);
            }
            queryWrapper.orderByDesc(Log::getCreateTime);
            Page<Log> pageResult = page(new Page<>(page, limit), queryWrapper);
            Map<String, Object> data = new HashMap<>();
            data.put("total", pageResult.getTotal());
            List<Map<String, Object>> list = new ArrayList<>();
            for (Log logItem : pageResult.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", logItem.getId());
                item.put("timestamp", logItem.getCreateTime() != null ? logItem.getCreateTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
                String levelStr = logItem.getType() == 1 ? "info" : logItem.getType() == 2 ? "warning" : "error";
                item.put("level", levelStr);
                item.put("module", logItem.getModule());
                item.put("message", logItem.getDescription());
                item.put("details", logItem.getErrorInfo());
                list.add(item);
            }
            data.put("list", list);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to search logs: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<String> exportLogs(String level, String module, String startTime, String endTime) {
        try {
            return Result.success("日志导出功能待实现");
        } catch (Exception e) {
            log.error("Failed to export logs: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

}
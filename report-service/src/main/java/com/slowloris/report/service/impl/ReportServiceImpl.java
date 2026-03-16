package com.slowloris.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slowloris.common.Result;
import com.slowloris.report.entity.Report;
import com.slowloris.report.mapper.ReportMapper;
import com.slowloris.report.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_REPORT_RECENT = "report:recent:";
    private static final String REDIS_KEY_REPORT_COUNT_TYPE = "report:count:type:";
    private static final String REDIS_KEY_REPORT_COUNT_TODAY = "report:count:today";

    @Override
    public Long generateReport(Report report) {
        // Set default values
        if (report.getType() == null) {
            report.setType(1); // 1-攻击检测报告
        }
        if (report.getName() == null) {
            report.setName("Report_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        }
        if (report.getGenerateTime() == null) {
            report.setGenerateTime(LocalDateTime.now());
        }
        if (report.getStatus() == null) {
            report.setStatus(1); // 1-已完成
        }

        // Save to database
        save(report);

        // Invalidate cache
        deleteKeysByPattern(REDIS_KEY_REPORT_RECENT + "*");
        deleteKeysByPattern(REDIS_KEY_REPORT_COUNT_TYPE + "*");
        redisTemplate.delete(REDIS_KEY_REPORT_COUNT_TODAY);

        return report.getId();
    }

    @Override
    public List<Report> getRecentReports(int limit) {
        // Check Redis cache first
        List<Report> reports = (List<Report>) redisTemplate.opsForValue().get(REDIS_KEY_REPORT_RECENT + limit);
        if (reports != null) {
            return reports;
        }

        // Get from database
        reports = reportMapper.selectRecentReports(limit);

        // Save to Redis cache
        redisTemplate.opsForValue().set(
                REDIS_KEY_REPORT_RECENT + limit,
                reports,
                5L,
                TimeUnit.MINUTES
        );

        return reports;
    }

    @Override
    public List<Map<String, Object>> countByType() {
        return reportMapper.countByType();
    }

    @Override
    public Integer getReportCountByType(Integer type) {
        // Check Redis cache first
        Integer count = (Integer) redisTemplate.opsForValue().get(REDIS_KEY_REPORT_COUNT_TYPE + type);
        if (count != null) {
            return count;
        }

        // Get from database
        count = reportMapper.countByReportType(type);

        // Save to Redis cache
        redisTemplate.opsForValue().set(
                REDIS_KEY_REPORT_COUNT_TYPE + type,
                count,
                30L,
                TimeUnit.MINUTES
        );

        return count;
    }

    @Override
    public Integer getTodayReportCount() {
        // Check Redis cache first
        Integer count = (Integer) redisTemplate.opsForValue().get(REDIS_KEY_REPORT_COUNT_TODAY);
        if (count != null) {
            return count;
        }

        // Get from database
        count = reportMapper.countTodayReports();

        // Save to Redis cache
        redisTemplate.opsForValue().set(
                REDIS_KEY_REPORT_COUNT_TODAY,
                count,
                5L,
                TimeUnit.MINUTES
        );

        return count;
    }

    @Override
    public List<Report> searchReports(Map<String, Object> params) {
        LambdaQueryWrapper<Report> queryWrapper = new LambdaQueryWrapper<>();
        
        // 根据报表类型过滤
        if (params.containsKey("type")) {
            queryWrapper.eq(Report::getType, params.get("type"));
        }
        
        // 根据报表状态过滤
        if (params.containsKey("status")) {
            queryWrapper.eq(Report::getStatus, params.get("status"));
        }
        
        // 根据报表名称搜索
        if (params.containsKey("name")) {
            queryWrapper.like(Report::getName, params.get("name"));
        }
        
        // 根据时间范围过滤
        if (params.containsKey("startTime")) {
            queryWrapper.ge(Report::getCreateTime, params.get("startTime"));
        }
        if (params.containsKey("endTime")) {
            queryWrapper.le(Report::getCreateTime, params.get("endTime"));
        }
        
        queryWrapper.orderByDesc(Report::getCreateTime);
        
        return list(queryWrapper);
    }

    @Override
    public boolean deleteReport(Long id) {
        // Delete from database
        boolean success = removeById(id);

        // Invalidate cache
        if (success) {
            redisTemplate.delete(REDIS_KEY_REPORT_RECENT + "*");
        }

        return success;
    }

    @Override
    public Report getReportDetail(Long id) {
        // Get from database
        return getById(id);
    }

    @Override
    public Result<Map<String, Object>> getReportList(Integer page, Integer limit, String type) {
        try {
            LambdaQueryWrapper<Report> queryWrapper = new LambdaQueryWrapper<>();
            if (type != null) {
                Integer typeInt = "daily".equals(type) ? 1 : "weekly".equals(type) ? 2 : 3;
                queryWrapper.eq(Report::getType, typeInt);
            }
            queryWrapper.orderByDesc(Report::getCreateTime);

            Page<Report> pageResult = page(new Page<>(page, limit), queryWrapper);
            Map<String, Object> data = new HashMap<>();
            data.put("total", pageResult.getTotal());
            List<Map<String, Object>> list = new ArrayList<>();
            for (Report report : pageResult.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", report.getId());
                item.put("title", report.getName());
                String typeStr = report.getType() == 1 ? "daily" : report.getType() == 2 ? "weekly" : "monthly";
                item.put("type", typeStr);
                String statusStr = report.getStatus() == 0 ? "generating" : report.getStatus() == 1 ? "generated" : "failed";
                item.put("status", statusStr);
                item.put("created_at", report.getCreateTime() != null ? report.getCreateTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
                item.put("url", "/reports/download/" + report.getId());
                list.add(item);
            }
            data.put("list", list);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get report list: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> generateReport(Map<String, Object> params) {
        try {
            Report report = new Report();
            String type = (String) params.get("type");
            report.setType("daily".equals(type) ? 1 : "weekly".equals(type) ? 2 : 3);
            report.setName((String) params.getOrDefault("name", "Report_" + System.currentTimeMillis()));
            report.setStatus(0);
            report.setGenerateTime(LocalDateTime.now());
            save(report);

            Map<String, Object> data = new HashMap<>();
            data.put("report_id", report.getId());
            data.put("status", "generating");
            data.put("estimated_time", 30);
            return Result.success(data, "报表生成已开始");
        } catch (Exception e) {
            log.error("Failed to generate report: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<String> downloadReport(Long id) {
        try {
            return Result.success("报表下载功能待实现");
        } catch (Exception e) {
            log.error("Failed to download report: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
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

}
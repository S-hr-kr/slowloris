package com.slowloris.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slowloris.common.Result;
import com.slowloris.monitor.entity.AttackDetection;
import com.slowloris.monitor.entity.IpMonitor;
import com.slowloris.monitor.mapper.AttackDetectionMapper;
import com.slowloris.monitor.mapper.IpMonitorMapper;
import com.slowloris.monitor.service.AttackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttackServiceImpl implements AttackService {

    private final AttackDetectionMapper attackDetectionMapper;
    private final IpMonitorMapper ipMonitorMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_ATTACKS_STATS = "attacks:stats";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getAttacks() {
        // 尝试从缓存获取
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(CACHE_ATTACKS_STATS);
        if (cached != null) return Result.success(cached);

        // 统计数据
        long totalAttacks = attackDetectionMapper.selectCount(null);
        long currentAttacks = attackDetectionMapper.selectCount(
                new LambdaQueryWrapper<AttackDetection>().eq(AttackDetection::getStatus, "active"));
        long blockedAttacks = attackDetectionMapper.selectCount(
                new LambdaQueryWrapper<AttackDetection>().eq(AttackDetection::getStatus, "blocked"));

        // 攻击类型统计
        List<AttackDetection> all = attackDetectionMapper.selectList(
                new LambdaQueryWrapper<AttackDetection>().select(AttackDetection::getType));
        long attackTypes = all.stream().map(AttackDetection::getType)
                .filter(Objects::nonNull).distinct().count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAttacks", totalAttacks);
        stats.put("currentAttacks", currentAttacks);
        stats.put("blockedAttacks", blockedAttacks);
        stats.put("attackTypes", attackTypes);

        // 攻击详情列表（最近100条）
        List<AttackDetection> records = attackDetectionMapper.selectList(
                new LambdaQueryWrapper<AttackDetection>()
                        .orderByDesc(AttackDetection::getDetectedAt)
                        .last("LIMIT 100"));

        List<Map<String, Object>> details = records.stream().map(a -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("type", a.getType() != null ? a.getType() : "Unknown");
            item.put("sourceIp", a.getIpAddress());
            item.put("target", "server:80");
            item.put("startTime", a.getDetectedAt() != null ? a.getDetectedAt().format(FMT) : null);
            item.put("status", a.getStatus() != null ? a.getStatus() : "active");
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stats", stats);
        data.put("details", details);

        redisTemplate.opsForValue().set(CACHE_ATTACKS_STATS, data, 30, TimeUnit.SECONDS);
        return Result.success(data);
    }

    @Override
    public Result<String> blockAttack(Long attackId) {
        AttackDetection attack = attackDetectionMapper.selectById(attackId);
        if (attack == null) return Result.error("攻击记录不存在");

        attack.setStatus("blocked");
        attack.setResolvedAt(LocalDateTime.now());
        attackDetectionMapper.updateById(attack);

        // 同步封锁对应 IP
        if (attack.getIpAddress() != null) {
            IpMonitor monitor = ipMonitorMapper.selectByIpAddress(attack.getIpAddress());
            if (monitor != null) {
                monitor.setStatus(2);
                ipMonitorMapper.updateById(monitor);
            }
            redisTemplate.opsForValue().set("ip:blocked:" + attack.getIpAddress(), true);
        }

        redisTemplate.delete(CACHE_ATTACKS_STATS);
        return Result.success("攻击已阻止");
    }
}

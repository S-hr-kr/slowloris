package com.slowloris.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slowloris.common.Result;
import com.slowloris.system.entity.SystemConfig;
import com.slowloris.system.mapper.SystemConfigMapper;
import com.slowloris.system.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_CONFIG = "system:config:";
    private static final String REDIS_KEY_CONFIG_ALL = "system:config:all";

    @Override
    public String getConfigValue(String configKey) {
        return getConfigValue(configKey, null);
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        // Check Redis cache first
        String key = REDIS_KEY_CONFIG + configKey;
        String value = (String) redisTemplate.opsForValue().get(key);
        if (value != null) {
            return value;
        }

        // Get from database
        value = systemConfigMapper.getConfigValueByKey(configKey);
        if (value == null) {
            return defaultValue;
        }

        // Save to Redis cache
        redisTemplate.opsForValue().set(
                key,
                value,
                24L,
                TimeUnit.HOURS
        );

        return value;
    }

    @Override
    public boolean updateConfigValue(String configKey, String configValue) {
        // Update database
        SystemConfig config = lambdaQuery()
                .eq(SystemConfig::getConfigKey, configKey)
                .one();
        if (config == null) {
            return false;
        }

        config.setConfigValue(configValue);
        boolean success = updateById(config);

        // Invalidate cache
        if (success) {
            redisTemplate.delete(REDIS_KEY_CONFIG + configKey);
            redisTemplate.delete(REDIS_KEY_CONFIG_ALL);
        }

        return success;
    }

    @Override
    public Map<String, String> getAllConfigs() {
        // Check Redis cache first
        Map<String, String> configs = (Map<String, String>) redisTemplate.opsForValue().get(REDIS_KEY_CONFIG_ALL);
        if (configs != null) {
            return configs;
        }

        // Get all configs from database
        List<SystemConfig> configList = lambdaQuery()
                .eq(SystemConfig::getStatus, 1)
                .list();

        // Convert to map
        configs = new HashMap<>();
        for (SystemConfig config : configList) {
            configs.put(config.getConfigKey(), config.getConfigValue());
        }

        // Save to Redis cache
        redisTemplate.opsForValue().set(
                REDIS_KEY_CONFIG_ALL,
                configs,
                24L,
                TimeUnit.HOURS
        );

        return configs;
    }

    @Override
    public void refreshConfigCache() {
        // Delete all config caches
        deleteKeysByPattern(REDIS_KEY_CONFIG + "*");
        redisTemplate.delete(REDIS_KEY_CONFIG_ALL);
        log.info("System config cache refreshed");
    }

    @Override
    public Result<Map<String, Object>> getSystemConfig() {
        try {
            Map<String, String> configs = getAllConfigs();
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> detection = new HashMap<>();
            detection.put("enabled", true);
            detection.put("interval", 60);
            Map<String, Object> thresholds = new HashMap<>();
            thresholds.put("request_rate", 100);
            thresholds.put("connection_time", 30);
            thresholds.put("packet_size", 40);
            detection.put("thresholds", thresholds);
            Map<String, Object> alert = new HashMap<>();
            alert.put("enabled", true);
            alert.put("email_notification", "true".equals(configs.getOrDefault("alert.email.enabled", "false")));
            alert.put("webhook", "");
            Map<String, Object> model = new HashMap<>();
            model.put("default_model", "xgboost");
            model.put("retrain_interval", 24);
            data.put("detection", detection);
            data.put("alert", alert);
            data.put("model", model);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get system config: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> updateSystemConfig(Map<String, Object> configData) {
        try {
            for (Map.Entry<String, Object> entry : configData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                updateConfigValue(key, value.toString());
            }
            return Result.success(configData, "配置更新成功");
        } catch (Exception e) {
            log.error("Failed to update system config: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    @Override
    public Result<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("uptime", 86400);
            data.put("cpu_usage", 30 + Math.random() * 40);
            data.put("memory_usage", 50 + Math.random() * 30);
            data.put("disk_usage", 40 + Math.random() * 30);
            data.put("connections", 50 + (int) (Math.random() * 100));
            data.put("detection_status", "running");
            data.put("last_update", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get system status: {}", e.getMessage());
            return Result.error("服务器内部错误");
        }
    }

    /**
     * Delete keys by pattern
     */
    private void deleteKeysByPattern(String pattern) {
        try {
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to delete keys with pattern: {}", pattern, e);
        }
    }

}
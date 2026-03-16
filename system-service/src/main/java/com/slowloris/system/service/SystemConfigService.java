package com.slowloris.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.slowloris.common.Result;
import com.slowloris.system.entity.SystemConfig;

import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 根据配置键获取配置值
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置键获取配置值，带默认值
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 更新配置值
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 是否成功
     */
    boolean updateConfigValue(String configKey, String configValue);

    /**
     * 获取所有配置
     * @return 配置映射
     */
    Map<String, String> getAllConfigs();

    /**
     * 刷新配置缓存
     */
    void refreshConfigCache();

    /**
     * 获取系统配置
     * @return 系统配置
     */
    Result<Map<String, Object>> getSystemConfig();

    /**
     * 更新系统配置
     * @param configData 配置数据
     * @return 更新结果
     */
    Result<Map<String, Object>> updateSystemConfig(Map<String, Object> configData);

    /**
     * 获取系统状态
     * @return 系统状态
     */
    Result<Map<String, Object>> getSystemStatus();

}
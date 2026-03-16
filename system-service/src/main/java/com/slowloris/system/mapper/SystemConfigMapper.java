package com.slowloris.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.system.entity.SystemConfig;

/**
 * 系统配置Mapper接口
 */
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键获取配置值
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValueByKey(String configKey);

}
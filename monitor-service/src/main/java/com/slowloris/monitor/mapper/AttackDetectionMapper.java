package com.slowloris.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.monitor.entity.AttackDetection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttackDetectionMapper extends BaseMapper<AttackDetection> {
}

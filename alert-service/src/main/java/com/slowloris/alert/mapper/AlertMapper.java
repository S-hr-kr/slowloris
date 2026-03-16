package com.slowloris.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.alert.entity.Alert;
import org.apache.ibatis.annotations.MapKey;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 告警Mapper接口
 */
public interface AlertMapper extends BaseMapper<Alert> {

    /**
     * 查询未处理告警数量
     * @return 未处理告警数量
     */
    Integer countUnprocessed();

    /**
     * 查询最近N条告警
     * @param limit 限制数量
     * @return 告警列表
     */
    List<Alert> selectRecentAlerts(int limit);

    /**
     * 按类型统计告警数量
     * @return 按类型统计结果
     */
    @MapKey("type")
    List<Map<String, Object>> countByType( );

    /**
     * 按级别统计告警数量
     * @return 按级别统计结果
     */
    @MapKey("level")
    List<Map<String, Object>> countByLevel();

    /**
     * 查询今日告警数量
     * @return 今日告警数量
     */
    Integer countTodayAlerts();

    /**
     * 按日期统计最近N天的告警数量
     * @param days 天数
     * @return 按日期统计结果
     */
    List<Map<String, Object>> countDailyAlerts(int days);

}
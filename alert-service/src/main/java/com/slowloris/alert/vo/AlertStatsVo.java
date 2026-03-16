package com.slowloris.alert.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 告警统计响应参数
 */
@Data
public class AlertStatsVo {

    /**
     * 总告警数量
     */
    private Long totalCount;

    /**
     * 未处理告警数量
     */
    private Integer unprocessedCount;

    /**
     * 已处理告警数量
     */
    private Integer processedCount;

    /**
     * 已忽略告警数量
     */
    private Integer ignoredCount;

    /**
     * 今日告警数量
     */
    private Integer todayCount;

    /**
     * 按类型统计
     */
    private List<Map<String, Object>> typeStats;

    /**
     * 按级别统计
     */
    private List<Map<String, Object>> levelStats;

    /**
     * 最近7天告警趋势
     */
    private List<Map<String, Object>> dailyTrend;

}
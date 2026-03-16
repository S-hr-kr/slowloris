package com.slowloris.monitor.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流量分析响应参数
 */
@Data
public class TrafficAnalysisVo {

    /**
     * 总流量（字节）
     */
    private Long totalTraffic;

    /**
     * 今日流量（字节）
     */
    private Long todayTraffic;

    /**
     * 昨日流量（字节）
     */
    private Long yesterdayTraffic;

    /**
     * 流量增长率（%）
     */
    private Double growthRate;

    /**
     * 按小时流量统计
     */
    private List<Map<String, Object>> hourlyTraffic;

    /**
     * 按IP段流量统计
     */
    private List<Map<String, Object>> ipSegmentTraffic;

    /**
     * 按状态统计IP数量
     */
    private Map<String, Integer> ipStatusCount;

}
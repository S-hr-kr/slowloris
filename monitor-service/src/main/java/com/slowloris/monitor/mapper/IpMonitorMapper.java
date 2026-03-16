package com.slowloris.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.monitor.entity.IpMonitor;
import java.util.List;
import java.util.Map;

/**
 * IP监控Mapper接口
 */
public interface IpMonitorMapper extends BaseMapper<IpMonitor> {

    /**
     * 根据IP地址查询监控记录
     * @param ipAddress IP地址
     * @return 监控记录
     */
    IpMonitor selectByIpAddress(String ipAddress);

    /**
     * 查询异常IP列表
     * @return 异常IP列表
     */
    List<IpMonitor> selectAbnormalIps();

    /**
     * 查询最近N条监控记录
     * @param limit 限制数量
     * @return 监控记录列表
     */
    List<IpMonitor> selectRecentRecords(int limit);

    /**
     * 按状态统计IP数量
     * @return 按状态统计结果
     */
    List<Map<String, Object>> countByStatus();

    /**
     * 获取今日总流量
     * @return 今日总流量
     */
    Long getTodayTraffic();

    /**
     * 获取昨日总流量
     * @return 昨日总流量
     */
    Long getYesterdayTraffic();

    /**
     * 按小时统计流量
     * @return 按小时统计结果
     */
    List<Map<String, Object>> countHourlyTraffic();

    /**
     * 按IP段统计流量
     * @return 按IP段统计结果
     */
    List<Map<String, Object>> countTrafficByIpSegment();

}
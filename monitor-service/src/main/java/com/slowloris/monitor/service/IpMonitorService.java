package com.slowloris.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.slowloris.common.Result;
import com.slowloris.monitor.entity.IpMonitor;
import com.slowloris.monitor.vo.TrafficAnalysisVo;

import java.util.List;
import java.util.Map;

/**
 * IP监控服务接口
 */
public interface IpMonitorService extends IService<IpMonitor> {

    /**
     * 根据IP地址获取监控信息
     * @param ipAddress IP地址
     * @return 监控信息
     */
    IpMonitor getByIpAddress(String ipAddress);

    /**
     * 更新IP访问记录
     * @param ipAddress IP地址
     * @param trafficSize 流量大小
     */
    void updateAccessRecord(String ipAddress, long trafficSize);

    /**
     * 获取异常IP列表
     * @return 异常IP列表
     */
    List<IpMonitor> getAbnormalIps();

    /**
     * 获取最近N条监控记录
     * @param limit 限制数量
     * @return 监控记录列表
     */
    List<IpMonitor> getRecentRecords(int limit);

    /**
     * 获取流量分析数据
     * @return 流量分析数据
     */
    TrafficAnalysisVo getTrafficAnalysis();

    /**
     * 根据状态获取IP列表
     * @param status 状态：0-正常，1-可疑，2-异常
     * @return IP列表
     */
    List<IpMonitor> getIpsByStatus(Integer status);

    /**
     * 手动设置IP状态
     * @param ipAddress IP地址
     * @param status 状态：0-正常，1-可疑，2-异常
     * @return 是否成功
     */
    boolean setIpStatus(String ipAddress, Integer status);

    /**
     * 获取IP列表
     * @param page 页码
     * @param limit 每页数量
     * @param status IP状态
     * @return IP列表
     */
    Result<Map<String, Object>> getIpList(Integer page, Integer limit, String status);

    /**
     * 获取IP详情
     * @param ip IP地址
     * @return IP详情
     */
    Result<Map<String, Object>> getIpDetail(String ip);

    /**
     * 添加IP监控
     * @param params 请求参数
     * @return 添加结果
     */
    Result<Map<String, Object>> addIpMonitor(Map<String, Object> params);

    /**
     * 获取流量统计
     * @param timeRange 时间范围
     * @param ip 过滤IP
     * @return 流量统计
     */
    Result<Map<String, Object>> getTrafficStats(String timeRange, String ip);

    /**
     * 获取流量图表数据
     * @param ip 目标IP地址
     * @param timeRange 时间范围
     * @return 图表数据
     */
    Result<Map<String, Object>> getChart(String ip, String timeRange);

    /**
     * 获取检测结果
     * @param page 页码
     * @param limit 每页数量
     * @param type 攻击类型
     * @param status 状态
     * @return 检测结果
     */
    Result<Map<String, Object>> getAttackDetectionResults(Integer page, Integer limit, String type, String status);

    /**
     * 获取攻击详情
     * @param id 攻击ID
     * @return 攻击详情
     */
    Result<Map<String, Object>> getAttackDetectionDetail(Long id);

    /**
     * 手动触发检测
     * @param params 请求参数
     * @return 检测结果
     */
    Result<Map<String, Object>> triggerAttackDetectionScan(Map<String, Object> params);

    /**
     * 执行攻击预测
     * @param params 请求参数
     * @return 预测结果
     */
    Result<Map<String, Object>> predictAttack(Map<String, Object> params);

    /**
     * 获取预测历史记录
     * @param page 页码
     * @param limit 每页数量
     * @param ip 过滤IP
     * @param prediction 预测结果
     * @return 历史记录
     */
    Result<List<Map<String, Object>>> getPredictionHistory(Integer page, Integer limit, String ip, String prediction);

}
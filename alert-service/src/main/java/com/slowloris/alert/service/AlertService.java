package com.slowloris.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.slowloris.alert.entity.Alert;
import com.slowloris.alert.vo.AlertStatsVo;
import com.slowloris.common.Result;

import java.util.List;
import java.util.Map;

/**
 * 告警服务接口
 */
public interface AlertService extends IService<Alert> {

    /**
     * 创建告警
     * @param alert 告警信息
     * @return 告警ID
     */
    Long createAlert(Alert alert);

    /**
     * 获取未处理告警数量
     * @return 未处理告警数量
     */
    Integer getUnprocessedCount();

    /**
     * 获取最近N条告警
     * @param limit 限制数量
     * @return 告警列表
     */
    List<Alert> getRecentAlerts(int limit);

    /**
     * 获取告警统计信息
     * @return 告警统计信息
     */
    AlertStatsVo getAlertStats();

    /**
     * 更新告警状态
     * @param id 告警ID
     * @param status 状态：0-未处理，1-已处理，2-已忽略
     * @param handler 处理人
     * @param remark 处理备注
     * @return 是否成功
     */
    boolean updateAlertStatus(Long id, Integer status, String handler, String remark);

    /**
     * 根据条件查询告警列表
     * @param params 查询条件
     * @return 告警列表
     */
    List<Alert> searchAlerts(Map<String, Object> params);

    /**
     * 按类型统计告警数量
     * @return 类型统计结果
     */
    List<Map<String, Object>> countByType();

    /**
     * 按级别统计告警数量
     * @return 级别统计结果
     */
    List<Map<String, Object>> countByLevel();

    /**
     * 获取告警列表
     * @param page 页码
     * @param limit 每页数量
     * @param severity 严重程度
     * @param status 状态
     * @return 告警列表
     */
    Result<Map<String, Object>> getAlertList(Integer page, Integer limit, String severity, String status);

    /**
     * 获取告警详情
     * @param id 告警ID
     * @return 告警详情
     */
    Result<Map<String, Object>> getAlertDetail(Long id);

    /**
     * 处理告警
     * @param id 告警ID
     * @param params 请求参数
     * @return 处理结果
     */
    Result<Map<String, Object>> handleAlert(Long id, Map<String, Object> params);

}
package com.slowloris.prediction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.slowloris.common.Result;
import com.slowloris.prediction.entity.PredictionHistory;

import java.util.List;
import java.util.Map;

/**
 * 预测历史服务接口
 */
public interface PredictionHistoryService extends IService<PredictionHistory> {

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

}

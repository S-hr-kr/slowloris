package com.slowloris.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.slowloris.common.Result;
import com.slowloris.log.entity.Log;

import java.util.List;
import java.util.Map;

/**
 * 日志服务接口
 */
public interface LogService extends IService<Log> {

    /**
     * 保存日志
     * @param log 日志信息
     * @return 保存结果
     */
    boolean saveLog(Log log);

    /**
     * 获取最近N条日志
     * @param limit 限制数量
     * @return 日志列表
     */
    List<Log> getRecentLogs(int limit);

    /**
     * 按类型统计日志数量
     * @return 类型统计结果
     */
    List<Map<String, Object>> countByType();

    /**
     * 按模块统计日志数量
     * @return 模块统计结果
     */
    List<Map<String, Object>> countByModule();

    /**
     * 获取今日访问量
     * @return 今日访问量
     */
    Integer getTodayAccessCount();

    /**
     * 获取今日错误数
     * @return 今日错误数
     */
    Integer getTodayErrorCount();

    /**
     * 根据条件查询日志
     * @param params 查询条件
     * @return 日志列表
     */
    List<Log> searchLogs(Map<String, Object> params);

    /**
     * 批量删除日志
     * @param ids 日志ID列表
     * @return 删除结果
     */
    boolean deleteLogs(List<Long> ids);

    /**
     * 清理过期日志
     * @param days 保留天数
     * @return 清理结果
     */
    boolean cleanExpiredLogs(int days);

    /**
     * 获取日志列表
     * @param page 页码
     * @param limit 每页数量
     * @param level 日志级别
     * @param module 模块名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    Result<Map<String, Object>> getLogList(Integer page, Integer limit, String level, String module, String startTime, String endTime);

    /**
     * 搜索日志
     * @param params 请求参数
     * @return 搜索结果
     */
    Result<Map<String, Object>> searchLogsWithPage(Map<String, Object> params);

    /**
     * 导出日志
     * @param level 日志级别
     * @param module 模块名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 导出结果
     */
    Result<String> exportLogs(String level, String module, String startTime, String endTime);

}
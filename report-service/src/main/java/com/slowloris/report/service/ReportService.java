package com.slowloris.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.slowloris.common.Result;
import com.slowloris.report.entity.Report;

import java.util.List;
import java.util.Map;

/**
 * 报告服务接口
 */
public interface ReportService extends IService<Report> {

    /**
     * 生成报告
     * @param report 报告信息
     * @return 报告ID
     */
    Long generateReport(Report report);

    /**
     * 获取最近N条报告
     * @param limit 限制数量
     * @return 报告列表
     */
    List<Report> getRecentReports(int limit);

    /**
     * 按类型统计报告数量
     * @return 类型统计结果
     */
    List<Map<String, Object>> countByType();

    /**
     * 获取指定类型的报告数量
     * @param type 报告类型
     * @return 报告数量
     */
    Integer getReportCountByType(Integer type);

    /**
     * 获取今日生成的报告数量
     * @return 今日报告数量
     */
    Integer getTodayReportCount();

    /**
     * 根据条件查询报告
     * @param params 查询条件
     * @return 报告列表
     */
    List<Report> searchReports(Map<String, Object> params);

    /**
     * 删除报告
     * @param id 报告ID
     * @return 删除结果
     */
    boolean deleteReport(Long id);

    /**
     * 获取报告详情
     * @param id 报告ID
     * @return 报告详情
     */
    Report getReportDetail(Long id);

    /**
     * 获取报表列表
     * @param page 页码
     * @param limit 每页数量
     * @param type 报表类型
     * @return 报表列表
     */
    Result<Map<String, Object>> getReportList(Integer page, Integer limit, String type);

    /**
     * 生成报表
     * @param params 请求参数
     * @return 生成结果
     */
    Result<Map<String, Object>> generateReport(Map<String, Object> params);

    /**
     * 下载报表
     * @param id 报表ID
     * @return 报表文件
     */
    Result<String> downloadReport(Long id);

}
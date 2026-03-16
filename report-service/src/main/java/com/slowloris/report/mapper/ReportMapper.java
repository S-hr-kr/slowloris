package com.slowloris.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.report.entity.Report;
import org.apache.ibatis.annotations.MapKey;

import java.util.List;
import java.util.Map;

/**
 * 报告Mapper接口
 */
public interface ReportMapper extends BaseMapper<Report> {

    /**
     * 查询最近N条报告
     * @param limit 限制数量
     * @return 报告列表
     */
    List<Report> selectRecentReports(int limit);

    /**
     * 按类型统计报告数量
     * @return 类型统计结果
     */
    @MapKey("type")
    List<Map<String, Object>> countByType();

    /**
     * 查询指定类型的报告数量
     * @param type 报告类型
     * @return 报告数量
     */
    Integer countByReportType(Integer type);

    /**
     * 查询今日生成的报告数量
     * @return 今日报告数量
     */
    Integer countTodayReports();

}
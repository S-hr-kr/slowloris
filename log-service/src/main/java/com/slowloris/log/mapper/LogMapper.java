package com.slowloris.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.log.entity.Log;
import org.apache.ibatis.annotations.MapKey;
import java.util.List;
import java.util.Map;

/**
 * 日志Mapper接口
 */
public interface LogMapper extends BaseMapper<Log> {

    /**
     * 查询最近N条日志
     * @param limit 限制数量
     * @return 日志列表
     */
    List<Log> selectRecentLogs(int limit);

    /**
     * 按类型统计日志数量
     * @return 类型统计结果
     */
    @MapKey("type")
    List<Map<String, Object>> countByType();

    /**
     * 按模块统计日志数量
     * @return 模块统计结果
     */
    List<Map<String, Object>> countByModule();

    /**
     * 查询今日访问量
     * @return 今日访问量
     */
    Integer countTodayAccess();

    /**
     * 查询今日错误数
     * @return 今日错误数
     */
    Integer countTodayErrors();

}
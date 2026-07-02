package com.slowloris.alertlog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.alertlog.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {

    @Select("SELECT * FROM sys_log ORDER BY create_time DESC LIMIT #{limit}")
    List<SysLog> selectRecentLogs(@Param("limit") int limit);
}

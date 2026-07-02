package com.slowloris.alertlog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.alertlog.entity.Alert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlertMapper extends BaseMapper<Alert> {

    @Select("SELECT COUNT(*) FROM alert WHERE status = 0")
    Integer countUnprocessed();

    /** 某用户未处理告警数；admin=true 时统计全部。 */
    @Select("SELECT COUNT(*) FROM alert WHERE status = 0 AND (#{admin} = true OR user_id <=> #{userId})")
    Integer countUnprocessedScoped(@Param("userId") String userId, @Param("admin") boolean admin);

    @Select("SELECT * FROM alert ORDER BY create_time DESC LIMIT #{limit}")
    List<Alert> selectRecentAlerts(@Param("limit") int limit);
}

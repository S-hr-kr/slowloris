package com.slowloris.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.monitor.entity.AttackDetection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AttackDetectionMapper extends BaseMapper<AttackDetection> {

    @Select("SELECT * FROM attack_detection ORDER BY detected_at DESC LIMIT #{limit}")
    List<AttackDetection> findRecent(@Param("limit") int limit);

    /** 按归属用户筛选的近期决策记录；admin=true 时返回全部。 */
    @Select("SELECT * FROM attack_detection WHERE (#{admin} = true OR user_id <=> #{userId}) "
            + "ORDER BY detected_at DESC LIMIT #{limit}")
    List<AttackDetection> findRecentScoped(@Param("userId") String userId,
                                           @Param("admin") boolean admin,
                                           @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM attack_detection WHERE status = 'active'")
    int countActive();

    /** 某用户处置中的攻击数；admin=true 时统计全部。 */
    @Select("SELECT COUNT(*) FROM attack_detection WHERE status = 'active' "
            + "AND (#{admin} = true OR user_id <=> #{userId})")
    int countActiveScoped(@Param("userId") String userId, @Param("admin") boolean admin);
}

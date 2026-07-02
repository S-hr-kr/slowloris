package com.slowloris.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.monitor.entity.MetricsSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MetricsSnapshotMapper extends BaseMapper<MetricsSnapshot> {

    @Select("SELECT * FROM metrics_snapshot WHERE ip_address = #{ip} ORDER BY create_time DESC LIMIT 1")
    MetricsSnapshot findLatestByIp(@Param("ip") String ip);

    /** 某用户对某 IP 的最新快照；admin=true 时不限归属。 */
    @Select("SELECT * FROM metrics_snapshot WHERE ip_address = #{ip} "
            + "AND (#{admin} = true OR user_id <=> #{userId}) ORDER BY create_time DESC LIMIT 1")
    MetricsSnapshot findLatestByIpScoped(@Param("ip") String ip,
                                         @Param("userId") String userId,
                                         @Param("admin") boolean admin);

    @Select("SELECT * FROM metrics_snapshot WHERE ip_address = #{ip} AND create_time >= NOW() - INTERVAL 24 HOUR ORDER BY create_time ASC")
    List<MetricsSnapshot> findLast24h(@Param("ip") String ip);

    /** 某用户对某 IP 近 24h 快照（用于预测）；admin=true 时不限归属。 */
    @Select("SELECT * FROM metrics_snapshot WHERE ip_address = #{ip} "
            + "AND (#{admin} = true OR user_id <=> #{userId}) "
            + "AND create_time >= NOW() - INTERVAL 24 HOUR ORDER BY create_time ASC")
    List<MetricsSnapshot> findLast24hScoped(@Param("ip") String ip,
                                            @Param("userId") String userId,
                                            @Param("admin") boolean admin);

    @Select("SELECT * FROM metrics_snapshot WHERE is_attack = 1 ORDER BY create_time DESC LIMIT #{limit}")
    List<MetricsSnapshot> findRecentAttacks(@Param("limit") int limit);
}

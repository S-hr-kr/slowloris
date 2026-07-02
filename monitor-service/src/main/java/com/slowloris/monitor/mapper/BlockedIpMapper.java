package com.slowloris.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.monitor.entity.BlockedIp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BlockedIpMapper extends BaseMapper<BlockedIp> {

    @Select("SELECT * FROM blocked_ip WHERE ip_address = #{ip} AND status = 1 LIMIT 1")
    BlockedIp findActiveByIp(@Param("ip") String ip);

    /** 某用户范围内某 IP 的封禁中记录（多用户隔离：同一来源 IP 各用户各自封禁）。 */
    @Select("SELECT * FROM blocked_ip WHERE ip_address = #{ip} AND status = 1 "
            + "AND user_id <=> #{userId} LIMIT 1")
    BlockedIp findActiveByIpAndUser(@Param("ip") String ip, @Param("userId") String userId);

    @Select("SELECT * FROM blocked_ip WHERE status = 1 ORDER BY block_time DESC")
    List<BlockedIp> findAllActive();

    /** 某用户的封禁中列表；admin=true 时返回全部。 */
    @Select("SELECT * FROM blocked_ip WHERE status = 1 AND (#{admin} = true OR user_id <=> #{userId}) "
            + "ORDER BY block_time DESC")
    List<BlockedIp> findActiveScoped(@Param("userId") String userId, @Param("admin") boolean admin);

    @Select("SELECT COUNT(*) FROM blocked_ip WHERE status = 1")
    int countActive();

    /** 某用户封禁中数量；admin=true 时统计全部。 */
    @Select("SELECT COUNT(*) FROM blocked_ip WHERE status = 1 AND (#{admin} = true OR user_id <=> #{userId})")
    int countActiveScoped(@Param("userId") String userId, @Param("admin") boolean admin);
}

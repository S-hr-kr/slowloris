package com.slowloris.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.monitor.entity.MonitoredTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MonitoredTargetMapper extends BaseMapper<MonitoredTarget> {

    /** 按 IP 查任意归属的目标（探针上报兜底/兼容旧逻辑）。同一 IP 多用户时取最近一条。 */
    @Select("SELECT * FROM monitored_target WHERE ip_address = #{ip} ORDER BY id DESC LIMIT 1")
    MonitoredTarget findByIp(@Param("ip") String ip);

    /** 按 IP + 探针 token 精确定位目标（探针上报鉴权；token 全局唯一可反查归属用户）。 */
    @Select("SELECT * FROM monitored_target WHERE ip_address = #{ip} AND agent_token = #{token} LIMIT 1")
    MonitoredTarget findByIpAndToken(@Param("ip") String ip, @Param("token") String token);

    /** 按 IP + 归属用户精确定位目标（多用户隔离：每个用户对同一 IP 各有一条）。 */
    @Select("SELECT * FROM monitored_target WHERE ip_address = #{ip} AND user_id <=> #{userId} LIMIT 1")
    MonitoredTarget findByIpAndUser(@Param("ip") String ip, @Param("userId") String userId);

    /** 某用户的全部活跃监控目标。 */
    @Select("SELECT * FROM monitored_target WHERE status = 1 AND user_id <=> #{userId}")
    List<MonitoredTarget> findActiveByUser(@Param("userId") String userId);

    /** 全部活跃监控目标（管理员视角 / 服务重启后恢复调度）。 */
    @Select("SELECT * FROM monitored_target WHERE status = 1")
    List<MonitoredTarget> findAllActive();

    /** 停止某用户对某 IP 的监控（仅影响该用户自己的目标行）。 */
    @Update("UPDATE monitored_target SET status = 0 WHERE ip_address = #{ip} AND user_id <=> #{userId}")
    void deactivate(@Param("ip") String ip, @Param("userId") String userId);
}

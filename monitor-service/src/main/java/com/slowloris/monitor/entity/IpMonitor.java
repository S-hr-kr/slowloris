package com.slowloris.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ip_monitor")
public class IpMonitor implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 访问次数
     */
    private Integer accessCount;

    /**
     * 最近访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 流量大小（字节）
     */
    private Long trafficSize;

    /**
     * 状态：0-正常，1-可疑，2-异常
     */
    private Integer status;

    /**
     * 国家
     */
    private String country;

    /**
     * 城市
     */
    private String city;

    /**
     * 运营商
     */
    private String isp;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
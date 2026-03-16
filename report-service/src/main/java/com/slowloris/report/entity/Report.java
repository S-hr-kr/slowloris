package com.slowloris.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 报告类型：1-攻击检测报告，2-流量分析报告，3-系统状态报告
     */
    private Integer type;

    /**
     * 报告名称
     */
    private String name;

    /**
     * 报告描述
     */
    private String description;

    /**
     * 报告内容（JSON格式）
     */
    private String content;

    /**
     * 生成人
     */
    private String generator;

    /**
     * 生成时间
     */
    private LocalDateTime generateTime;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态：0-生成中，1-已完成，2-生成失败
     */
    private Integer status;

    /**
     * 文件路径
     */
    private String filePath;

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
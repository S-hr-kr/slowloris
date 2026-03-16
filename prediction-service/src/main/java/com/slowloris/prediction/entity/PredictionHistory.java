package com.slowloris.prediction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("prediction_history")
public class PredictionHistory implements Serializable {

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
     * 预测结果：攻击、正常
     */
    private String prediction;

    /**
     * 置信度
     */
    private Double confidence;

    /**
     * 风险分数
     */
    private Double riskScore;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 特征重要性（JSON格式）
     */
    private String featureImportance;

    /**
     * 预测详情（JSON格式）
     */
    private String details;

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

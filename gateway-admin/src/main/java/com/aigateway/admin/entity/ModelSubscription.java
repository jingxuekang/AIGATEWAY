package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型订阅关系
 */
@Data
@TableName("model_subscription")
public class ModelSubscription {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long modelId;

    private Long userId;

    private String tenantId;

    private String appId;

    /**
     * 订阅状态：1=订阅，0=取消
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}


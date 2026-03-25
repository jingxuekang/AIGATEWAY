package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Usage 日志数据库记录
 */
@Data
@TableName("usage_log")
public class UsageLogRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime timestamp;

    private String traceId;

    private String requestId;

    private String tenantId;

    private String appId;

    private String userId;

    @TableField(exist = false)
    private String username;          // 展示用登录用户名（查询时填充）

    private String model;

    private String provider;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer cacheCreationTokens;

    private Integer cacheReadTokens;

    private String status;

    private Long latencyMs;

    private Long ttftMs;

    private Long tpotMs;

    private String errorCode;

    private String errorMessage;

    @TableField(exist = false)
    private String requestContent;    // 请求内容摘要（前 500 字符）

    @TableField(exist = false)
    private String responseContent;   // 响应内容摘要（前 500 字符）
}


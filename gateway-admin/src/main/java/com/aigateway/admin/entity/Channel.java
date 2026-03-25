package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("channel")
public class Channel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 渠道名称 */
    private String name;

    /** Provider 标识（如 openai/deepseek/volcano/azure） */
    private String provider;

    /** API Base URL */
    private String baseUrl;

    /** 官方 API Key */
    private String apiKey;

    /** 支持的模型列表，逗号分隔 */
    private String models;

    /** 负载均衡权重，默认 100 */
    private Integer weight;

    /** 最大并发数，默认 100 */
    private Integer maxConcurrency;

    /** 超时时间(ms)，默认 30000 */
    private Integer timeout;

    /** 已用 token 数 */
    private Long usedQuota;

    /** 总配额(0=不限) */
    private Long totalQuota;

    /** 状态: 1=启用 0=禁用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

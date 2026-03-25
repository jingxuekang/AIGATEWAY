package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("provider")
public class Provider {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 唯一标识，如 openai / deepseek / volcano */
    private String code;

    /** 展示名称 */
    private String name;

    /** 官方 API Base URL */
    private String baseUrl;

    /** API Key */
    private String apiKey;

    /** 1=启用 0=禁用 */
    private Integer status;

    private String description;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}


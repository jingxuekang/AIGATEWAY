package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统设置实体（key-value）
 */
@Data
@TableName("system_setting")
public class SystemSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configKey;

    private String configValue;
}


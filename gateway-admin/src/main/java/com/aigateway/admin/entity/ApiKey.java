package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * API Key 实体
 */
@Data
@TableName("api_key")
public class ApiKey {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String keyValue;
    
    private String keyName;
    
    private Long userId;
    
    private String tenantId;
    
    private String appId;
    
    private Integer status;
    
    private LocalDateTime expireTime;
    
    private String allowedModels;
    
    private Long totalQuota;
    
    private Long usedQuota;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;

    /** 用户名（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String username;
}

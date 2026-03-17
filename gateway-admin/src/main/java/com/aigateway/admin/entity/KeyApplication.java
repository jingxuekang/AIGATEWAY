package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * API Key 申请实体
 */
@Data
@TableName("key_application")
public class KeyApplication {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String tenantId;
    
    private String appId;
    
    private String keyName;
    
    private String allowedModels;
    
    private String reason;
    
    private Integer approvalStatus;
    
    private Long approverId;
    
    private String approvalComment;
    
    private LocalDateTime approvalTime;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

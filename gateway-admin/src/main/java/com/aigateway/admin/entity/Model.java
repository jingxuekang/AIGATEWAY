package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型实体
 */
@Data
@TableName("model")
public class Model {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String modelName;
    
    private String modelVersion;
    
    private String provider;
    
    private String description;
    
    private Integer status;
    
    private BigDecimal inputPrice;
    
    private BigDecimal outputPrice;
    
    private Integer maxTokens;
    
    private Boolean supportStream;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}

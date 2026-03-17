package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("channel")
public class Channel {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String provider;
    
    private String baseUrl;
    
    private String apiKey;
    
    private Integer status;
    
    private Integer weight;
    
    private Integer maxConcurrency;
    
    private Integer timeout;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
}

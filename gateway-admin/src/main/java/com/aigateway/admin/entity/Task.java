package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String taskName;
    
    private String taskType;
    
    private String status;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Long duration;
    
    private String result;
    
    private LocalDateTime createTime;
}

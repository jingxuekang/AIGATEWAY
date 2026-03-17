package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("topup_record")
public class TopUpRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private BigDecimal amount;
    
    private Long quota;
    
    private String paymentMethod;
    
    private String status;
    
    private String transactionId;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

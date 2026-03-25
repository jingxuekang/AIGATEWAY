package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("top_up_record")
public class TopUpRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private BigDecimal amount;

    private String currency;
    
    @TableField(exist = false)
    private Long quota;
    
    @TableField(exist = false)
    private String paymentMethod;
    
    private String status;
    
    @TableField(exist = false)
    private String transactionId;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

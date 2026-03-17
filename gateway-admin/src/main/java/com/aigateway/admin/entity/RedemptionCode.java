package com.aigateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 兑换码实体
 */
@Data
@TableName("redemption_code")
public class RedemptionCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private BigDecimal amount;

    private String status;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}


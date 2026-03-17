package com.aigateway.admin.controller;

import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "TopUp Management", description = "充值管理")
@RestController
@RequestMapping("/api/admin/topup")
@RequiredArgsConstructor
public class TopUpController {
    
    @Operation(summary = "获取充值记录")
    @GetMapping
    public Result<List<Map<String, Object>>> listTopUpRecords() {
        List<Map<String, Object>> records = new ArrayList<>();
        
        String[] methods = {"alipay", "wechat", "balance"};
        String[] statuses = {"success", "pending", "failed"};
        
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", i);
            record.put("userId", i);
            record.put("username", "user" + i);
            record.put("amount", new BigDecimal(i * 100));
            record.put("quota", i * 100000L);
            record.put("paymentMethod", methods[i % 3]);
            record.put("status", statuses[i % 3]);
            record.put("createTime", LocalDateTime.now().minusDays(i));
            records.add(record);
        }
        
        return Result.success(records);
    }
}

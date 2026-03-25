package com.aigateway.admin.controller;

import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 充值记录 - 仅 admin
 */
@Tag(name = "TopUp Management", description = "充值管理")
@RestController
@RequestMapping("/api/admin/topup")
@RequiredArgsConstructor
public class TopUpController {

    private static void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (role == null || !"admin".equals(role.toString())) {
            throw new BusinessException(403, "Forbidden: admin role required");
        }
    }

    @Operation(summary = "获取充值记录（仅 admin）")
    @GetMapping
    public Result<List<Map<String, Object>>> listTopUpRecords(HttpServletRequest request) {
        requireAdmin(request);
        List<Map<String, Object>> records = new ArrayList<>();

        String[] methods  = {"alipay", "wechat", "balance"};
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

package com.aigateway.admin.controller;

import com.aigateway.admin.entity.RedemptionCode;
import com.aigateway.admin.service.RedemptionCodeService;
import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 兑换码 - 仅 admin
 */
@Tag(name = "Redemption Code", description = "兑换码管理")
@RestController
@RequestMapping("/api/admin/redemptions")
@RequiredArgsConstructor
public class RedemptionController {

    private final RedemptionCodeService redemptionCodeService;

    private static void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (role == null || !"admin".equals(role.toString())) {
            throw new BusinessException(403, "Forbidden: admin role required");
        }
    }

    @Operation(summary = "获取兑换码列表（仅 admin）")
    @GetMapping
    public Result<List<RedemptionCode>> listRedemptions(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(redemptionCodeService.list());
    }
}


package com.aigateway.admin.controller;

import com.aigateway.admin.service.DashboardService;
import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Dashboard控制器 - 仅 admin 可访问
 */
@Tag(name = "Dashboard", description = "仪表盘统计")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;

    private static void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (role == null || !"admin".equals(role.toString())) {
            throw new BusinessException(403, "Forbidden: admin role required");
        }
    }
    
    @Operation(summary = "获取统计数据（admin 查全局，普通用户查自己）")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(HttpServletRequest request) {
        Object roleAttr = request.getAttribute("role");
        Object userIdAttr = request.getAttribute("userId");
        boolean isAdmin = roleAttr != null && "admin".equals(roleAttr.toString());
        String userId = (!isAdmin && userIdAttr != null) ? userIdAttr.toString() : null;
        Map<String, Object> stats = dashboardService.getStatistics(userId);
        return Result.success(stats);
    }
}

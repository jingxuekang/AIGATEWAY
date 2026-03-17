package com.aigateway.admin.controller;

import com.aigateway.admin.service.DashboardService;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Dashboard控制器
 */
@Tag(name = "Dashboard", description = "仪表盘统计")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @Operation(summary = "获取统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = dashboardService.getStatistics();
        return Result.success(stats);
    }
}

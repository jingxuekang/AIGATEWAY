package com.aigateway.admin.controller;

import com.aigateway.admin.entity.ApiKey;
import com.aigateway.admin.entity.KeyApplication;
import com.aigateway.admin.service.KeyApplicationService;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Key Application", description = "API Key 申请与审批")
@RestController
@RequestMapping("/api/admin/key-applications")
@RequiredArgsConstructor
public class KeyApplicationController {

    private final KeyApplicationService keyApplicationService;

    @Operation(summary = "提交申请")
    @PostMapping
    public Result<KeyApplication> submit(@RequestBody KeyApplication application,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) application.setUserId(userId);
        return Result.success(keyApplicationService.submitApplication(application));
    }

    @Operation(summary = "查询全部申请（管理员）")
    @GetMapping
    public Result<List<KeyApplication>> list(
            @RequestParam(required = false, defaultValue = "false") boolean pendingOnly) {
        if (pendingOnly) {
            return Result.success(keyApplicationService.listPending());
        }
        return Result.success(keyApplicationService.list());
    }

    @Operation(summary = "查询我的申请")
    @GetMapping("/mine")
    public Result<List<KeyApplication>> mine(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(keyApplicationService.listByUser(userId));
    }

    @Operation(summary = "审批通过 - 自动生成 Key")
    @PutMapping("/{id}/approve")
    public Result<ApiKey> approve(@PathVariable Long id,
                                   @RequestBody Map<String, String> body,
                                   HttpServletRequest request) {
        Long approverId = (Long) request.getAttribute("userId");
        String comment = body.getOrDefault("comment", "");
        ApiKey key = keyApplicationService.approve(id, approverId, comment);
        return Result.success(key);
    }

    @Operation(summary = "审批拒绝")
    @PutMapping("/{id}/reject")
    public Result<Boolean> reject(@PathVariable Long id,
                                   @RequestBody Map<String, String> body,
                                   HttpServletRequest request) {
        Long approverId = (Long) request.getAttribute("userId");
        String comment = body.getOrDefault("comment", "");
        keyApplicationService.reject(id, approverId, comment);
        return Result.success(true);
    }
}

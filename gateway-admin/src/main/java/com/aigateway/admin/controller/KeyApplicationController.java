package com.aigateway.admin.controller;

import com.aigateway.admin.dto.KeyApplicationRequest;
import com.aigateway.admin.entity.ApiKey;
import com.aigateway.admin.entity.KeyApplication;
import com.aigateway.admin.entity.User;
import com.aigateway.admin.service.KeyApplicationService;
import com.aigateway.admin.service.UserService;
import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API Key 申请审批
 *
 * 权限矩阵：
 *   POST   /                    - 所有已登录用户（提交申请）
 *   GET    /                    - 仅 admin（查看所有申请）
 *   GET    /my 或 /mine         - 所有已登录用户（只看自己）
 *   PUT    /{id}/approve        - 仅 admin（审批通过，自动生成 Key）
 *   PUT    /{id}/reject         - 仅 admin（审批拒绝）
 */
@Tag(name = "Key Application", description = "API Key 申请与审批")
@RestController
@RequestMapping("/api/admin/key-applications")
@RequiredArgsConstructor
public class KeyApplicationController {

    private final KeyApplicationService keyApplicationService;
    private final UserService userService;

    /** 批量填充申请列表中的申请人用户名 */
    private void fillUsernames(List<KeyApplication> list) {
        if (list == null || list.isEmpty()) return;
        List<Long> userIds = list.stream()
                .map(KeyApplication::getUserId)
                .filter(id -> id != null)
                .distinct().toList();
        if (userIds.isEmpty()) return;
        Map<Long, String> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        list.forEach(a -> a.setUsername(userMap.getOrDefault(a.getUserId(), String.valueOf(a.getUserId()))));
    }

    // ==================== 工具方法 ====================

    private static String getRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null ? role.toString() : "user";
    }

    private static Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "Not authenticated");
        return (Long) userId;
    }

    private static void requireAdmin(HttpServletRequest request) {
        if (!"admin".equals(getRole(request))) {
            throw new BusinessException(403, "Admin role required");
        }
    }

    // ==================== 接口 ====================

    @Operation(summary = "提交申请（所有已登录用户）")
    @PostMapping
    public Result<KeyApplication> submit(@RequestBody KeyApplicationRequest req,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        KeyApplication application = new KeyApplication();
        application.setUserId(userId);
        application.setKeyName(req.getKeyName());
        application.setAllowedModels(req.getAllowedModelsAsString());
        application.setReason(req.getReason());
        application.setTenantId(req.getTenantId());
        application.setAppId(req.getAppId());
        return Result.success(keyApplicationService.submitApplication(application));
    }

    @Operation(summary = "查询全部申请（仅 admin）")
    @GetMapping
    public Result<List<KeyApplication>> list(
            @RequestParam(required = false, defaultValue = "false") boolean pendingOnly,
            HttpServletRequest request) {
        requireAdmin(request);
        List<KeyApplication> list = pendingOnly
                ? keyApplicationService.listPending()
                : keyApplicationService.list();
        fillUsernames(list);
        return Result.success(list);
    }

    @Operation(summary = "查询我的申请（当前登录用户）")
    @GetMapping("/my")
    public Result<List<KeyApplication>> mine(HttpServletRequest request) {
        Long userId = getUserId(request);
        return Result.success(keyApplicationService.listByUser(userId));
    }

    @Operation(summary = "查询我的申请（兼容旧路径）")
    @GetMapping("/mine")
    public Result<List<KeyApplication>> mineAlias(HttpServletRequest request) {
        Long userId = getUserId(request);
        return Result.success(keyApplicationService.listByUser(userId));
    }

    @Operation(summary = "审批通过 - 自动生成 Key（仅 admin）")
    @PutMapping("/{id}/approve")
    public Result<ApiKey> approve(@PathVariable Long id,
                                   @RequestBody Map<String, Object> body,
                                   HttpServletRequest request) {
        requireAdmin(request);
        Long approverId = getUserId(request);
        String comment    = body.getOrDefault("comment", "").toString();
        Long totalQuota   = body.get("totalQuota") != null ? Long.parseLong(body.get("totalQuota").toString()) : 0L;
        String expireTime = body.get("expireTime") != null ? body.get("expireTime").toString() : null;
        Long targetKeyId = null;
        if (body.get("targetKeyId") != null) {
            Object v = body.get("targetKeyId");
            if (v != null && !v.toString().isBlank()) targetKeyId = Long.parseLong(v.toString());
        }
        ApiKey key = keyApplicationService.approve(id, approverId, comment, totalQuota, expireTime, targetKeyId);
        return Result.success(key);
    }

    @Operation(summary = "审批拒绝（仅 admin）")
    @PutMapping("/{id}/reject")
    public Result<Boolean> reject(@PathVariable Long id,
                                   @RequestBody Map<String, String> body,
                                   HttpServletRequest request) {
        requireAdmin(request);   // ← 权限门卫
        Long approverId = getUserId(request);
        String comment = body.getOrDefault("comment", "");
        keyApplicationService.reject(id, approverId, comment);
        return Result.success(true);
    }
}

package com.aigateway.admin.controller;

import com.aigateway.admin.entity.Model;
import com.aigateway.admin.entity.ModelSubscription;
import com.aigateway.admin.service.ModelService;
import com.aigateway.admin.service.ModelSubscriptionService;
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
 * 模型订阅管理
 *
 * 前端调用方式：
 *   POST /api/admin/model-subscriptions/subscribe?modelId=1
 *   POST /api/admin/model-subscriptions/unsubscribe?modelId=1
 *   GET  /api/admin/model-subscriptions/my
 *   GET  /api/admin/model-subscriptions  （可订阅模型列表，含当前用户订阅状态）
 */
@Tag(name = "Model Subscription Management", description = "模型订阅管理")
@RestController
@RequestMapping("/api/admin/model-subscriptions")
@RequiredArgsConstructor
public class ModelSubscriptionController {

    private final ModelService modelService;
    private final ModelSubscriptionService subscriptionService;

    /** 从请求中解析当前登录用户 ID（优先 JWT attribute，其次 query param） */
    private Long resolveUserId(HttpServletRequest request, Long queryUserId) {
        if (queryUserId != null) return queryUserId;
        Object attr = request.getAttribute("userId");
        return attr != null ? (Long) attr : null;
    }

    @Operation(summary = "获取可订阅模型列表（admin 看全部，普通用户只看自己已订阅的）")
    @GetMapping
    public Result<List<Map<String, Object>>> listModels(
            HttpServletRequest request,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String appId) {

        Long resolvedUserId = resolveUserId(request, userId);
        Object roleAttr = request.getAttribute("role");
        boolean isAdmin = roleAttr != null && "admin".equals(roleAttr.toString());

        // admin 和普通用户都返回所有平台可用模型，标记当前用户已订阅（审批通过）的模型
        List<Model> models = modelService.listAvailableModels();
        List<Long> subscribedModelIds = List.of();
        if (resolvedUserId != null) {
            subscribedModelIds = subscriptionService.listUserSubscriptions(
                    resolvedUserId,
                    tenantId != null ? tenantId : "",
                    appId != null ? appId : ""
            ).stream().map(ModelSubscription::getModelId).collect(Collectors.toList());
        }
        final List<Long> finalSubscribed = subscribedModelIds;
        List<Map<String, Object>> result = models.stream().map(m -> Map.<String, Object>of(
                "id", m.getId(),
                "modelName", m.getModelName(),
                "provider", m.getProvider(),
                "inputPrice", m.getInputPrice(),
                "outputPrice", m.getOutputPrice(),
                "description", m.getDescription() != null ? m.getDescription() : "",
                "subscribed", finalSubscribed.contains(m.getId())
        )).collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "获取我的订阅（从 JWT 中读取当前用户）")
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMySubscriptions(
            HttpServletRequest request,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String appId) {

        Long resolvedUserId = resolveUserId(request, userId);
        if (resolvedUserId == null) {
            return Result.error(401, "Not authenticated");
        }

        List<ModelSubscription> subs = subscriptionService.listUserSubscriptions(
                resolvedUserId,
                tenantId != null ? tenantId : "",
                appId != null ? appId : "");
        List<Long> modelIds = subs.stream()
                .map(ModelSubscription::getModelId)
                .collect(Collectors.toList());
        List<Model> models = modelIds.isEmpty()
                ? List.of()
                : modelService.listByIds(modelIds);

        List<Map<String, Object>> result = models.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "modelName", m.getModelName() != null ? m.getModelName() : "",
                        "provider", m.getProvider() != null ? m.getProvider() : "",
                        "inputPrice", m.getInputPrice() != null ? m.getInputPrice() : 0,
                        "outputPrice", m.getOutputPrice() != null ? m.getOutputPrice() : 0,
                        "description", m.getDescription() != null ? m.getDescription() : "",
                        "subscribed", true
                ))
                .collect(Collectors.toList());

        return Result.success(result);
    }

    @Operation(summary = "订阅模型（从 JWT 读取当前用户）")
    @PostMapping("/subscribe")
    public Result<Boolean> subscribeByParam(
            HttpServletRequest request,
            @RequestParam Long modelId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String appId) {
        Long resolvedUserId = resolveUserId(request, userId);
        if (resolvedUserId == null) return Result.error(401, "Not authenticated");
        subscriptionService.subscribe(
                modelId, resolvedUserId,
                tenantId != null ? tenantId : "",
                appId != null ? appId : "");
        return Result.success(true);
    }

    @Operation(summary = "取消订阅（从 JWT 读取当前用户）")
    @PostMapping("/unsubscribe")
    public Result<Boolean> unsubscribeByParam(
            HttpServletRequest request,
            @RequestParam Long modelId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String appId) {
        Long resolvedUserId = resolveUserId(request, userId);
        if (resolvedUserId == null) return Result.error(401, "Not authenticated");
        subscriptionService.unsubscribe(
                modelId, resolvedUserId,
                tenantId != null ? tenantId : "",
                appId != null ? appId : "");
        return Result.success(true);
    }

    @Operation(summary = "订阅模型（PathVariable 方式，兼容旧路径）")
    @PostMapping("/{id}/subscribe")
    public Result<Boolean> subscribeModel(HttpServletRequest request, @PathVariable Long id) {
        Long resolvedUserId = resolveUserId(request, null);
        subscriptionService.subscribe(id, resolvedUserId != null ? resolvedUserId : 1L, "", "");
        return Result.success(true);
    }

    @Operation(summary = "取消订阅（PathVariable 方式，兼容旧路径）")
    @DeleteMapping("/{id}/subscribe")
    public Result<Boolean> unsubscribeModel(HttpServletRequest request, @PathVariable Long id) {
        Long resolvedUserId = resolveUserId(request, null);
        subscriptionService.unsubscribe(id, resolvedUserId != null ? resolvedUserId : 1L, "", "");
        return Result.success(true);
    }
}

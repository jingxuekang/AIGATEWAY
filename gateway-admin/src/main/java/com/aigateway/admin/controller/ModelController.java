package com.aigateway.admin.controller;

import com.aigateway.admin.entity.Model;
import com.aigateway.admin.service.ModelService;
import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型控制器
 * GET  / - 所有已登录用户（查看模型列表）
 * POST / PUT - 仅 admin
 */
@Tag(name = "Model Management", description = "模型管理")
@RestController
@RequestMapping("/api/admin/models")
@RequiredArgsConstructor
public class ModelController {
    
    private final ModelService modelService;

    private static void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (role == null || !"admin".equals(role.toString())) {
            throw new BusinessException(403, "Forbidden: admin role required");
        }
    }
    
    @Operation(summary = "获取可用模型列表（所有已登录用户）")
    @GetMapping
    public Result<List<Model>> listModels() {
        List<Model> models = modelService.listAvailableModels();
        return Result.success(models);
    }
    
    @Operation(summary = "发布新模型（仅 admin）")
    @PostMapping
    public Result<Model> createModel(HttpServletRequest request, @RequestBody Model model) {
        requireAdmin(request);
        modelService.save(model);
        return Result.success(model);
    }
    
    @Operation(summary = "更新模型（仅 admin）")
    @PutMapping("/{id}")
    public Result<Boolean> updateModel(HttpServletRequest request, @PathVariable Long id, @RequestBody Model model) {
        requireAdmin(request);
        model.setId(id);
        return Result.success(modelService.updateById(model));
    }
}
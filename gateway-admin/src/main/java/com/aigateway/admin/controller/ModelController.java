package com.aigateway.admin.controller;

import com.aigateway.admin.entity.Model;
import com.aigateway.admin.service.ModelService;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型控制器
 */
@Tag(name = "Model Management", description = "模型管理")
@RestController
@RequestMapping("/api/admin/models")
@RequiredArgsConstructor
public class ModelController {
    
    private final ModelService modelService;
    
    @Operation(summary = "获取可用模型列表")
    @GetMapping
    public Result<List<Model>> listModels() {
        List<Model> models = modelService.listAvailableModels();
        return Result.success(models);
    }
    
    @Operation(summary = "发布新模型")
    @PostMapping
    public Result<Model> createModel(@RequestBody Model model) {
        modelService.save(model);
        return Result.success(model);
    }
    
    @Operation(summary = "更新模型")
    @PutMapping("/{id}")
    public Result<Boolean> updateModel(@PathVariable Long id, @RequestBody Model model) {
        model.setId(id);
        return Result.success(modelService.updateById(model));
    }
}
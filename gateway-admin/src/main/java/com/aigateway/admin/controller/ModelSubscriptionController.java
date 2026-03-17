package com.aigateway.admin.controller;

import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Model Subscription Management", description = "模型订阅管理")
@RestController
@RequestMapping("/api/admin/model-subscriptions")
@RequiredArgsConstructor
public class ModelSubscriptionController {
    
    @Operation(summary = "获取可订阅模型列表")
    @GetMapping
    public Result<List<Map<String, Object>>> listModels() {
        List<Map<String, Object>> models = new ArrayList<>();
        
        String[][] modelData = {
            {"gpt-4", "openai", "30", "60", "GPT-4 模型", "true"},
            {"gpt-3.5-turbo", "openai", "1.5", "2", "GPT-3.5 Turbo 模型", "true"},
            {"claude-3-opus", "anthropic", "15", "75", "Claude 3 Opus 模型", "false"},
            {"claude-3-sonnet", "anthropic", "3", "15", "Claude 3 Sonnet 模型", "true"},
        };
        
        for (int i = 0; i < modelData.length; i++) {
            Map<String, Object> model = new HashMap<>();
            model.put("id", i + 1);
            model.put("modelName", modelData[i][0]);
            model.put("provider", modelData[i][1]);
            model.put("inputPrice", Double.parseDouble(modelData[i][2]));
            model.put("outputPrice", Double.parseDouble(modelData[i][3]));
            model.put("description", modelData[i][4]);
            model.put("subscribed", Boolean.parseBoolean(modelData[i][5]));
            models.add(model);
        }
        
        return Result.success(models);
    }
    
    @Operation(summary = "订阅模型")
    @PostMapping("/{id}/subscribe")
    public Result<Boolean> subscribeModel(@PathVariable Long id) {
        return Result.success(true);
    }
    
    @Operation(summary = "取消订阅")
    @DeleteMapping("/{id}/subscribe")
    public Result<Boolean> unsubscribeModel(@PathVariable Long id) {
        return Result.success(true);
    }
}

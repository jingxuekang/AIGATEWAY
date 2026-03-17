package com.aigateway.core.controller;

import com.aigateway.common.result.Result;
import com.aigateway.core.client.AdminClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的模型列表接口
 * GET /v1/models
 */
@Slf4j
@Tag(name = "Models API", description = "OpenAI 兼容模型列表接口")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ModelsController {

    private final AdminClient adminClient;

    @Operation(summary = "获取可用模型列表", description = "返回 OpenAI 兼容格式的模型列表")
    @GetMapping("/models")
    public Result<Map<String, Object>> listModels() {
        List<String> modelIds = adminClient.listAvailableModelIds();
        long now = System.currentTimeMillis() / 1000;

        List<Map<String, Object>> data = modelIds.stream()
                .map(id -> Map.<String, Object>ofEntries(
                        Map.entry("id", id),
                        Map.entry("object", "model"),
                        Map.entry("created", now),
                        Map.entry("owned_by", "aigateway")
                ))
                .toList();

        return Result.success(Map.of(
                "object", "list",
                "data", data
        ));
    }
}

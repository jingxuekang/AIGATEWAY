package com.aigateway.core.controller;

import com.aigateway.common.result.Result;
import com.aigateway.core.service.ChatService;
import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * OpenAI 兼容聊天接口
 *
 * POST /v1/chat/completions
 *   - stream=false（默认）: 同步返回 JSON
 *   - stream=true : 返回 SSE 流
 *
 * 客户端通过请求体中 stream 字段控制，与 OpenAI API 完全兼容。
 */
@Tag(name = "Chat API", description = "OpenAI 兼容聊天接口，支持同步与 SSE 流式输出")
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 统一聊天入口：根据 stream 字段自动选择同步或流式响应。
     * 注意：Spring MVC 通过 produces 区分，但为了兼容 OpenAI SDK 直接 POST 同一路径，
     * 这里用两个端点分别处理，客户端无感知（SDK 内部会判断 stream）。
     */
    @Operation(summary = "Chat Completions (Sync)",
            description = "同步聊天接口，stream=false 时使用，返回完整 JSON 响应")
    @PostMapping("/chat/completions")
    public Object chatCompletions(@RequestBody ChatRequest request) {
        if (Boolean.TRUE.equals(request.getStream())) {
            // 兼容：客户端 POST 到同一端点但 stream=true，返回 SSE
            // 此场景由客户端直接调 /chat/completions/stream 更规范，
            // 这里也支持，避免客户端配置问题
            log.warn("Client sent stream=true to sync endpoint, redirecting to stream handler");
            return chatService.chatStream(request);
        }
        log.info("[Sync] model={}", request.getModel());
        ChatResponse response = chatService.chat(request);
        return Result.success(response);
    }

    /**
     * 流式聊天接口（SSE）
     * 客户端设置 Accept: text/event-stream 或直接调用此端点。
     */
    @Operation(summary = "Chat Completions (Stream / SSE)",
            description = "流式聊天接口，返回 Server-Sent Events 格式数据，与 OpenAI stream=true 兼容")
    @PostMapping(value = "/chat/completions/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatCompletionsStream(@RequestBody ChatRequest request) {
        log.info("[Stream] model={}", request.getModel());
        request.setStream(true);
        return chatService.chatStream(request);
    }
}

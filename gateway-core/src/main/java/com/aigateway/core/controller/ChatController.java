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
 * POST /v1/chat/completions          stream=false: 同步 JSON
 * POST /v1/chat/completions          stream=true:  SSE 流
 * POST /v1/chat/completions/stream   显式流式端点
 */
@Tag(name = "Chat API", description = "OpenAI 兼容聊天接口，支持同步与 SSE 流式输出")
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 同步聊天（stream=false）
     */
    @Operation(summary = "Chat Completions (Sync)",
            description = "同步聊天接口，返回完整 JSON 响应")
    @PostMapping(value = "/chat/completions",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ChatResponse> chatCompletions(@RequestBody ChatRequest request) {
        if (Boolean.TRUE.equals(request.getStream())) {
            // stream=true 但调用了同步端点，返回 400 提示客户端使用正确端点
            throw new IllegalArgumentException(
                "Use POST /v1/chat/completions/stream or set Accept: text/event-stream for streaming.");
        }
        log.info("[Sync] model={}", request.getModel());
        ChatResponse response = chatService.chat(request);
        return Result.success(response);
    }

    /**
     * 流式聊天（SSE）— 兼容两种调用方式：
     * 1. POST /v1/chat/completions/stream
     * 2. POST /v1/chat/completions with Accept: text/event-stream
     */
    @Operation(summary = "Chat Completions (Stream / SSE)",
            description = "流式聊天接口，返回 Server-Sent Events 格式")
    @PostMapping(value = "/chat/completions/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatCompletionsStream(@RequestBody ChatRequest request) {
        log.info("[Stream] model={}", request.getModel());
        request.setStream(true);
        return chatService.chatStream(request);
    }

    /**
     * 兼容：同一路径 /v1/chat/completions 使用 Accept: text/event-stream
     */
    @PostMapping(value = "/chat/completions",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatCompletionsSSE(@RequestBody ChatRequest request) {
        log.info("[Stream-SSE] model={}", request.getModel());
        request.setStream(true);
        return chatService.chatStream(request);
    }

    /**
     * 多模态接口（火山引擎 Responses API 兼容）
     * 接受 OpenAI 多模态格式（messages 含 image_url），路由到火山引擎 /responses 接口
     *
     * 请求格式示例：
     * {
     *   "model": "doubao-seed-2-0-pro-260215",
     *   "messages": [{
     *     "role": "user",
     *     "content": [
     *       {"type": "image_url", "image_url": {"url": "https://..."}},
     *       {"type": "text", "text": "你看见了什么？"}
     *     ]
     *   }]
     * }
     */
    @Operation(summary = "Multimodal Responses API",
            description = "多模态接口，支持图片+文本输入，路由到火山引擎 Responses API")
    @PostMapping(value = "/responses",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ChatResponse> responses(@RequestBody ChatRequest request) {
        log.info("[Multimodal] model={}", request.getModel());
        ChatResponse response = chatService.chatMultiModal(request);
        return Result.success(response);
    }
}

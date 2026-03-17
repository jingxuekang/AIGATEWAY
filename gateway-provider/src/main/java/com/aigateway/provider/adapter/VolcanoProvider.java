package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 火山方舟（字节跳动）Provider 适配器
 * 
 * 支持的模型：
 * - doubao-pro-32k
 * - doubao-lite-32k
 * - doubao-character-32k
 * 
 * API 文档: https://www.volcengine.com/docs/82379/1099475
 */
@Slf4j
@Component
public class VolcanoProvider implements ModelProvider {
    
    @Value("${provider.volcano.api-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String volcanoApiUrl;
    
    @Value("${provider.volcano.api-key:}")
    private String volcanoApiKey;
    
    private final WebClient webClient;
    
    public VolcanoProvider() {
        this.webClient = WebClient.builder()
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    @Override
    public String getProviderName() {
        return "volcano";
    }
    
    @Override
    public boolean supports(String model) {
        return model != null && (
            model.startsWith("doubao-") || 
            model.contains("volcano") ||
            model.startsWith("ep-")  // 火山方舟的 endpoint ID
        );
    }
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("Volcano chat request: model={}", request.getModel());
        
        if (volcanoApiKey == null || volcanoApiKey.isEmpty()) {
            log.warn("Volcano API Key not configured, returning mock response");
            return createMockResponse(request);
        }
        
        try {
            // 1. 转换请求格式为火山方舟格式
            Map<String, Object> volcanoRequest = convertToVolcanoFormat(request);
            
            // 2. 调用火山方舟 API
            Map<String, Object> volcanoResponse = webClient
                .post()
                .uri(volcanoApiUrl + "/chat/completions")
                .header("Authorization", "Bearer " + volcanoApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(volcanoRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            // 3. 转换响应为统一格式
            return convertFromVolcanoFormat(volcanoResponse, request.getModel());
            
        } catch (Exception e) {
            log.error("Volcano API call failed", e);
            return createMockResponse(request);
        }
    }
    
    @Override
    public Flux<String> chatStream(ChatRequest request) {
        log.info("Volcano stream request: model={}", request.getModel());
        
        if (volcanoApiKey == null || volcanoApiKey.isEmpty()) {
            log.warn("Volcano API Key not configured, returning mock stream");
            return createMockStream(request);
        }
        
        try {
            // 1. 转换请求格式
            Map<String, Object> volcanoRequest = convertToVolcanoFormat(request);
            volcanoRequest.put("stream", true);
            
            // 2. 调用火山方舟流式 API
            return webClient
                .post()
                .uri(volcanoApiUrl + "/chat/completions")
                .header("Authorization", "Bearer " + volcanoApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(volcanoRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::convertVolcanoStreamToOpenAI);
                
        } catch (Exception e) {
            log.error("Volcano stream API call failed", e);
            return createMockStream(request);
        }
    }
    
    /**
     * 转换请求格式为火山方舟格式
     */
    private Map<String, Object> convertToVolcanoFormat(ChatRequest request) {
        Map<String, Object> volcanoRequest = new HashMap<>();
        
        // 模型名称
        volcanoRequest.put("model", request.getModel());
        
        // 消息列表（格式相同）
        volcanoRequest.put("messages", request.getMessages());
        
        // 参数映射
        Map<String, Object> parameters = new HashMap<>();
        if (request.getTemperature() != null) {
            parameters.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            parameters.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            parameters.put("top_p", request.getTopP());
        }
        
        // 火山方舟特有参数
        if (!parameters.isEmpty()) {
            volcanoRequest.putAll(parameters);
        }
        
        return volcanoRequest;
    }
    
    /**
     * 转换火山方舟响应为统一格式
     */
    private ChatResponse convertFromVolcanoFormat(Map<String, Object> volcanoResponse, String model) {
        ChatResponse response = new ChatResponse();
        
        // 基本信息
        response.setId((String) volcanoResponse.get("id"));
        response.setModel(model);
        response.setObject("chat.completion");
        response.setCreated(((Number) volcanoResponse.get("created")).longValue());
        
        // 选择列表
        java.util.List<Map<String, Object>> choices = 
            (java.util.List<Map<String, Object>>) volcanoResponse.get("choices");
        
        if (choices != null && !choices.isEmpty()) {
            java.util.List<ChatResponse.Choice> responseChoices = choices.stream()
                .map(choice -> {
                    ChatResponse.Choice c = new ChatResponse.Choice();
                    c.setIndex(((Number) choice.get("index")).intValue());
                    c.setFinishReason((String) choice.get("finish_reason"));
                    
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null) {
                        ChatResponse.Message msg = new ChatResponse.Message();
                        msg.setRole((String) message.get("role"));
                        msg.setContent((String) message.get("content"));
                        c.setMessage(msg);
                    }
                    
                    return c;
                })
                .collect(Collectors.toList());
            
            response.setChoices(responseChoices);
        }
        
        // 使用统计
        Map<String, Object> usage = (Map<String, Object>) volcanoResponse.get("usage");
        if (usage != null) {
            ChatResponse.Usage u = new ChatResponse.Usage();
            u.setPromptTokens(((Number) usage.get("prompt_tokens")).intValue());
            u.setCompletionTokens(((Number) usage.get("completion_tokens")).intValue());
            u.setTotalTokens(((Number) usage.get("total_tokens")).intValue());
            response.setUsage(u);
        }
        
        return response;
    }
    
    /**
     * 转换火山方舟流式响应为 OpenAI 格式
     */
    private String convertVolcanoStreamToOpenAI(String volcanoChunk) {
        // 火山方舟的流式格式与 OpenAI 类似，可能需要少量转换
        if (volcanoChunk.startsWith("data: ")) {
            return volcanoChunk;
        }
        return "data: " + volcanoChunk + "\n\n";
    }
    
    /**
     * 创建模拟响应（用于测试）
     */
    private ChatResponse createMockResponse(ChatRequest request) {
        ChatResponse response = new ChatResponse();
        response.setId("volcano-mock-" + System.currentTimeMillis());
        response.setModel(request.getModel());
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        
        ChatResponse.Choice choice = new ChatResponse.Choice();
        choice.setIndex(0);
        choice.setFinishReason("stop");
        
        ChatResponse.Message message = new ChatResponse.Message();
        message.setRole("assistant");
        message.setContent("这是来自火山方舟 " + request.getModel() + " 的模拟响应。请配置真实的 API Key 以使用实际的 AI 模型。");
        choice.setMessage(message);
        
        response.setChoices(java.util.Collections.singletonList(choice));
        
        ChatResponse.Usage usage = new ChatResponse.Usage();
        usage.setPromptTokens(10);
        usage.setCompletionTokens(20);
        usage.setTotalTokens(30);
        response.setUsage(usage);
        
        return response;
    }
    
    /**
     * 创建模拟流式响应（用于测试）
     */
    private Flux<String> createMockStream(ChatRequest request) {
        return Flux.just(
            "data: {\"id\":\"volcano-mock\",\"object\":\"chat.completion.chunk\",\"created\":" + 
                (System.currentTimeMillis() / 1000) + ",\"model\":\"" + request.getModel() + 
                "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"这是\"}}]}\n\n",
            "data: {\"id\":\"volcano-mock\",\"object\":\"chat.completion.chunk\",\"created\":" + 
                (System.currentTimeMillis() / 1000) + ",\"model\":\"" + request.getModel() + 
                "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"来自火山方舟的\"}}]}\n\n",
            "data: {\"id\":\"volcano-mock\",\"object\":\"chat.completion.chunk\",\"created\":" + 
                (System.currentTimeMillis() / 1000) + ",\"model\":\"" + request.getModel() + 
                "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"模拟流式响应\"}}]}\n\n",
            "data: {\"id\":\"volcano-mock\",\"object\":\"chat.completion.chunk\",\"created\":" + 
                (System.currentTimeMillis() / 1000) + ",\"model\":\"" + request.getModel() + 
                "\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n",
            "data: [DONE]\n\n"
        );
    }
}

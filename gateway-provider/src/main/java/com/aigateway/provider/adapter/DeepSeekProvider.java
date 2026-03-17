package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * DeepSeek 提供商适配器
 * DeepSeek API 兼容 OpenAI 格式
 * 官方文档: https://platform.deepseek.com/api-docs/
 */
@Slf4j
@Component
public class DeepSeekProvider implements ModelProvider {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private WebClient webClient;
    
    @Value("${deepseek.api.key:}")
    private String apiKey;
    
    @Value("${deepseek.api.base-url:https://api.deepseek.com}")
    private String baseUrl;
    
    public DeepSeekProvider(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }
    
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }
        return webClient;
    }
    
    @Override
    public String getProviderName() {
        return "deepseek";
    }
    
    @Override
    public boolean supports(String model) {
        return model != null && model.startsWith("deepseek-");
    }
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("DeepSeek chat request: model={}", request.getModel());
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("DeepSeek API key not configured, returning mock response");
            return createMockResponse(request);
        }
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(request);
            
            // 调用 DeepSeek API
            Mono<ChatResponse> responseMono = getWebClient().post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .timeout(Duration.ofSeconds(60));
            
            return responseMono.block();
            
        } catch (Exception e) {
            log.error("DeepSeek API call failed", e);
            throw new RuntimeException("DeepSeek API call failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<String> chatStream(ChatRequest request) {
        log.info("DeepSeek stream request: model={}", request.getModel());
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("DeepSeek API key not configured, returning mock stream");
            return createMockStream(request);
        }
        
        try {
            // 构建请求体（启用流式）
            Map<String, Object> requestBody = buildRequestBody(request);
            requestBody.put("stream", true);
            
            // 调用 DeepSeek API（流式）
            return getWebClient().post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(60));
            
        } catch (Exception e) {
            log.error("DeepSeek stream API call failed", e);
            return Flux.error(new RuntimeException("DeepSeek stream API call failed: " + e.getMessage(), e));
        }
    }
    
    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        body.put("messages", request.getMessages());
        
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getFrequencyPenalty() != null) {
            body.put("frequency_penalty", request.getFrequencyPenalty());
        }
        if (request.getPresencePenalty() != null) {
            body.put("presence_penalty", request.getPresencePenalty());
        }
        if (request.getStop() != null) {
            body.put("stop", request.getStop());
        }
        
        return body;
    }
    
    /**
     * 创建模拟响应（当 API Key 未配置时）
     */
    private ChatResponse createMockResponse(ChatRequest request) {
        ChatResponse response = new ChatResponse();
        response.setId("chatcmpl-mock-deepseek-" + System.currentTimeMillis());
        response.setModel(request.getModel());
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        
        ChatResponse.Choice choice = new ChatResponse.Choice();
        choice.setIndex(0);
        choice.setFinishReason("stop");
        
        ChatResponse.Message message = new ChatResponse.Message();
        message.setRole("assistant");
        message.setContent("This is a mock response from DeepSeek. Please configure your DeepSeek API key in application.yml to use real DeepSeek models.");
        choice.setMessage(message);
        
        response.setChoices(java.util.Collections.singletonList(choice));
        
        ChatResponse.Usage usage = new ChatResponse.Usage();
        usage.setPromptTokens(15);
        usage.setCompletionTokens(25);
        usage.setTotalTokens(40);
        response.setUsage(usage);
        
        return response;
    }
    
    /**
     * 创建模拟流式响应（当 API Key 未配置时）
     */
    private Flux<String> createMockStream(ChatRequest request) {
        long timestamp = System.currentTimeMillis() / 1000;
        String model = request.getModel();
        
        return Flux.just(
            "data: {\"id\":\"chatcmpl-mock-deepseek\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"\"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-deepseek\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Mock \"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-deepseek\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"response \"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-deepseek\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"from \"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-deepseek\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"DeepSeek. \"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-deepseek\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Please configure API key.\"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-deepseek\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n",
            "data: [DONE]\n\n"
        );
    }
}

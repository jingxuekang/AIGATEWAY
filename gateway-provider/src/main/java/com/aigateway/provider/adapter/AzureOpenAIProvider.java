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
 * Azure OpenAI 提供商适配器
 * Azure OpenAI API 文档: https://learn.microsoft.com/en-us/azure/ai-services/openai/reference
 */
@Slf4j
@Component
public class AzureOpenAIProvider implements ModelProvider {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private WebClient webClient;
    
    @Value("${azure.openai.api-key:}")
    private String apiKey;
    
    @Value("${azure.openai.endpoint:}")
    private String endpoint;
    
    @Value("${azure.openai.deployment:gpt-4o-mini}")
    private String deployment;
    
    @Value("${azure.openai.api-version:2025-01-01-preview}")
    private String apiVersion;
    
    public AzureOpenAIProvider(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }
    
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(endpoint)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }
        return webClient;
    }
    
    @Override
    public String getProviderName() {
        return "azure-openai";
    }
    
    @Override
    public boolean supports(String model) {
        // 支持 azure- 前缀的模型，或者直接的 gpt 模型名
        return model != null && (
            model.startsWith("azure-") || 
            model.equals("gpt-4o-mini") ||
            model.equals("gpt-4") ||
            model.equals("gpt-35-turbo")
        );
    }
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("Azure OpenAI chat request: model={}, deployment={}", request.getModel(), deployment);
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_AZURE_API_KEY")) {
            log.warn("Azure OpenAI API key not configured, returning mock response");
            return createMockResponse(request);
        }
        
        if (endpoint == null || endpoint.isEmpty()) {
            log.error("Azure OpenAI endpoint not configured");
            throw new RuntimeException("Azure OpenAI endpoint not configured");
        }
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(request);
            
            // 构建 Azure OpenAI 特定的 URL
            String uri = String.format("/openai/deployments/%s/chat/completions?api-version=%s", 
                deployment, apiVersion);
            
            log.debug("Azure OpenAI request URL: {}{}", endpoint, uri);
            
            // 调用 Azure OpenAI API
            Mono<ChatResponse> responseMono = getWebClient().post()
                    .uri(uri)
                    .header("api-key", apiKey)  // Azure 使用 api-key 而不是 Authorization
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .timeout(Duration.ofSeconds(60));
            
            return responseMono.block();
            
        } catch (Exception e) {
            log.error("Azure OpenAI API call failed", e);
            throw new RuntimeException("Azure OpenAI API call failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<String> chatStream(ChatRequest request) {
        log.info("Azure OpenAI stream request: model={}, deployment={}", request.getModel(), deployment);
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_AZURE_API_KEY")) {
            log.warn("Azure OpenAI API key not configured, returning mock stream");
            return createMockStream(request);
        }
        
        if (endpoint == null || endpoint.isEmpty()) {
            log.error("Azure OpenAI endpoint not configured");
            return Flux.error(new RuntimeException("Azure OpenAI endpoint not configured"));
        }
        
        try {
            // 构建请求体（启用流式）
            Map<String, Object> requestBody = buildRequestBody(request);
            requestBody.put("stream", true);
            
            // 构建 Azure OpenAI 特定的 URL
            String uri = String.format("/openai/deployments/%s/chat/completions?api-version=%s", 
                deployment, apiVersion);
            
            // 调用 Azure OpenAI API（流式）
            return getWebClient().post()
                    .uri(uri)
                    .header("api-key", apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(60));
            
        } catch (Exception e) {
            log.error("Azure OpenAI stream API call failed", e);
            return Flux.error(new RuntimeException("Azure OpenAI stream API call failed: " + e.getMessage(), e));
        }
    }
    
    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        // Azure OpenAI 不需要在请求体中包含 model，因为已经在 URL 中指定了 deployment
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
        response.setId("chatcmpl-mock-azure-" + System.currentTimeMillis());
        response.setModel(request.getModel());
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        
        ChatResponse.Choice choice = new ChatResponse.Choice();
        choice.setIndex(0);
        choice.setFinishReason("stop");
        
        ChatResponse.Message message = new ChatResponse.Message();
        message.setRole("assistant");
        message.setContent("This is a mock response from Azure OpenAI. Please configure your Azure OpenAI API key in application.yml to use real Azure OpenAI models.");
        choice.setMessage(message);
        
        response.setChoices(java.util.Collections.singletonList(choice));
        
        ChatResponse.Usage usage = new ChatResponse.Usage();
        usage.setPromptTokens(15);
        usage.setCompletionTokens(30);
        usage.setTotalTokens(45);
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
            "data: {\"id\":\"chatcmpl-mock-azure\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"\"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-azure\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Mock \"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-azure\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"response \"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-azure\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"from \"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-azure\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Azure OpenAI. \"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-azure\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Please configure API key.\"}}]}\n\n",
            "data: {\"id\":\"chatcmpl-mock-azure\",\"object\":\"chat.completion.chunk\",\"created\":" + timestamp + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n",
            "data: [DONE]\n\n"
        );
    }
}

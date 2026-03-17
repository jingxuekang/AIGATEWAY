package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;

/**
 * Anthropic Claude Provider
 * Anthropic Messages API -> OpenAI ChatCompletion 格式统一转换
 */
@Slf4j
@Component
public class AnthropicProvider implements ModelProvider {

    @Value("${provider.anthropic.api-url:https://api.anthropic.com}")
    private String apiUrl;

    @Value("${provider.anthropic.api-key:}")
    private String apiKey;

    @Value("${provider.anthropic.version:2023-06-01}")
    private String apiVersion;

    @Value("${provider.anthropic.timeout:60000}")
    private long timeout;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() { return "anthropic"; }

    @Override
    public boolean supports(String model) {
        return model != null && model.startsWith("claude-");
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Anthropic] API key not configured, returning mock");
            return mockResponse(request.getModel());
        }
        try {
            log.info("[Anthropic] sync model={}", request.getModel());
            String json = webClient().post().uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(buildBody(request, false))
                    .retrieve().bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout)).block();
            return convertToOpenAI(json, request.getModel());
        } catch (Exception e) {
            log.error("[Anthropic] sync failed: {}", e.getMessage(), e);
            throw new RuntimeException("Anthropic call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Anthropic] API key not configured, returning mock stream");
            return mockStream(request.getModel());
        }
        log.info("[Anthropic] stream model={}", request.getModel());
        return webClient().post().uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(buildBody(request, true))
                .retrieve().bodyToFlux(String.class)
                .timeout(Duration.ofMillis(timeout))
                .map(this::convertStreamChunk)
                .filter(line -> !line.isBlank())
                .onErrorMap(e -> new RuntimeException("Anthropic stream failed: " + e.getMessage(), e));
    }

    private WebClient webClient() {
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", apiVersion)
                .build();
    }

    private Map<String, Object> buildBody(ChatRequest req, boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", req.getModel());
        body.put("stream", stream);
        body.put("max_tokens", req.getMaxTokens() != null ? req.getMaxTokens() : 4096);
        // Anthropic 区分 system 消息和 user/assistant 消息
        List<Map<String, Object>> messages = new ArrayList<>();
        if (req.getMessages() != null) {
            for (ChatRequest.Message m : req.getMessages()) {
                if ("system".equals(m.getRole())) {
                    body.put("system", m.getContent()); // Anthropic system 字段单独设置
                } else {
                    messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
                }
            }
        }
        body.put("messages", messages);
        if (req.getTemperature() != null) body.put("temperature", req.getTemperature());
        if (req.getTopP() != null) body.put("top_p", req.getTopP());
        if (req.getStop() != null) body.put("stop_sequences", req.getStop());
        return body;
    }

    /** Anthropic Messages API 响应 -> OpenAI ChatCompletion 格式 */
    private ChatResponse convertToOpenAI(String json, String model) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        ChatResponse r = new ChatResponse();
        r.setId(node.path("id").asText());
        r.setModel(model); r.setObject("chat.completion");
        r.setCreated(System.currentTimeMillis() / 1000);
        ChatResponse.Choice c = new ChatResponse.Choice(); c.setIndex(0);
        c.setFinishReason(node.path("stop_reason").asText("stop"));
        ChatResponse.Message m = new ChatResponse.Message(); m.setRole("assistant");
        JsonNode content = node.path("content");
        if (content.isArray() && content.size() > 0) {
            m.setContent(content.get(0).path("text").asText(""));
        }
        c.setMessage(m); r.setChoices(Collections.singletonList(c));
        ChatResponse.Usage u = new ChatResponse.Usage();
        JsonNode usage = node.path("usage");
        u.setPromptTokens(usage.path("input_tokens").asInt(0));
        u.setCompletionTokens(usage.path("output_tokens").asInt(0));
        u.setTotalTokens(u.getPromptTokens() + u.getCompletionTokens());
        u.setCacheCreationTokens(usage.path("cache_creation_input_tokens").asInt(0));
        u.setCacheReadTokens(usage.path("cache_read_input_tokens").asInt(0));
        r.setUsage(u);
        return r;
    }

    /** Anthropic SSE chunk -> OpenAI SSE 格式 */
    private String convertStreamChunk(String line) {
        if (line == null || line.isBlank()) return "";
        try {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if ("[DONE]".equals(data)) return "data: [DONE]\n\n";
                JsonNode node = objectMapper.readTree(data);
                String type = node.path("type").asText("");
                if ("content_block_delta".equals(type)) {
                    String text = node.path("delta").path("text").asText("");
                    String chunk = "{\"id\":\"anthropic-stream\",\"object\":\"chat.completion.chunk\","
                        + "\"created\":" + (System.currentTimeMillis()/1000) + ","
                        + "\"choices\":[{\"index\":0,\"delta\":{\"content\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}}]}";
                    return "data: " + chunk + "\n\n";
                }
                if ("message_stop".equals(type)) {
                    return "data: [DONE]\n\n";
                }
            }
        } catch (Exception e) {
            log.debug("[Anthropic] stream chunk parse error: {}", e.getMessage());
        }
        return "";
    }

    private ChatResponse mockResponse(String model) {
        ChatResponse r = new ChatResponse(); r.setId("mock-" + System.currentTimeMillis()); r.setModel(model);
        r.setObject("chat.completion"); r.setCreated(System.currentTimeMillis() / 1000);
        ChatResponse.Choice c = new ChatResponse.Choice(); c.setIndex(0); c.setFinishReason("stop");
        ChatResponse.Message m = new ChatResponse.Message(); m.setRole("assistant"); m.setContent("[Mock] Anthropic key not configured");
        c.setMessage(m); r.setChoices(Collections.singletonList(c));
        ChatResponse.Usage u = new ChatResponse.Usage(); u.setPromptTokens(0); u.setCompletionTokens(0); u.setTotalTokens(0); r.setUsage(u);
        return r;
    }

    private Flux<String> mockStream(String model) {
        long ts = System.currentTimeMillis() / 1000;
        return Flux.just(
            "data: {\"id\":\"mock\",\"object\":\"chat.completion.chunk\",\"created\":" + ts + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\"}}]}\n\n",
            "data: {\"id\":\"mock\",\"object\":\"chat.completion.chunk\",\"created\":" + ts + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"[Mock] Anthropic key not configured\"}}]}\n\n",
            "data: {\"id\":\"mock\",\"object\":\"chat.completion.chunk\",\"created\":" + ts + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n",
            "data: [DONE]\n\n"
        );
    }
}

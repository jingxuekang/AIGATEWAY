package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude Messages API Provider
 *
 * 与 OpenAI 协议的差异：
 *  1. 认证："x-api-key" + "anthropic-version" header
 *  2. 端点：POST /v1/messages
 *  3. 请求体：system 消息单独提取到顶层 "system" 字段；
 *             messages 数组只含 user/assistant
 *  4. 响应体：结构不同，转换为统一 ChatResponse
 *  5. 流式：SSE event 类型为 content_block_delta
 */
@Slf4j
public class AnthropicChannelProvider implements ModelProvider {

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String CHAT_URI          = "/v1/messages";

    private final long         channelId;
    private final String       channelName;
    private final String       provider;
    private final String       models;
    private final int          weight;
    private final int          timeout;
    private final ObjectMapper objectMapper;
    private final WebClient    webClient;

    public AnthropicChannelProvider(Map<String, Object> channelData,
                                    ObjectMapper objectMapper,
                                    WebClient.Builder webClientBuilder) {
        this.channelId   = toLong(channelData.get("id"));
        this.channelName = str(channelData.get("name"));
        this.provider    = str(channelData.get("provider"));
        this.models      = str(channelData.get("models"));
        this.weight      = toInt(channelData.get("weight"), 100);
        this.timeout     = toInt(channelData.get("timeout"), 30000);
        this.objectMapper = objectMapper;

        String baseUrl = str(channelData.get("baseUrl"));
        String apiKey  = str(channelData.get("apiKey"));
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.anthropic.com";

        this.webClient = webClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type",      "application/json")
                .defaultHeader("x-api-key",         apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
    }

    @Override
    public String getProviderName() {
        return "channel:" + channelId + ":anthropic";
    }

    @Override
    public boolean supports(String model) {
        if (model == null) return false;
        if (models != null && !models.isBlank()) {
            for (String m : models.split(",")) {
                if (model.trim().equalsIgnoreCase(m.trim())) return true;
            }
        }
        return model.startsWith("claude-");
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String body = serializeBody(buildAnthropicRequest(request, false));
        try {
            String resp = webClient.post()
                    .uri(CHAT_URI)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
            return convertResponse(resp);
        } catch (Exception e) {
            log.error("[Anthropic:{}] chat failed: {}", channelName, e.getMessage(), e);
            throw new ProviderException("Channel[" + channelName + "] Anthropic call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        String body = serializeBody(buildAnthropicRequest(request, true));
        return webClient.post()
                .uri(CHAT_URI)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(timeout))
                .filter(line -> line != null && !line.isBlank())
                // 将 Anthropic SSE 格式透传（event: content_block_delta / data: {...}）
                // 上层 ChatService 可按需做格式转换；此处保持 SSE 原始行
                .map(line -> line.startsWith("data:") ? line + "\n\n" : "data: " + line + "\n\n")
                .doOnError(e -> log.error("[Anthropic:{}] stream failed: {}", channelName, e.getMessage(), e));
    }

    // -------------------------------------------------------------------------
    // Anthropic 协议转换
    // -------------------------------------------------------------------------

    /**
     * 将统一 ChatRequest 转换为 Anthropic Messages API 请求体。
     * system 消息提取为顶层字段，其余消息保留在 messages 数组中。
     */
    private Map<String, Object> buildAnthropicRequest(ChatRequest request, boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel());

        // 提取 system 消息
        List<Map<String, Object>> messages = new ArrayList<>();
        StringBuilder systemContent = new StringBuilder();

        if (request.getMessages() != null) {
            for (ChatRequest.Message msg : request.getMessages()) {
                if ("system".equals(msg.getRole())) {
                    if (systemContent.length() > 0) systemContent.append("\n");
                    systemContent.append(msg.getContentAsString());
                } else {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("role",    msg.getRole());
                    m.put("content", msg.getContent());
                    messages.add(m);
                }
            }
        }

        if (systemContent.length() > 0) {
            body.put("system", systemContent.toString());
        }
        body.put("messages", messages);

        // max_tokens 在 Anthropic 中为必填，给默认值
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);

        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());
        if (request.getTopP()        != null) body.put("top_p",       request.getTopP());
        if (stream) body.put("stream", true);

        return body;
    }

    /**
     * 将 Anthropic Messages API 响应转换为统一 ChatResponse。
     *
     * Anthropic 响应示例：
     * {
     *   "id": "msg_xxx",
     *   "type": "message",
     *   "role": "assistant",
     *   "content": [{"type":"text","text":"..."}],
     *   "model": "claude-3-5-sonnet-20241022",
     *   "usage": {"input_tokens": 10, "output_tokens": 100}
     * }
     */
    private ChatResponse convertResponse(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            ChatResponse response = new ChatResponse();
            response.setId(jsonStr(root, "id"));
            response.setModel(jsonStr(root, "model"));
            response.setObject("chat.completion");

            // 提取文本内容
            String text = "";
            JsonNode contentArr = root.path("content");
            if (contentArr.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode part : contentArr) {
                    if ("text".equals(jsonStr(part, "type"))) {
                        sb.append(jsonStr(part, "text"));
                    }
                }
                text = sb.toString();
            }

            ChatResponse.Message msg = new ChatResponse.Message();
            msg.setRole("assistant");
            msg.setContent(text);

            ChatResponse.Choice choice = new ChatResponse.Choice();
            choice.setIndex(0);
            choice.setMessage(msg);
            choice.setFinishReason(jsonStr(root, "stop_reason"));
            response.setChoices(List.of(choice));

            // 用量：Anthropic 用 input_tokens / output_tokens
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode()) {
                ChatResponse.Usage u = new ChatResponse.Usage();
                u.setPromptTokens(usage.path("input_tokens").asInt(0));
                u.setCompletionTokens(usage.path("output_tokens").asInt(0));
                u.setTotalTokens(u.getPromptTokens() + u.getCompletionTokens());
                // cache tokens（Claude 3.5 支持 prompt caching）
                if (usage.has("cache_creation_input_tokens"))
                    u.setCacheCreationTokens(usage.path("cache_creation_input_tokens").asInt(0));
                if (usage.has("cache_read_input_tokens"))
                    u.setCacheReadTokens(usage.path("cache_read_input_tokens").asInt(0));
                response.setUsage(u);
            }

            return response;
        } catch (Exception e) {
            log.error("[Anthropic:{}] Failed to parse response: {}", channelName, rawJson, e);
            throw new ProviderException("Failed to parse Anthropic response: " + e.getMessage(), e);
        }
    }

    public int  getWeight()    { return weight; }
    public long getChannelId() { return channelId; }

    private String serializeBody(Map<String, Object> bodyMap) {
        try {
            return objectMapper.writeValueAsString(bodyMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Anthropic request body", e);
        }
    }

    private static String jsonStr(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static String str(Object o)           { return o != null ? o.toString() : null; }
    private static long   toLong(Object o)         { return o != null ? Long.parseLong(o.toString()) : 0L; }
    private static int    toInt(Object o, int def) {
        if (o == null) return def;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return def; }
    }
}

package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAI 兼容协议抽象基类
 * 封装公共的 WebClient 调用、请求序列化、流式 SSE 解析逻辑。
 * 子类只需重写 getProviderName()、supports()、getChatUri()、buildRequestHeaders() 等钩子方法。
 */
@Slf4j
public abstract class AbstractOpenAiCompatibleProvider implements ModelProvider {

    protected final String channelName;
    protected final String provider;
    protected final String baseUrl;
    protected final String apiKey;
    protected final String models;
    protected final int    weight;
    protected final int    timeout;
    protected final ObjectMapper objectMapper;
    protected final WebClient webClient;

    protected AbstractOpenAiCompatibleProvider(Map<String, Object> channelData,
                                               ObjectMapper objectMapper,
                                               WebClient.Builder webClientBuilder) {
        this.channelName  = str(channelData.get("name"));
        this.provider     = str(channelData.get("provider"));
        this.baseUrl      = str(channelData.get("baseUrl"));
        this.apiKey       = str(channelData.get("apiKey"));
        this.models       = str(channelData.get("models"));
        this.weight       = toInt(channelData.get("weight"), 100);
        this.timeout      = toInt(channelData.get("timeout"), 30000);
        this.objectMapper = objectMapper;
        this.webClient    = buildWebClient(webClientBuilder);
    }

    // -------------------------------------------------------------------------
    // 钩子方法（子类可重写）
    // -------------------------------------------------------------------------

    /** 同步调用的 URI，根据请求内容自动判断端点 */
    protected String getChatUri(ChatRequest request) {
        // 火山引擎多模态请求（messages 中含 image_url）走 /responses 端点
        if (isMultiModal(request) && "volcano".equalsIgnoreCase(this.provider)) {
            return "/responses";
        }
        return "/chat/completions";
    }

    /** 检测请求是否包含多模态内容（image_url） */
    protected boolean isMultiModal(ChatRequest request) {
        if (request.getMessages() == null) return false;
        for (ChatRequest.Message msg : request.getMessages()) {
            Object content = msg.getContent();
            if (content instanceof java.util.List<?> list) {
                for (Object item : list) {
                    if (item instanceof java.util.Map<?, ?> map
                            && "image_url".equals(map.get("type"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 流式调用的 URI，默认同 getChatUri */
    protected String getStreamUri(ChatRequest request) {
        return getChatUri(request);
    }

    /** 构建 WebClient（子类可重写以定制 header 等） */
    protected WebClient buildWebClient(WebClient.Builder builder) {
        return builder.clone()
                .baseUrl(this.baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer " + this.apiKey)
                .build();
    }

    /** 构建请求体 Map，子类可重写以添加协议特有字段 */
    protected Map<String, Object> buildRequestBody(ChatRequest request, boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model",    request.getModel());
        body.put("messages", request.getMessages());
        if (stream) body.put("stream", true);
        if (request.getTemperature()     != null) body.put("temperature",       request.getTemperature());
        if (request.getMaxTokens()       != null) body.put("max_tokens",        request.getMaxTokens());
        if (request.getTopP()            != null) body.put("top_p",             request.getTopP());
        if (request.getFrequencyPenalty() != null) body.put("frequency_penalty", request.getFrequencyPenalty());
        if (request.getPresencePenalty()  != null) body.put("presence_penalty",  request.getPresencePenalty());
        if (request.getStop()            != null) body.put("stop",             request.getStop());
        return body;
    }

    // -------------------------------------------------------------------------
    // ModelProvider 接口实现（公共逻辑）
    // -------------------------------------------------------------------------

    @Override
    public boolean supports(String model) {
        if (model == null) return false;
        // 1. 按 channel.models 精确匹配（逗号分隔）
        if (models != null && !models.isBlank()) {
            for (String m : models.split(",")) {
                if (model.trim().equalsIgnoreCase(m.trim())) return true;
            }
        }
        // 2. 按 provider 前缀匹配兜底
        return matchByProvider(model);
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String uri  = getChatUri(request);
        String body = serializeBody(buildRequestBody(request, false));
        try {
            String resp = webClient.post()
                    .uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
            
            // 火山引擎 /responses 端点返回格式不同，需要转换
            if ("/responses".equals(uri)) {
                return convertVolcanoResponsesFormat(resp);
            }
            
            return objectMapper.readValue(resp, ChatResponse.class);
        } catch (Exception e) {
            logHttpError(uri, e);
            throw new ProviderException("Channel[" + channelName + "] call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 火山引擎 Responses API 返回格式转换为标准 ChatResponse
     * Responses API 返回: { "output": { "text": "..." }, "usage": {...} }
     * 标准格式:          { "choices": [{ "message": { "content": "..." } }], "usage": {...} }
     */
    protected ChatResponse convertVolcanoResponsesFormat(String rawJson) throws Exception {
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(rawJson);

        ChatResponse response = new ChatResponse();
        response.setId(root.path("id").asText());
        response.setModel(root.path("model").asText());
        response.setObject("chat.completion");

        // 火山引擎 Responses API output 是数组，遍历找 message 类型
        StringBuilder textBuilder = new StringBuilder();
        com.fasterxml.jackson.databind.JsonNode output = root.path("output");
        if (output.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode item : output) {
                String type = item.path("type").asText("");
                if ("message".equals(type)) {
                    // message 类型：content 是数组
                    com.fasterxml.jackson.databind.JsonNode contentArr = item.path("content");
                    if (contentArr.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode c : contentArr) {
                            if ("output_text".equals(c.path("type").asText())) {
                                textBuilder.append(c.path("text").asText(""));
                            }
                        }
                    }
                } else if ("reasoning".equals(type)) {
                    // reasoning 类型：跳过，不展示推理内容
                }
            }
        } else {
            // 兜底：尝试 output.text
            textBuilder.append(root.path("output").path("text").asText(""));
        }

        ChatResponse.Message msg = new ChatResponse.Message();
        msg.setRole("assistant");
        msg.setContent(textBuilder.toString());

        ChatResponse.Choice choice = new ChatResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(msg);
        choice.setFinishReason(root.path("incomplete_details").isMissingNode() ? "stop" : "length");
        response.setChoices(java.util.List.of(choice));

        // 用量信息
        com.fasterxml.jackson.databind.JsonNode usage = root.path("usage");
        if (!usage.isMissingNode()) {
            ChatResponse.Usage u = new ChatResponse.Usage();
            u.setPromptTokens(usage.path("input_tokens").asInt(usage.path("prompt_tokens").asInt(0)));
            u.setCompletionTokens(usage.path("output_tokens").asInt(usage.path("completion_tokens").asInt(0)));
            u.setTotalTokens(u.getPromptTokens() + u.getCompletionTokens());
            response.setUsage(u);
        }

        return response;
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        String uri  = getStreamUri(request);
        String body = serializeBody(buildRequestBody(request, true));
        try {
            return webClient.post()
                    .uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .filter(line -> line != null && !line.isBlank())
                    .map(line -> line.startsWith("data:") ? line + "\n\n" : "data: " + line + "\n\n");
        } catch (Exception e) {
            log.error("[{}] stream failed: {}", channelName, e.getMessage(), e);
            return Flux.error(new ProviderException(
                    "Channel[" + channelName + "] stream failed: " + e.getMessage(), e));
        }
    }

    // -------------------------------------------------------------------------
    // 内部工具
    // -------------------------------------------------------------------------

    protected String serializeBody(Map<String, Object> bodyMap) {
        try {
            return objectMapper.writeValueAsString(bodyMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    protected void logHttpError(String uri, Exception e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof WebClientResponseException w) {
                String body = w.getResponseBodyAsString();
                if (body != null && body.length() > 2000) body = body.substring(0, 2000);
                log.error("[{}] call failed uri={} status={} resp={}",
                        channelName, uri, w.getStatusCode(), body, e);
                return;
            }
            t = t.getCause();
        }
        log.error("[{}] call failed uri={} error={}", channelName, uri, e.getMessage(), e);
    }

    protected boolean matchByProvider(String model) {
        if (provider == null) return false;
        return switch (provider.toLowerCase()) {
            case "deepseek" -> model.startsWith("deepseek-");
            case "openai"   -> model.startsWith("gpt-") || model.startsWith("o1-")
                                || model.startsWith("o3-") || model.startsWith("o4-");
            case "volcano"  -> model.startsWith("doubao-") || model.startsWith("ep-");
            case "anthropic"-> model.startsWith("claude-");
            case "qwen"     -> model.startsWith("qwen-");
            case "moonshot" -> model.startsWith("moonshot-");
            case "glm"      -> model.startsWith("glm-");
            case "minimax"  -> model.startsWith("abab") || model.startsWith("minimax-");
            case "baichuan" -> model.startsWith("Baichuan");
            case "hunyuan"  -> model.startsWith("hunyuan-");
            case "yi"       -> model.startsWith("yi-");
            default         -> false;
        };
    }

    public int    getWeight()    { return weight; }
    public long   getChannelId() { return 0L; } // 子类可重写

    protected static String str(Object o)           { return o != null ? o.toString() : null; }
    protected static long   toLong(Object o)         { return o != null ? Long.parseLong(o.toString()) : 0L; }
    protected static int    toInt(Object o, int def) {
        if (o == null) return def;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return def; }
    }
}

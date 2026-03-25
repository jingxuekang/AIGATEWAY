package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import com.aigateway.provider.util.SpringAiResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude Messages API Provider（Spring AI 版本）
 *
 * Spring AI AnthropicChatModel 自动处理以下协议差异：
 *  1. 认证："x-api-key" + "anthropic-version" header
 *  2. 端点：POST /v1/messages
 *  3. system 消息自动提取到顶层 "system" 字段
 *  4. max_tokens 必填（Spring AI 有默认值）
 *  5. 响应格式自动映射（input_tokens/output_tokens 等）
 *  6. Claude prompt caching tokens 自动映射
 */
@Slf4j
public class AnthropicChannelProvider implements ModelProvider {

    private final long           channelId;
    private final String         channelName;
    private final String         provider;
    private final String         models;
    private final int            weight;
    private final AnthropicChatModel chatModel;

    public AnthropicChannelProvider(Map<String, Object> channelData) {
        this.channelId   = toLong(channelData.get("id"));
        this.channelName = str(channelData.get("name"));
        this.provider    = str(channelData.get("provider"));
        this.models      = str(channelData.get("models"));
        this.weight      = toInt(channelData.get("weight"), 100);

        String baseUrl = str(channelData.get("baseUrl"));
        String apiKey  = str(channelData.get("apiKey"));

        AnthropicApi.Builder apiBuilder = AnthropicApi.builder().apiKey(apiKey);
        if (baseUrl != null && !baseUrl.isBlank()) {
            apiBuilder.baseUrl(baseUrl);
        }

        this.chatModel = AnthropicChatModel.builder()
                .anthropicApi(apiBuilder.build())
                .defaultOptions(AnthropicChatOptions.builder()
                        .maxTokens(4096)   // Anthropic 必填，给合理默认值
                        .build())
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
        try {
            Prompt prompt = toPrompt(request);
            org.springframework.ai.chat.model.ChatResponse aiResponse = chatModel.call(prompt);
            return SpringAiResponseConverter.convert(aiResponse);
        } catch (Exception e) {
            log.error("[Anthropic:{}] chat failed: {}", channelName, e.getMessage(), e);
            throw new ProviderException("Channel[" + channelName + "] Anthropic call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        try {
            Prompt prompt = toPrompt(request);
            return chatModel.stream(prompt)
                    .map(chunk -> {
                        String json = SpringAiResponseConverter.toSseChunk(chunk);
                        return json.isEmpty() ? "" : "data: " + json + "\n\n";
                    })
                    .filter(line -> !line.isEmpty())
                    .concatWith(Flux.just("data: [DONE]\n\n"))
                    .doOnError(e -> log.error("[Anthropic:{}] stream failed: {}",
                            channelName, e.getMessage(), e));
        } catch (Exception e) {
            log.error("[Anthropic:{}] stream init failed: {}", channelName, e.getMessage(), e);
            return Flux.error(new ProviderException(
                    "Channel[" + channelName + "] Anthropic stream failed: " + e.getMessage(), e));
        }
    }

    // -------------------------------------------------------------------------
    // 请求转换：ChatRequest -> Spring AI Prompt
    // -------------------------------------------------------------------------

    private Prompt toPrompt(ChatRequest request) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        if (request.getMessages() != null) {
            for (ChatRequest.Message msg : request.getMessages()) {
                String content = msg.getContentAsString();
                if (content == null) content = "";
                switch (msg.getRole()) {
                    case "system"    -> messages.add(new SystemMessage(content));
                    case "assistant" -> messages.add(new AssistantMessage(content));
                    default          -> messages.add(new UserMessage(content));
                }
            }
        }

        AnthropicChatOptions.Builder optBuilder = AnthropicChatOptions.builder()
                .model(request.getModel())
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
        if (request.getTemperature() != null) optBuilder.temperature(request.getTemperature());
        if (request.getTopP()        != null) optBuilder.topP(request.getTopP());

        return new Prompt(messages, optBuilder.build());
    }

    @Override
    public int getWeight() { return weight; }
    public long getChannelId() { return channelId; }

    private static String str(Object o)           { return o != null ? o.toString() : null; }
    private static long   toLong(Object o)         { return o != null ? Long.parseLong(o.toString()) : 0L; }
    private static int    toInt(Object o, int def) {
        if (o == null) return def;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return def; }
    }
}

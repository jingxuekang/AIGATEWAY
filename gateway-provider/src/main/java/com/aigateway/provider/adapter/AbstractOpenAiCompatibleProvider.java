package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import com.aigateway.provider.util.SpringAiResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议抽象基类（Spring AI 版本）
 *
 * 使用 Spring AI OpenAiChatModel 替换手写 WebClient 调用。
 * 子类只需提供 getProviderName()、getChannelId()，以及可选重写 buildChatModel() 定制化配置。
 * AzureChannelProvider 等子类通过重写 buildChatModel() 注入不同的 API 实现。
 */
@Slf4j
public abstract class AbstractOpenAiCompatibleProvider implements ModelProvider {

    protected final String    channelName;
    protected final String    provider;
    protected final String    baseUrl;
    protected final String    apiKey;
    protected final String    models;
    protected final int       weight;
    protected final int       timeout;
    protected final ChatModel chatModel;

    protected AbstractOpenAiCompatibleProvider(Map<String, Object> channelData) {
        this.channelName = str(channelData.get("name"));
        this.provider    = str(channelData.get("provider"));
        this.baseUrl     = str(channelData.get("baseUrl"));
        this.apiKey      = str(channelData.get("apiKey"));
        this.models      = str(channelData.get("models"));
        this.weight      = toInt(channelData.get("weight"), 100);
        this.timeout     = toInt(channelData.get("timeout"), 30000);
        this.chatModel   = buildChatModel();
    }

    // -------------------------------------------------------------------------
    // 钩子方法（子类可重写）
    // -------------------------------------------------------------------------

    /**
     * 构建 Spring AI ChatModel。
     * 默认使用 OpenAiChatModel 指向 baseUrl（覆盖所有 OpenAI 兼容协议）。
     * AzureChannelProvider 重写此方法使用 AzureOpenAiChatModel。
     */
    protected ChatModel buildChatModel() {
        var api = OpenAiApi.builder()
                .baseUrl(this.baseUrl)
                .apiKey(this.apiKey)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .build();
    }

    // -------------------------------------------------------------------------
    // ModelProvider 接口实现
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
        try {
            Prompt prompt = toPrompt(request);
            org.springframework.ai.chat.model.ChatResponse aiResponse = chatModel.call(prompt);
            return SpringAiResponseConverter.convert(aiResponse);
        } catch (Exception e) {
            log.error("[{}] chat failed: {}", channelName, e.getMessage(), e);
            throw new ProviderException("Channel[" + channelName + "] call failed: " + e.getMessage(), e);
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
                    .concatWith(Flux.just("data: [DONE]\n\n"));
        } catch (Exception e) {
            log.error("[{}] stream failed: {}", channelName, e.getMessage(), e);
            return Flux.error(new ProviderException(
                    "Channel[" + channelName + "] stream failed: " + e.getMessage(), e));
        }
    }

    // -------------------------------------------------------------------------
    // 请求转换：ChatRequest -> Spring AI Prompt
    // -------------------------------------------------------------------------

    protected Prompt toPrompt(ChatRequest request) {
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

        OpenAiChatOptions.Builder optBuilder = OpenAiChatOptions.builder()
                .model(request.getModel());
        if (request.getTemperature()      != null) optBuilder.temperature(request.getTemperature());
        if (request.getMaxTokens()        != null) optBuilder.maxTokens(request.getMaxTokens());
        if (request.getTopP()             != null) optBuilder.topP(request.getTopP());
        if (request.getFrequencyPenalty() != null) optBuilder.frequencyPenalty(request.getFrequencyPenalty());
        if (request.getPresencePenalty()  != null) optBuilder.presencePenalty(request.getPresencePenalty());
        if (request.getStop()             != null) optBuilder.stop(request.getStop());

        return new Prompt(messages, optBuilder.build());
    }

    // -------------------------------------------------------------------------
    // 内部工具
    // -------------------------------------------------------------------------

    protected boolean matchByProvider(String model) {
        if (provider == null) return false;
        return switch (provider.toLowerCase()) {
            case "deepseek"  -> model.startsWith("deepseek-");
            case "openai"    -> model.startsWith("gpt-") || model.startsWith("o1-")
                                || model.startsWith("o3-") || model.startsWith("o4-");
            case "volcano"   -> model.startsWith("doubao-") || model.startsWith("ep-");
            case "anthropic" -> model.startsWith("claude-");
            case "qwen"      -> model.startsWith("qwen-");
            case "moonshot"  -> model.startsWith("moonshot-");
            case "glm"       -> model.startsWith("glm-");
            case "minimax"   -> model.startsWith("abab") || model.startsWith("minimax-");
            case "baichuan"  -> model.startsWith("Baichuan");
            case "hunyuan"   -> model.startsWith("hunyuan-");
            case "yi"        -> model.startsWith("yi-");
            default          -> false;
        };
    }

    @Override
    public int getWeight() { return weight; }
    public long getChannelId() { return 0L; }

    protected static String str(Object o)           { return o != null ? o.toString() : null; }
    protected static long   toLong(Object o)         { return o != null ? Long.parseLong(o.toString()) : 0L; }
    protected static int    toInt(Object o, int def) {
        if (o == null) return def;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return def; }
    }
}

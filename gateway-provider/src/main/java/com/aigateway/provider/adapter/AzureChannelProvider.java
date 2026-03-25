package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Azure OpenAI Service Provider
 *
 * 与 OpenAI 兼容协议的差异：
 *  1. 认证 header 使用 "api-key" 而非 "Authorization: Bearer ..."
 *  2. URI 携带 api-version query 参数
 *  3. model 字段对应 Azure 的 deployment name
 */
public class AzureChannelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String API_VERSION = "2025-01-01-preview";

    private final long channelId;

    public AzureChannelProvider(Map<String, Object> channelData,
                                ObjectMapper objectMapper,
                                WebClient.Builder webClientBuilder) {
        super(channelData, objectMapper, webClientBuilder);
        this.channelId = toLong(channelData.get("id"));
    }

    @Override
    public String getProviderName() {
        return "channel:" + channelId + ":azure";
    }

    @Override
    public long getChannelId() {
        return channelId;
    }

    /**
     * Azure 使用 "api-key" header 认证，不使用 Bearer token。
     */
    @Override
    protected WebClient buildWebClient(WebClient.Builder builder) {
        return builder.clone()
                .baseUrl(this.baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("api-key", this.apiKey)   // Azure 专用 header
                .build();
    }

    /**
     * Azure 的 Chat Completions 需要携带 api-version query 参数。
     */
    @Override
    protected String getChatUri(ChatRequest request) {
        return "/chat/completions?api-version=" + API_VERSION;
    }

    @Override
    protected boolean matchByProvider(String model) {
        // Azure deployment name 通常以 azure- 或 gpt- 开头
        return model.startsWith("azure-") || model.startsWith("gpt-");
    }
}

package com.aigateway.provider.adapter;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Azure OpenAI Service Provider（Spring AI 版本）
 *
 * Azure 与标准 OpenAI 的差异：
 *  1. 认证 header："api-key: xxx"（非 "Authorization: Bearer xxx"）
 *  2. URI 携带 api-version 参数
 *
 * 实现方案：使用 Spring AI OpenAiChatModel，通过自定义 RestClient
 * 注入 "api-key" header，复用 OpenAI 兼容协议的全部逻辑。
 */
public class AzureChannelProvider extends AbstractOpenAiCompatibleProvider {

    private final long channelId;

    public AzureChannelProvider(Map<String, Object> channelData) {
        super(channelData);
        this.channelId = toLong(channelData.get("id"));
    }

    /**
     * 重写 buildChatModel()：
     * 通过自定义 RestClient 注入 Azure 专用的 "api-key" header，
     * 替换标准 OpenAI 的 "Authorization: Bearer" 认证方式。
     * api-version 通过 baseUrl 拼接传入（由 channel 配置的 baseUrl 包含）。
     */
    @Override
    protected ChatModel buildChatModel() {
        // 构建携带 Azure api-key header 的 RestClient
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader("api-key", this.apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        // 构建携带 Azure api-key header 的 WebClient（用于流式）
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader("api-key", this.apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(this.baseUrl)
                .apiKey("placeholder")          // Azure 用 api-key header，此处占位
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().build())
                .build();
    }

    @Override
    public String getProviderName() {
        return "channel:" + channelId + ":azure";
    }

    @Override
    public long getChannelId() {
        return channelId;
    }

    @Override
    protected boolean matchByProvider(String model) {
        return model.startsWith("azure-") || model.startsWith("gpt-");
    }
}

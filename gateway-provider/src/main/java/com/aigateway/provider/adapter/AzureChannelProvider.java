package com.aigateway.provider.adapter;

import lombok.extern.slf4j.Slf4j;
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
 * Azure 端点格式：
 *   https://{resource}.openai.azure.com/openai/deployments/{deployment}/chat/completions?api-version=xxx
 *
 * channel 的 baseUrl 配置两种格式均支持：
 *   格式A（含 api-version）：
 *     https://xxx.openai.azure.com/openai/deployments/gpt-4o-mini?api-version=2024-02-01
 *   格式B（不含 api-version）：
 *     https://xxx.openai.azure.com/openai/deployments/gpt-4o-mini
 *
 * 实现：将 baseUrl 拆分为 rootUrl + completionsPath，
 *   rootUrl         = https://xxx.openai.azure.com
 *   completionsPath = /openai/deployments/gpt-4o-mini/chat/completions?api-version=xxx
 * 这样 Spring AI 不会再自动追加 /v1/chat/completions。
 */
@Slf4j
public class AzureChannelProvider extends AbstractOpenAiCompatibleProvider {

    private final long channelId;

    public AzureChannelProvider(Map<String, Object> channelData) {
        super(channelData);
        this.channelId = toLong(channelData.get("id"));
    }

    @Override
    protected ChatModel buildChatModel() {
        // 解析 baseUrl，提取 rootUrl 和 completionsPath
        // 例：https://myresource.openai.azure.com/openai/deployments/gpt-4o-mini?api-version=2024-02-01
        //  -> rootUrl         = https://myresource.openai.azure.com
        //  -> completionsPath = /openai/deployments/gpt-4o-mini/chat/completions?api-version=2024-02-01
        String rootUrl = extractRootUrl(this.baseUrl);
        String completionsPath = buildCompletionsPath(this.baseUrl);

        log.debug("[Azure] rootUrl={}, completionsPath={}", rootUrl, completionsPath);

        // 构建携带 Azure api-key header 的 RestClient
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(rootUrl)
                .defaultHeader("api-key", this.apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        // 构建携带 Azure api-key header 的 WebClient（用于流式）
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(rootUrl)
                .defaultHeader("api-key", this.apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(rootUrl)
                .apiKey("placeholder")           // Azure 用 api-key header，此处占位
                .completionsPath(completionsPath)
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().build())
                .build();
    }

    /**
     * 提取 schema + host，去掉路径和查询参数。
     * https://myresource.openai.azure.com/openai/... -> https://myresource.openai.azure.com
     */
    private static String extractRootUrl(String baseUrl) {
        if (baseUrl == null) return "";
        try {
            java.net.URI uri = new java.net.URI(baseUrl);
            return uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        } catch (Exception e) {
            // fallback: 截取到第三个 /
            int thirdSlash = baseUrl.indexOf('/', baseUrl.indexOf("//") + 2);
            return thirdSlash > 0 ? baseUrl.substring(0, thirdSlash) : baseUrl;
        }
    }

    /**
     * 构建 completionsPath：
     *   输入：https://xxx.openai.azure.com/openai/deployments/gpt-4o-mini?api-version=2024-02-01
     *   输出：/openai/deployments/gpt-4o-mini/chat/completions?api-version=2024-02-01
     *
     *   输入：https://xxx.openai.azure.com/openai/deployments/gpt-4o-mini
     *   输出：/openai/deployments/gpt-4o-mini/chat/completions?api-version=2024-02-01（使用默认版本）
     */
    private static final String DEFAULT_API_VERSION = "2024-02-01";

    private static String buildCompletionsPath(String baseUrl) {
        if (baseUrl == null) return "/openai/deployments/chat/completions?api-version=" + DEFAULT_API_VERSION;
        try {
            java.net.URI uri = new java.net.URI(baseUrl);
            String path = uri.getRawPath();
            // 去掉末尾的 /
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            // 拼接 /chat/completions
            String completionsPath = path + "/chat/completions";
            // 提取或使用默认 api-version
            String query = uri.getRawQuery();
            if (query != null && query.contains("api-version")) {
                completionsPath += "?" + query;
            } else {
                completionsPath += "?api-version=" + DEFAULT_API_VERSION;
            }
            return completionsPath;
        } catch (Exception e) {
            // fallback
            int qIdx = baseUrl.indexOf('?');
            String pathPart = qIdx > 0 ? baseUrl.substring(baseUrl.indexOf('/', baseUrl.indexOf("//") + 2), qIdx)
                                        : baseUrl.substring(baseUrl.indexOf('/', baseUrl.indexOf("//") + 2));
            if (pathPart.endsWith("/")) pathPart = pathPart.substring(0, pathPart.length() - 1);
            String queryPart = qIdx > 0 ? baseUrl.substring(qIdx + 1) : "api-version=" + DEFAULT_API_VERSION;
            return pathPart + "/chat/completions?" + queryPart;
        }
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

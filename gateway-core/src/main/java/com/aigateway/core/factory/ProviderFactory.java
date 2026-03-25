package com.aigateway.core.factory;

import com.aigateway.provider.adapter.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Provider 工厂
 *
 * 按渠道的 provider 类型实例化对应的 ModelProvider 实现类。
 * 遵循开闭原则：新增协议类型只需新增实现类 + 在此工厂加一个 case，
 * 不需要修改 ChannelProviderRegistry 或路由层。
 *
 * 协议分类：
 *   azure      → AzureChannelProvider
 *   anthropic  → AnthropicChannelProvider
 *   其他       → OpenAiCompatibleChannelProvider（OpenAI 兼容协议）
 *               包含：openai / deepseek / qwen / moonshot / glm /
 *                     minimax / baichuan / hunyuan / yi / volcano 等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderFactory {

    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    /**
     * 根据渠道数据创建对应的 ModelProvider 实例。
     * 返回的是普通 Java 对象（非 Spring Bean），生命周期由调用方管理。
     *
     * @param channelData 来自 admin 的渠道配置 Map
     * @return 对应协议的 ModelProvider 实例
     */
    public ModelProvider create(Map<String, Object> channelData) {
        String provider = channelData.get("provider") != null
                ? channelData.get("provider").toString().toLowerCase()
                : "";

        return switch (provider) {
            case "azure" -> {
                log.debug("[ProviderFactory] Creating AzureChannelProvider for channel: {}",
                        channelData.get("name"));
                yield new AzureChannelProvider(channelData, objectMapper, webClientBuilder);
            }
            case "anthropic" -> {
                log.debug("[ProviderFactory] Creating AnthropicChannelProvider for channel: {}",
                        channelData.get("name"));
                yield new AnthropicChannelProvider(channelData, objectMapper, webClientBuilder);
            }
            default -> {
                log.debug("[ProviderFactory] Creating OpenAiCompatibleChannelProvider for channel: {} (provider={})",
                        channelData.get("name"), provider);
                yield new OpenAiCompatibleChannelProvider(channelData, objectMapper, webClientBuilder);
            }
        };
    }
}

package com.aigateway.core.factory;

import com.aigateway.provider.adapter.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Provider 工厂（Spring AI 版本）
 *
 * 按渠道的 provider 类型实例化对应的 ModelProvider 实现类。
 * 各实现类内部通过 Spring AI 的 ChatModel 构建 API 客户端，
 * 不再需要注入 ObjectMapper 和 WebClient.Builder。
 *
 * 协议分类：
 *   azure      → AzureChannelProvider（Spring AI AzureOpenAiChatModel）
 *   anthropic  → AnthropicChannelProvider（Spring AI AnthropicChatModel）
 *   其他       → OpenAiCompatibleChannelProvider（Spring AI OpenAiChatModel）
 *               包含：openai / deepseek / qwen / moonshot / glm /
 *                     minimax / baichuan / hunyuan / yi / volcano 等
 */
@Slf4j
@Component
public class ProviderFactory {

    /**
     * 根据渠道数据创建对应的 ModelProvider 实例。
     * 返回的是普通 Java 对象（非 Spring Bean），生命周期由 ChannelProviderRegistry 管理。
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
                yield new AzureChannelProvider(channelData);
            }
            case "anthropic" -> {
                log.debug("[ProviderFactory] Creating AnthropicChannelProvider for channel: {}",
                        channelData.get("name"));
                yield new AnthropicChannelProvider(channelData);
            }
            default -> {
                log.debug("[ProviderFactory] Creating OpenAiCompatibleChannelProvider for channel: {} (provider={})",
                        channelData.get("name"), provider);
                yield new OpenAiCompatibleChannelProvider(channelData);
            }
        };
    }
}

package com.aigateway.provider.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * OpenAI 兼容协议 Provider
 * 覆盖所有使用 OpenAI 兼容接口的渠道：
 * DeepSeek、OpenAI、Qwen（阿里）、Moonshot（Kimi）、GLM（智谱）、
 * MiniMax、Baichuan、HunYuan、Yi（零一万物）、Volcano（豆包）等。
 *
 * 直接复用抽象基类全部逻辑，无需额外重写。
 */
public class OpenAiCompatibleChannelProvider extends AbstractOpenAiCompatibleProvider {

    private final long channelId;

    public OpenAiCompatibleChannelProvider(Map<String, Object> channelData,
                                           ObjectMapper objectMapper,
                                           WebClient.Builder webClientBuilder) {
        super(channelData, objectMapper, webClientBuilder);
        this.channelId = toLong(channelData.get("id"));
    }

    @Override
    public String getProviderName() {
        return "channel:" + channelId + ":" + provider;
    }

    @Override
    public long getChannelId() {
        return channelId;
    }
}

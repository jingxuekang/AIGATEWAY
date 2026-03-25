package com.aigateway.provider.adapter;

import java.util.Map;

/**
 * OpenAI 兼容协议 Provider
 * 覆盖所有使用 OpenAI 兼容接口的渠道：
 * DeepSeek、OpenAI、Qwen（阿里）、Moonshot（Kimi）、GLM（智谱）、
 * MiniMax、Baichuan、HunYuan、Yi（零一万物）、Volcano（豆包）等。
 *
 * 完全复用抽象基类逻辑，无需任何重写。
 * Spring AI OpenAiChatModel 直接处理 OpenAI 兼容协议的请求/响应。
 */
public class OpenAiCompatibleChannelProvider extends AbstractOpenAiCompatibleProvider {

    private final long channelId;

    public OpenAiCompatibleChannelProvider(Map<String, Object> channelData) {
        super(channelData);
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

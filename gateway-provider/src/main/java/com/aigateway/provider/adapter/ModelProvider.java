package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * 模型提供商接口
 */
public interface ModelProvider {
    
    /**
     * 获取提供商名称
     */
    String getProviderName();
    
    /**
     * 是否支持该模型
     */
    boolean supports(String model);
    
    /**
     * 同步调用
     */
    ChatResponse chat(ChatRequest request);
    
    /**
     * 流式调用
     */
    Flux<String> chatStream(ChatRequest request);
}

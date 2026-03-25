package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议 Provider
 * 覆盖所有使用 OpenAI 兼容接口的渠道：
 * DeepSeek、OpenAI、Qwen（阿里）、Moonshot（Kimi）、GLM（智谱）、
 * MiniMax、Baichuan、HunYuan、Yi（零一万物）、Volcano（豆包）等。
 *
 * 火山引擎多模态请求走 /responses 端点，请求体格式使用 input 字段。
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

    /**
     * 火山引擎多模态请求（/responses 端点）使用不同的请求体格式：
     *   - 普通 chat: { model, messages, ... }
     *   - 多模态:    { model, input: [...], ... }
     *
     * 火山引擎 Responses API content 类型：
     *   text    -> input_text
     *   image   -> input_image
     */
    @Override
    protected Map<String, Object> buildRequestBody(ChatRequest request, boolean stream) {
        // 非火山引擎或非多模态，走标准 OpenAI 格式
        if (!"volcano".equalsIgnoreCase(this.provider) || !isMultiModal(request)) {
            return super.buildRequestBody(request, stream);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel());

        List<Map<String, Object>> inputList = new ArrayList<>();
        if (request.getMessages() != null) {
            for (ChatRequest.Message msg : request.getMessages()) {
                Map<String, Object> inputMsg = new LinkedHashMap<>();
                inputMsg.put("role", msg.getRole());

                Object content = msg.getContent();
                if (content instanceof List<?> parts) {
                    List<Map<String, Object>> convertedParts = new ArrayList<>();
                    for (Object part : parts) {
                        if (part instanceof Map<?, ?> partMap) {
                            String type = String.valueOf(partMap.get("type"));
                            Map<String, Object> newPart = new LinkedHashMap<>();
                            if ("image_url".equals(type)) {
                                // OpenAI: {type:image_url, image_url:{url:"..."}}
                                // Volcano: {type:input_image, image_url:"..."}
                                newPart.put("type", "input_image");
                                Object imageUrlObj = partMap.get("image_url");
                                if (imageUrlObj instanceof Map<?, ?> imageUrlMap) {
                                    newPart.put("image_url", imageUrlMap.get("url"));
                                } else {
                                    newPart.put("image_url", imageUrlObj);
                                }
                            } else {
                                // text -> input_text
                                newPart.put("type", "input_text");
                                newPart.put("text", partMap.get("text"));
                            }
                            convertedParts.add(newPart);
                        }
                    }
                    inputMsg.put("content", convertedParts);
                } else {
                    inputMsg.put("content", content);
                }
                inputList.add(inputMsg);
            }
        }
        body.put("input", inputList);

        if (stream) body.put("stream", true);
        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());
        if (request.getMaxTokens()   != null) body.put("max_output_tokens", request.getMaxTokens());
        if (request.getTopP()        != null) body.put("top_p", request.getTopP());

        return body;
    }
}

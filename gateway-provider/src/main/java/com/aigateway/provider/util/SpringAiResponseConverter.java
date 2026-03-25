package com.aigateway.provider.util;

import com.aigateway.provider.model.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI 响应转换器
 * 将 Spring AI 的 ChatResponse 转换为项目统一的 ChatResponse 格式。
 */
@Slf4j
public class SpringAiResponseConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 将 Spring AI 同步响应转换为统一 ChatResponse
     */
    public static ChatResponse convert(org.springframework.ai.chat.model.ChatResponse aiResponse) {
        if (aiResponse == null) return null;

        ChatResponse response = new ChatResponse();
        response.setObject("chat.completion");

        List<ChatResponse.Choice> choices = new ArrayList<>();
        List<Generation> generations = aiResponse.getResults();
        if (generations != null) {
            for (int i = 0; i < generations.size(); i++) {
                Generation gen = generations.get(i);
                AssistantMessage output = gen.getOutput();

                ChatResponse.Message msg = new ChatResponse.Message();
                msg.setRole("assistant");
                msg.setContent(output != null ? output.getText() : "");

                ChatResponse.Choice choice = new ChatResponse.Choice();
                choice.setIndex(i);
                choice.setMessage(msg);
                if (gen.getMetadata() != null) {
                    Object finishReason = gen.getMetadata().get("finishReason");
                    if (finishReason != null) choice.setFinishReason(finishReason.toString());
                }
                choices.add(choice);
            }
        }
        response.setChoices(choices);

        // 用量信息
        if (aiResponse.getMetadata() != null && aiResponse.getMetadata().getUsage() != null) {
            var usage = aiResponse.getMetadata().getUsage();
            ChatResponse.Usage u = new ChatResponse.Usage();
            u.setPromptTokens(usage.getPromptTokens() != null
                    ? usage.getPromptTokens().intValue() : 0);
            u.setCompletionTokens(usage.getCompletionTokens() != null
                    ? usage.getCompletionTokens().intValue() : 0);
            u.setTotalTokens(usage.getTotalTokens() != null
                    ? usage.getTotalTokens().intValue() : 0);
            response.setUsage(u);
        }

        return response;
    }

    /**
     * 将 Spring AI 流式 chunk 转换为 OpenAI SSE data JSON 字符串
     * 格式：data: {"choices":[{"delta":{"content":"..."}}]}
     */
    public static String toSseChunk(org.springframework.ai.chat.model.ChatResponse chunk) {
        if (chunk == null) return "";
        try {
            List<Generation> generations = chunk.getResults();
            if (generations == null || generations.isEmpty()) return "";

            List<java.util.Map<String, Object>> choices = new ArrayList<>();
            for (int i = 0; i < generations.size(); i++) {
                AssistantMessage output = generations.get(i).getOutput();
                String text = output != null ? output.getText() : "";
                java.util.Map<String, Object> delta = java.util.Map.of("content", text);
                choices.add(java.util.Map.of("index", i, "delta", delta));
            }
            java.util.Map<String, Object> body = java.util.Map.of("choices", choices);
            return MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            log.warn("Failed to serialize SSE chunk", e);
            return "";
        }
    }
}

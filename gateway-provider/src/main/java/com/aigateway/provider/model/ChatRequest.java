package com.aigateway.provider.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一聊天请求 - 支持保留所有未知字段
 * 支持多模态内容（文本 + 图片）
 */
@Data
public class ChatRequest {

    private String model;
    private List<Message> messages;
    private Boolean stream = false;
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    private List<String> stop;
    private String user;

    /**
     * 保存所有未定义的字段，防止消息丢失
     */
    @JsonAnySetter
    private Map<String, Object> additionalProperties = new HashMap<>();

    @Data
    public static class Message {
        private String role;

        /**
         * 内容：可以是字符串（纯文本）或 List<ContentPart>（多模态）
         * 使用 Object 类型兼容两种格式，序列化时自动处理
         */
        private Object content;

        /** 便捷方法：获取纯文本内容 */
        public String getContentAsString() {
            if (content instanceof String s) return s;
            return content != null ? content.toString() : null;
        }

        /** 判断是否为多模态内容 */
        public boolean isMultiModal() {
            return content instanceof List;
        }
    }

    /**
     * 多模态内容块
     * 支持 text、image_url、input_text、input_image 等类型
     */
    @Data
    public static class ContentPart {
        /** 内容类型: text / image_url / input_text / input_image */
        private String type;

        /** 文本内容（type=text 或 type=input_text）*/
        private String text;

        /** 图片 URL（type=image_url）*/
        @JsonProperty("image_url")
        private ImageUrl imageUrl;

        /** 火山引擎格式图片 URL（type=input_image）*/
        @JsonProperty("image_url")
        private String volcanoImageUrl;

        @Data
        public static class ImageUrl {
            private String url;
            private String detail; // low / high / auto
        }
    }
}

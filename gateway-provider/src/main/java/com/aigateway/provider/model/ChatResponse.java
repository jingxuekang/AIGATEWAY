package com.aigateway.provider.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ChatResponse {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
    
    public Long getCreated() { return created; }
    public void setCreated(Long created) { this.created = created; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
    
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }

    public static class Choice {
        private Integer index;
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;
        private Message delta;
        
        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
        
        public Message getDelta() { return delta; }
        public void setDelta(Message delta) { this.delta = delta; }
    }

    public static class Message {
        private String role;
        private String content;
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
        @JsonProperty("cache_creation_tokens")
        private Integer cacheCreationTokens;
        @JsonProperty("cache_read_tokens")
        private Integer cacheReadTokens;
        
        public Integer getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
        
        public Integer getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
        
        public Integer getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
        
        public Integer getCacheCreationTokens() { return cacheCreationTokens; }
        public void setCacheCreationTokens(Integer cacheCreationTokens) { this.cacheCreationTokens = cacheCreationTokens; }
        
        public Integer getCacheReadTokens() { return cacheReadTokens; }
        public void setCacheReadTokens(Integer cacheReadTokens) { this.cacheReadTokens = cacheReadTokens; }
    }
}

package com.aigateway.provider.model;

import java.util.List;

/**
 * 统一聊天请求
 */
public class ChatRequest {
    
    private String model;
    private List<Message> messages;
    private Boolean stream = false;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Double frequencyPenalty;
    private Double presencePenalty;
    private List<String> stop;
    private String user;
    
    // Getters and Setters
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    
    public Boolean getStream() {
        return stream;
    }
    
    public void setStream(Boolean stream) {
        this.stream = stream;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Double getTopP() {
        return topP;
    }
    
    public void setTopP(Double topP) {
        this.topP = topP;
    }
    
    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }
    
    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }
    
    public Double getPresencePenalty() {
        return presencePenalty;
    }
    
    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }
    
    public List<String> getStop() {
        return stop;
    }
    
    public void setStop(List<String> stop) {
        this.stop = stop;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public static class Message {
        private String role;
        private String content;
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}

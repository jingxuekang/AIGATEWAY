package com.aigateway.provider.adapter;

import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class OpenAIProvider implements ModelProvider {

    @Value("${provider.openai.api-url:https://api.openai.com/v1}")
    private String apiUrl;

    @Value("${provider.openai.api-key:}")
    private String apiKey;

    @Value("${provider.openai.timeout:60000}")
    private long timeout;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() { return "openai"; }

    @Override
    public boolean supports(String model) {
        if (model == null) return false;
        return model.startsWith("gpt-") || model.startsWith("o1-") || model.startsWith("o3-") || model.startsWith("o4-");
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) { return mockResponse(request.getModel()); }
        try {
            String json = webClient().post().uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(buildBody(request, false))
                    .retrieve().bodyToMono(String.class).timeout(Duration.ofMillis(timeout)).block();
            return objectMapper.readValue(json, ChatResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("OpenAI call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) { return mockStream(request.getModel()); }
        return webClient().post().uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(buildBody(request, true)).retrieve().bodyToFlux(String.class)
                .timeout(Duration.ofMillis(timeout))
                .map(line -> line.isBlank() ? "" : "data: " + line + "\n\n")
                .filter(line -> !line.isBlank());
    }

    private WebClient webClient() {
        return WebClient.builder().baseUrl(apiUrl).defaultHeader("Authorization", "Bearer " + apiKey).build();
    }

    private Map<String, Object> buildBody(ChatRequest req, boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", req.getModel()); body.put("stream", stream);
        List<Map<String, Object>> msgs = new ArrayList<>();
        if (req.getMessages() != null) {
            for (ChatRequest.Message m : req.getMessages()) { msgs.add(Map.of("role", m.getRole(), "content", m.getContent())); }
        }
        body.put("messages", msgs);
        if (req.getTemperature() != null) body.put("temperature", req.getTemperature());
        if (req.getMaxTokens() != null) body.put("max_tokens", req.getMaxTokens());
        if (req.getTopP() != null) body.put("top_p", req.getTopP());
        if (req.getStop() != null) body.put("stop", req.getStop());
        return body;
    }

    private ChatResponse mockResponse(String model) {
        ChatResponse r = new ChatResponse(); r.setId("mock-" + System.currentTimeMillis()); r.setModel(model);
        r.setObject("chat.completion"); r.setCreated(System.currentTimeMillis() / 1000);
        ChatResponse.Choice c = new ChatResponse.Choice(); c.setIndex(0); c.setFinishReason("stop");
        ChatResponse.Message m = new ChatResponse.Message(); m.setRole("assistant"); m.setContent("[Mock] OpenAI key not configured");
        c.setMessage(m); r.setChoices(Collections.singletonList(c));
        ChatResponse.Usage u = new ChatResponse.Usage(); u.setPromptTokens(0); u.setCompletionTokens(0); u.setTotalTokens(0);
        r.setUsage(u); return r;
    }

    private Flux<String> mockStream(String model) {
        long ts = System.currentTimeMillis() / 1000;
        return Flux.just(
            "data: {\"id\":\"mock\",\"object\":\"chat.completion.chunk\",\"created\":" + ts + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\"}}]}\n\n",
            "data: {\"id\":\"mock\",\"object\":\"chat.completion.chunk\",\"created\":" + ts + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"[Mock] OpenAI key not configured\"}}]}\n\n",
            "data: {\"id\":\"mock\",\"object\":\"chat.completion.chunk\",\"created\":" + ts + ",\"model\":\"" + model + "\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n",
            "data: [DONE]\n\n"
        );
    }
}

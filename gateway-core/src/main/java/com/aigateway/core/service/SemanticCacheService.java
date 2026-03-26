package com.aigateway.core.service;

import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SemanticCacheService {

    @Value("${gateway.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${gateway.cache.max-size:1000}")
    private int maxSize;

    @Value("${gateway.cache.ttl-seconds:3600}")
    private long ttlSeconds;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public ChatResponse get(ChatRequest request) {
        if (!cacheEnabled) return null;
        if (Boolean.TRUE.equals(request.getStream())) return null;
        // 多模态请求（content 含图片）不走缓存
        if (isMultiModal(request)) return null;
        String key = buildCacheKey(request);
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expireAt()) {
            cache.remove(key);
            return null;
        }
        log.info("[Cache] HIT: model={}", request.getModel());
        return entry.response();
    }

    public void put(ChatRequest request, ChatResponse response) {
        if (!cacheEnabled || response == null) return;
        if (Boolean.TRUE.equals(request.getStream())) return;
        // 多模态请求不缓存
        if (isMultiModal(request)) return;
        if (cache.size() >= maxSize) evict();
        String key = buildCacheKey(request);
        long expireAt = System.currentTimeMillis() + ttlSeconds * 1000;
        cache.put(key, new CacheEntry(response, expireAt));
        log.debug("[Cache] PUT: model={}", request.getModel());
    }

    public void clear() {
        cache.clear();
    }

    public Map<String, Object> getStats() {
        long now = System.currentTimeMillis();
        long active = cache.values().stream().filter(e -> e.expireAt() > now).count();
        return Map.of(
            "totalEntries", cache.size(),
            "activeEntries", active,
            "maxSize", maxSize,
            "ttlSeconds", ttlSeconds,
            "enabled", cacheEnabled
        );
    }

    private String buildCacheKey(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getModel()).append("|");
        if (request.getTemperature() != null) sb.append("t:").append(request.getTemperature()).append("|");
        if (request.getMessages() != null) {
            for (ChatRequest.Message msg : request.getMessages()) {
                sb.append(msg.getRole()).append(":").append(msg.getContent()).append("|");
            }
        }
        return "cache:" + sha256(sb.toString());
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }

    private boolean isMultiModal(ChatRequest request) {
        if (request.getMessages() == null) return false;
        for (ChatRequest.Message msg : request.getMessages()) {
            if (msg.getContent() instanceof java.util.List) return true;
        }
        return false;
    }

    private void evict() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> e.getValue().expireAt() <= now);
        if (cache.size() >= maxSize) {
            int toRemove = maxSize / 10;
            List<String> toDelete = cache.keySet().stream().limit(toRemove).toList();
            toDelete.forEach(cache::remove);
        }
    }

    private record CacheEntry(ChatResponse response, long expireAt) {}
}

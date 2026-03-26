package com.aigateway.core.service;

import com.aigateway.core.client.AdminClient;
import com.aigateway.core.factory.ProviderFactory;
import com.aigateway.provider.adapter.ModelProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 渠道 Provider 注册表
 * 启动时 + 每 30 秒从 admin 拉取启用的渠道，动态注册/注销 Provider。
 * 采用差量更新：按 channelId 缓存已有 Provider，避免每次全量重建 HTTP 连接池。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelProviderRegistry {

    private final AdminClient adminClient;
    private final SmartRoutingService smartRoutingService;
    private final ProviderFactory providerFactory;

    /**
     * channelId -> ModelProvider 缓存，避免重复构建 Spring AI ChatModel（含连接池）
     */
    private final ConcurrentHashMap<Long, ModelProvider> providerCache = new ConcurrentHashMap<>();

    /**
     * admin 调用失败退避：连续失败次数越多，cooldown 越长（指数退避，上限 5 分钟）
     */
    private volatile long cooldownUntilMs = 0L;
    private volatile int consecutiveAdminFailures = 0;

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedDelay = 30000)  // 每 30 秒刷新一次
    public void refresh() {
        long now = System.currentTimeMillis();
        if (now < cooldownUntilMs) {
            return;
        }

        try {
            List<Map<String, Object>> channels = adminClient.listEnabledChannels();
            if (channels == null) {
                log.warn("[ChannelRegistry] Refresh skipped due to admin call failure; keeping previous providers");
                consecutiveAdminFailures++;
                cooldownUntilMs = now + calcCooldownMs(consecutiveAdminFailures);
                return;
            }

            consecutiveAdminFailures = 0;
            cooldownUntilMs = 0L;

            if (channels.isEmpty()) {
                log.debug("[ChannelRegistry] No enabled channels from admin");
                providerCache.clear();
                smartRoutingService.updateDynamicProviders(List.of());
                return;
            }

            // 本次启用的 channelId 集合
            Set<Long> activeIds = new HashSet<>();
            int added = 0, reused = 0;

            List<ModelProvider> providers = new ArrayList<>();
            for (Map<String, Object> ch : channels) {
                Long channelId = toLong(ch.get("id"));
                if (channelId == null || channelId == 0L) continue;
                activeIds.add(channelId);

                // 差量：已有 Provider 直接复用，不重建
                ModelProvider existing = providerCache.get(channelId);
                if (existing != null) {
                    providers.add(existing);
                    reused++;
                } else {
                    try {
                        ModelProvider p = providerFactory.create(ch);
                        providerCache.put(channelId, p);
                        providers.add(p);
                        added++;
                        log.info("[ChannelRegistry] Added new provider: channelId={}, name={}",
                                channelId, ch.get("name"));
                    } catch (Exception e) {
                        log.warn("[ChannelRegistry] Failed to create provider for channel: {}", ch.get("name"), e);
                    }
                }
            }

            // 移除已删除/禁用的渠道
            Set<Long> removedIds = new HashSet<>(providerCache.keySet());
            removedIds.removeAll(activeIds);
            removedIds.forEach(id -> {
                ModelProvider removed = providerCache.remove(id);
                if (removed != null) {
                    log.info("[ChannelRegistry] Removed provider: channelId={}, name={}",
                            id, removed.getProviderName());
                }
            });

            smartRoutingService.updateDynamicProviders(providers);
            log.info("[ChannelRegistry] Refreshed: total={}, added={}, reused={}, removed={}",
                    providers.size(), added, reused, removedIds.size());

        } catch (Exception e) {
            consecutiveAdminFailures++;
            cooldownUntilMs = System.currentTimeMillis() + calcCooldownMs(consecutiveAdminFailures);
            log.error("[ChannelRegistry] Refresh failed (cooldown applied)", e);
        }
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private static long calcCooldownMs(int consecutiveFailures) {
        if (consecutiveFailures <= 0) return 0L;
        long base = 45000L;
        int exp = Math.min(consecutiveFailures - 1, 3);
        long cooldown = base * (1L << exp);
        return Math.min(300000L, cooldown);
    }
}

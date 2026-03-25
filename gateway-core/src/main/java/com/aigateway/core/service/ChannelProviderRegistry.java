package com.aigateway.core.service;

import com.aigateway.core.client.AdminClient;
import com.aigateway.core.factory.ProviderFactory;
import com.aigateway.provider.adapter.ModelProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 渠道 Provider 注册表
 * 启动时 + 每 30 秒从 admin 拉取启用的渠道，动态注册/注销 Provider。
 * SmartRoutingService 持有的是同一个 List 引用（CopyOnWriteArrayList），更新后立即生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelProviderRegistry {

    private final AdminClient adminClient;
    private final SmartRoutingService smartRoutingService;
    private final ProviderFactory providerFactory;

    /**
     * admin 调用失败退避：避免每 30 秒都撞超时/失败，减少日志噪音和无效刷新。
     * - refresh() 里如果还在 cooldown 内，直接 return
     * - 连续失败次数越多，cooldown 越长（指数退避，带上限）
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
                // admin 暂时不可用（超时/网络抖动）时保留旧 provider，避免把路由清空
                log.warn("[ChannelRegistry] Refresh skipped due to admin call failure; keeping previous providers");
                consecutiveAdminFailures++;
                cooldownUntilMs = now + calcCooldownMs(consecutiveAdminFailures);
                return;
            }
            if (channels.isEmpty()) {
                log.debug("[ChannelRegistry] No enabled channels from admin");
                consecutiveAdminFailures = 0;
                cooldownUntilMs = 0L;
                smartRoutingService.updateDynamicProviders(List.of());
                return;
            }

            consecutiveAdminFailures = 0;
            cooldownUntilMs = 0L;

            List<ModelProvider> providers = new ArrayList<>();
            for (Map<String, Object> ch : channels) {
                try {
                    // 使用 ProviderFactory 按协议类型创建对应实现，不再直接 new DynamicChannelProvider
                    providers.add(providerFactory.create(ch));
                } catch (Exception e) {
                    log.warn("[ChannelRegistry] Failed to create provider for channel: {}", ch.get("name"), e);
                }
            }
            smartRoutingService.updateDynamicProviders(providers);
            log.info("[ChannelRegistry] Refreshed {} dynamic channel providers", providers.size());
        } catch (Exception e) {
            consecutiveAdminFailures++;
            cooldownUntilMs = now + calcCooldownMs(consecutiveAdminFailures);
            log.error("[ChannelRegistry] Refresh failed (cooldown applied)", e);
        }
    }

    private static long calcCooldownMs(int consecutiveFailures) {
        if (consecutiveFailures <= 0) return 0L;
        long base = 45000L;
        int exp = Math.min(consecutiveFailures - 1, 3);
        long cooldown = base * (1L << exp);
        return Math.min(300000L, cooldown);
    }
}

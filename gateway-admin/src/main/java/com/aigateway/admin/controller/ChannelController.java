package com.aigateway.admin.controller;

import com.aigateway.admin.entity.Channel;
import com.aigateway.admin.entity.UsageLogRecord;
import com.aigateway.admin.service.ChannelService;
import com.aigateway.admin.service.UsageLogRecordService;
import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Channel Management", description = "渠道管理")
@RestController
@RequestMapping("/api/admin/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final UsageLogRecordService usageLogRecordService;

    @Value("${admin.internal-secret:aigateway-internal-2026}")
    private String internalSecret;

    private static void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (role == null || !"admin".equals(role.toString()))
            throw new BusinessException(403, "Forbidden: admin role required");
    }

    // ====== 内部接口（供 gateway-core 拉取渠道配置）======

    @Operation(summary = "内部接口：获取启用渠道列表")
    @GetMapping("/internal")
    public Result<List<Channel>> listChannelsInternal(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        if (!internalSecret.equals(secret)) throw new BusinessException(403, "Invalid internal secret");
        List<Channel> enabled = channelService.listChannels().stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .toList();
        return Result.success(enabled);
    }

    // ====== 管理接口 ======

    @Operation(summary = "获取渠道列表")
    @GetMapping
    public Result<List<Channel>> listChannels() {
        List<Channel> channels = channelService.listChannels();
        // Mask apiKey for frontend display
        channels.forEach(c -> c.setApiKey(maskKey(c.getApiKey())));
        return Result.success(channels);
    }

    @Operation(summary = "创建渠道（仅 admin）")
    @PostMapping
    public Result<Channel> createChannel(HttpServletRequest request, @RequestBody Channel channel) {
        requireAdmin(request);
        channel.setId(null);
        channel.setUsedQuota(0L);
        channel.setCreateTime(LocalDateTime.now());
        channel.setUpdateTime(LocalDateTime.now());
        // 逻辑删除字段使用 @TableLogic：insert 时如果 deleted=null，select 会被自动过滤掉（deleted=0）
        if (channel.getDeleted() == null) channel.setDeleted(0);
        if (channel.getStatus() == null) channel.setStatus(1);
        channelService.save(channel);
        channel.setApiKey(maskKey(channel.getApiKey()));
        return Result.success(channel);
    }

    @Operation(summary = "更新渠道（仅 admin）")
    @PutMapping("/{id}")
    public Result<Boolean> updateChannel(HttpServletRequest request,
                                           @PathVariable Long id, @RequestBody Channel channel) {
        requireAdmin(request);
        // apiKey 为空则保留原值
        if (channel.getApiKey() == null || channel.getApiKey().isBlank()) {
            Channel old = channelService.getById(id);
            if (old != null) channel.setApiKey(old.getApiKey());
        }
        channel.setId(id);
        channel.setUpdateTime(LocalDateTime.now());
        return Result.success(channelService.updateById(channel));
    }

    @Operation(summary = "启用/禁用渠道（仅 admin）")
    @PutMapping("/{id}/status")
    public Result<Boolean> toggleStatus(HttpServletRequest request,
                                         @PathVariable Long id, @RequestBody Map<String, Integer> body) {
        requireAdmin(request);
        Channel ch = new Channel();
        ch.setId(id);
        ch.setStatus(body.get("status"));
        ch.setUpdateTime(LocalDateTime.now());
        return Result.success(channelService.updateById(ch));
    }

    @Operation(summary = "删除渠道（仅 admin）")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteChannel(HttpServletRequest request, @PathVariable Long id) {
        requireAdmin(request);
        Channel ch = new Channel();
        ch.setId(id);
        ch.setDeleted(1);
        ch.setUpdateTime(LocalDateTime.now());
        return Result.success(channelService.updateById(ch));
    }

    @Operation(summary = "管理员 Playground 对话（后端代理，不需要网关 Key）")
    @PostMapping("/chat")
    public Result<Map<String, Object>> adminChat(HttpServletRequest request,
                                                     @RequestBody Map<String, Object> body) {
        requireAdmin(request);
        String model = (String) body.get("model");
        Object messages = body.get("messages");
        if (model == null || messages == null) throw new BusinessException(400, "model and messages are required");

        // Find an enabled channel that supports the model
            Channel channel = channelService.listChannels().stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .filter(c -> supportsModel(c, model))
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "No enabled channel found for model: " + model));

        long start = System.currentTimeMillis();
        try {
            boolean isAzure = "azure".equalsIgnoreCase(channel.getProvider());
            boolean isVolcanoMultiModal = model != null && (
                    model.startsWith("doubao-") ||
                            model.startsWith("ep-") ||
                            model.startsWith("volcano-")
            );
            WebClient.Builder builder = WebClient.builder()
                    .baseUrl(channel.getBaseUrl())
                    .defaultHeader("Content-Type", "application/json");
            if (isAzure) {
                builder.defaultHeader("api-key", channel.getApiKey());
            } else {
                builder.defaultHeader("Authorization", "Bearer " + channel.getApiKey());
            }
            // Volcano/豆包多模态：上游仍走 /chat/completions（更 OpenAI 兼容）
            String uri;
            if (isVolcanoMultiModal) {
                uri = isAzure ? "/chat/completions?api-version=2025-01-01-preview" : "/chat/completions";
            } else {
                uri = isAzure ? "/chat/completions?api-version=2025-01-01-preview" : "/chat/completions";
            }
            String reqBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                    isVolcanoMultiModal
                            ? java.util.Map.of("model", model, "messages", messages)
                            : java.util.Map.of("model", model, "messages", messages));
            String resp = builder.build().post().uri(uri)
                    .bodyValue(reqBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(60));
            long latency = System.currentTimeMillis() - start;
            @SuppressWarnings("unchecked")
            Map<String, Object> respMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(resp, Map.class);

            // Record usage log with admin identity
            int promptTokens = 0, completionTokens = 0, totalTokens = 0;
            if (respMap.containsKey("usage")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> usage = (Map<String, Object>) respMap.get("usage");
                if (usage.get("prompt_tokens") instanceof Number n) promptTokens = n.intValue();
                if (usage.get("completion_tokens") instanceof Number n) completionTokens = n.intValue();
                if (usage.get("total_tokens") instanceof Number n) totalTokens = n.intValue();
            }
            String userId   = request.getAttribute("userId")   != null ? request.getAttribute("userId").toString()   : null;
            String username = request.getAttribute("username") != null ? request.getAttribute("username").toString() : "admin";
            UsageLogRecord logRecord = new UsageLogRecord();
            logRecord.setTimestamp(LocalDateTime.now());
            logRecord.setRequestId(java.util.UUID.randomUUID().toString().replace("-", ""));
            logRecord.setUserId(userId);
            logRecord.setModel(model);
            logRecord.setProvider("admin-playground:" + channel.getName());
            logRecord.setStatus("success");
            logRecord.setLatencyMs(latency);
            logRecord.setPromptTokens(promptTokens);
            logRecord.setCompletionTokens(completionTokens);
            logRecord.setTotalTokens(totalTokens);
            usageLogRecordService.saveAndIndex(logRecord);

            log.info("[AdminChat] user={} model={} channel={} latency={}ms prompt={} completion={}",
                    username, model, channel.getName(), latency, promptTokens, completionTokens);
            return Result.success(respMap);
        } catch (Exception e) {
            String userId   = request.getAttribute("userId")   != null ? request.getAttribute("userId").toString()   : null;
            String username = request.getAttribute("username") != null ? request.getAttribute("username").toString() : "admin";
            long latency = System.currentTimeMillis() - start;
            UsageLogRecord errRecord = new UsageLogRecord();
            errRecord.setTimestamp(LocalDateTime.now());
            errRecord.setRequestId(java.util.UUID.randomUUID().toString().replace("-", ""));
            errRecord.setUserId(userId);
            errRecord.setModel(model);
            errRecord.setProvider("admin-playground:" + channel.getName());
            errRecord.setStatus("error");
            errRecord.setLatencyMs(latency);
            errRecord.setErrorMessage(e.getMessage());
            usageLogRecordService.saveAndIndex(errRecord);
            log.warn("[AdminChat] user={} failed: model={} channel={} error={}", username, model, channel.getName(), e.getMessage());
            throw new BusinessException(502, "Upstream call failed: " + e.getMessage());
        }
    }

    @Operation(summary = "测试渠道连通性（仅 admin）")
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> testChannel(HttpServletRequest request, @PathVariable Long id) {
        requireAdmin(request);
        Channel ch = channelService.getById(id);
        if (ch == null) throw new BusinessException(404, "渠道不存在");
        long start = System.currentTimeMillis();
        try {
            boolean isAzure = "azure".equalsIgnoreCase(ch.getProvider());
            WebClient.Builder builder = WebClient.builder()
                    .baseUrl(ch.getBaseUrl())
                    .defaultHeader("Content-Type", "application/json");
            if (isAzure) {
                builder.defaultHeader("api-key", ch.getApiKey());
            } else {
                builder.defaultHeader("Authorization", "Bearer " + ch.getApiKey());
            }
            WebClient client = builder.build();

            // 优先用渠道配置的第一个模型，Azure 用 deployment 名称兜底
            String testModel;
            if (ch.getModels() != null && !ch.getModels().isBlank()) {
                testModel = ch.getModels().split(",")[0].trim();
            } else {
                testModel = switch (ch.getProvider() == null ? "" : ch.getProvider().toLowerCase()) {
                    case "deepseek" -> "deepseek-chat";
                    case "volcano"  -> "doubao-pro-32k";
                    case "anthropic"-> "claude-3-haiku-20240307";
                    case "qwen"     -> "qwen-turbo";
                    case "glm"      -> "glm-4.6v";
                    default         -> "gpt-4o-mini";
                };
            }

            // Azure 需要附加 api-version 参数
            String uri = isAzure ? "/chat/completions?api-version=2025-01-01-preview" : "/chat/completions";
            // 智谱 GLM：连通性测试时强制用 glm-4.6v + 多模态 content，避免 models 字段类型/内容异常导致分支误判
            boolean isGlmProvider = ch.getProvider() != null && ch.getProvider().equalsIgnoreCase("glm");
            if (isGlmProvider) testModel = "glm-4.6v";

            String contentJson = isGlmProvider
                    ? "[{\"type\":\"text\",\"text\":\"hi\"}]"
                    : "\"hi\"";

            String body = String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":%s}],\"max_tokens\":5}",
                testModel, contentJson);
            log.warn("[ChannelTest] id={} provider={} testModel={} contentJson={}",
                    id, ch.getProvider(), testModel, contentJson);

            client.post().uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(15));
            long latency = System.currentTimeMillis() - start;
            log.info("[ChannelTest] id={} provider={} model={} latency={}ms", id, ch.getProvider(), testModel, latency);
            return Result.success(Map.of("success", true, "latencyMs", latency, "model", testModel));
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException w) {
            long latency = System.currentTimeMillis() - start;
            String respBody = w.getResponseBodyAsString();
            log.warn("[ChannelTest] id={} provider={} failed: status={} body={}", id, ch.getProvider(), w.getStatusCode(), respBody);
            return Result.success(Map.of(
                    "success", false,
                    "latencyMs", latency,
                    "error", "HTTP " + w.getStatusCode() + ": " + respBody
            ));
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("[ChannelTest] id={} failed: {}", id, e.getMessage());
            return Result.success(Map.of("success", false, "latencyMs", latency, "error", e.getMessage()));
        }
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 8) return "****";
        return key.substring(0, 8) + "****" + key.substring(key.length() - 4);
    }

    /**
     * 渠道是否支持指定 model
     * 优先使用 channel.models 精确匹配；如果 models 为空/格式异常，则按 provider 前缀兜底
     * （避免 models 字段存储格式不一致导致无法路由）。
     */
    private boolean supportsModel(Channel c, String model) {
        if (c == null || model == null) return false;
        String rawModels = c.getModels();
        if (rawModels != null && !rawModels.isBlank()) {
            // 兼容换行/分号/竖线等分隔符；最终都变成逗号再 split
            String normalized = rawModels
                    .replace('\n', ',')
                    .replace('\r', ',')
                    .replace(';', ',')
                    .replace('|', ',')
                    .trim();
            for (String m : normalized.split(",")) {
                if (model.trim().equalsIgnoreCase(m.trim())) return true;
            }
        }

        // 兜底：按 provider 前缀规则匹配（与 AbstractOpenAiCompatibleProvider.matchByProvider() 保持一致）
        String provider = c.getProvider();
        if (provider == null) return false;
        String p = provider.trim().toLowerCase();
        return switch (p) {
            case "glm"      -> model.trim().toLowerCase().startsWith("glm-");
            case "deepseek" -> model.trim().toLowerCase().startsWith("deepseek-");
            case "openai"    -> model.trim().toLowerCase().startsWith("gpt-")
                                || model.trim().toLowerCase().startsWith("o1-")
                                || model.trim().toLowerCase().startsWith("o3-")
                                || model.trim().toLowerCase().startsWith("o4-");
            case "azure"     -> model.trim().toLowerCase().startsWith("azure-")
                                || model.trim().toLowerCase().startsWith("gpt-");
            case "volcano"   -> model.trim().toLowerCase().startsWith("doubao-")
                                || model.trim().toLowerCase().startsWith("ep-");
            case "anthropic" -> model.trim().toLowerCase().startsWith("claude-");
            case "qwen"      -> model.trim().toLowerCase().startsWith("qwen-");
            case "moonshot"  -> model.trim().toLowerCase().startsWith("moonshot-");
            case "minimax"   -> model.trim().toLowerCase().startsWith("abab")
                                || model.trim().toLowerCase().startsWith("minimax-");
            case "baichuan"  -> model.trim().startsWith("baichuan");
            case "hunyuan"   -> model.trim().toLowerCase().startsWith("hunyuan-");
            case "yi"         -> model.trim().toLowerCase().startsWith("yi-");
            default          -> false;
        };
    }

}

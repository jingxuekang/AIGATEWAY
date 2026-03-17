package com.aigateway.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * LLM 调用自定义 Prometheus 指标
 *
 * 暴露的指标：
 *   llm_requests_total{model, provider, status}    - 调用总次数（Counter）
 *   llm_tokens_total{model, provider, type}        - token 消耗（Counter，type=prompt/completion/total）
 *   llm_latency_seconds{model, provider, status}   - 端到端延迟直方图（Timer）
 */
@Slf4j
@Component
public class LlmMetrics {

    private final MeterRegistry meterRegistry;

    public LlmMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录一次 LLM 调用
     *
     * @param model            模型名称
     * @param provider         供应商名称
     * @param status           success / error
     * @param latencyMs        端到端延迟（毫秒）
     * @param promptTokens     输入 token 数
     * @param completionTokens 输出 token 数
     */
    public void record(String model, String provider, String status,
                       long latencyMs, int promptTokens, int completionTokens) {
        String safeModel    = model    != null ? model    : "unknown";
        String safeProvider = provider != null ? provider : "unknown";
        String safeStatus   = status   != null ? status   : "unknown";

        // 1. 请求计数
        Counter.builder("llm_requests_total")
                .description("Total number of LLM requests")
                .tag("model", safeModel)
                .tag("provider", safeProvider)
                .tag("status", safeStatus)
                .register(meterRegistry)
                .increment();

        // 2. Token 消耗计数
        if (promptTokens > 0) {
            Counter.builder("llm_tokens_total")
                    .description("Total tokens consumed")
                    .tag("model", safeModel)
                    .tag("provider", safeProvider)
                    .tag("type", "prompt")
                    .register(meterRegistry)
                    .increment(promptTokens);
        }
        if (completionTokens > 0) {
            Counter.builder("llm_tokens_total")
                    .description("Total tokens consumed")
                    .tag("model", safeModel)
                    .tag("provider", safeProvider)
                    .tag("type", "completion")
                    .register(meterRegistry)
                    .increment(completionTokens);
        }
        if (promptTokens + completionTokens > 0) {
            Counter.builder("llm_tokens_total")
                    .description("Total tokens consumed")
                    .tag("model", safeModel)
                    .tag("provider", safeProvider)
                    .tag("type", "total")
                    .register(meterRegistry)
                    .increment((double) promptTokens + completionTokens);
        }

        // 3. 延迟直方图
        Timer.builder("llm_latency_seconds")
                .description("LLM request end-to-end latency")
                .tag("model", safeModel)
                .tag("provider", safeProvider)
                .tag("status", safeStatus)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }
}

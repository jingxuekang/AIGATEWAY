package com.aigateway.core.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流拦截器 - 三层限流：
 *   1. 全局限流（global）
 *   2. per-key 限流（per-key）
 *   3. per-IP 限流（per-ip），防止 DDoS / 未认证滥用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterRegistry rateLimiterRegistry;

    @Value("${gateway.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${gateway.rate-limit.ip-limit-enabled:true}")
    private boolean ipLimitEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // OPTIONS 请求（CORS 预检）：直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        if (!rateLimitEnabled) return true;

        // 1. 全局限流
        RateLimiter globalLimiter = rateLimiterRegistry.rateLimiter("global");
        if (!globalLimiter.acquirePermission()) {
            log.warn("[RateLimit] Global limit exceeded: path={}, ip={}",
                request.getRequestURI(), getClientIp(request));
            sendError(response, "Global rate limit exceeded. Please try again later.");
            return false;
        }

        // 2. per-key 限流
        String apiKey = extractApiKey(request);
        if (apiKey != null && !apiKey.isBlank()) {
            String keyId = "key-" + (apiKey.length() > 16 ? apiKey.substring(0, 16) : apiKey);
            RateLimiter keyLimiter = rateLimiterRegistry.rateLimiter(
                keyId,
                rateLimiterRegistry.getConfiguration("per-key")
                    .orElse(rateLimiterRegistry.getDefaultConfig()));
            if (!keyLimiter.acquirePermission()) {
                log.warn("[RateLimit] Per-key limit exceeded: key={}, path={}",
                    keyId, request.getRequestURI());
                sendError(response, "Rate limit exceeded for this API key.");
                return false;
            }
        }

        // 3. per-IP 限流
        if (ipLimitEnabled) {
            String clientIp = getClientIp(request);
            String ipLimiterId = "ip-" + clientIp.replace(":", "_").replace(".", "_");
            RateLimiter ipLimiter = rateLimiterRegistry.rateLimiter(
                ipLimiterId,
                rateLimiterRegistry.getConfiguration("per-ip")
                    .orElseGet(() -> rateLimiterRegistry.getConfiguration("per-key")
                        .orElse(rateLimiterRegistry.getDefaultConfig())));
            if (!ipLimiter.acquirePermission()) {
                log.warn("[RateLimit] Per-IP limit exceeded: ip={}, path={}",
                    clientIp, request.getRequestURI());
                sendError(response, "Too many requests from your IP. Please slow down.");
                return false;
            }
        }

        return true;
    }

    private String extractApiKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7).trim();
        String xKey = request.getHeader("x-api-key");
        if (xKey != null && !xKey.isBlank()) return xKey.trim();
        return null;
    }

    @Value("${gateway.rate-limit.trust-proxy:false}")
    private boolean trustProxy;

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        // Only trust proxy headers if explicitly configured and request comes from a trusted source
        if (trustProxy) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) return realIp.trim();
        }
        return remoteAddr;
    }

    private void sendError(HttpServletResponse response, String message) throws Exception {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", "60");
        response.getWriter().write(
            "{\"code\":429,\"message\":\"" + message + "\",\"data\":null}");
    }
}

package com.aigateway.core.interceptor;

import com.aigateway.common.constant.CommonConstants;
import com.aigateway.common.context.ApiKeyContext;
import com.aigateway.common.model.ApiKeyInfo;
import com.aigateway.common.util.TraceIdUtil;
import com.aigateway.core.client.AdminClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AdminClient adminClient;

    // TODO: 后续可引入 Redis 依赖，在此处加缓存，减少 admin 调用次数

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 设置 Trace ID
        String traceId = request.getHeader(CommonConstants.HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) traceId = TraceIdUtil.generateTraceId();
        TraceIdUtil.setTraceId(traceId);
        response.setHeader(CommonConstants.HEADER_TRACE_ID, traceId);

        // TODO: 鉴权暂时禁用，放行所有请求（测试阶段）
        // 恢复时删除下面这行，取消注释后面的验证逻辑
        return true;

        /*
        String apiKey = extractApiKey(request);
        if (apiKey == null || apiKey.isBlank()) {
            sendUnauthorized(response, "Missing API Key. Use Authorization: Bearer sk-xxx");
            return false;
        }

        ApiKeyInfo keyInfo = adminClient.validateApiKey(apiKey);
        if (keyInfo == null) {
            sendUnauthorized(response, "Invalid or disabled API Key");
            return false;
        }

        if (keyInfo.getExpireTime() != null && LocalDateTime.now().isAfter(keyInfo.getExpireTime())) {
            sendUnauthorized(response, "API Key has expired");
            return false;
        }

        if (keyInfo.getTotalQuota() != null && keyInfo.getTotalQuota() > 0
                && keyInfo.getUsedQuota() != null && keyInfo.getUsedQuota() >= keyInfo.getTotalQuota()) {
            sendUnauthorized(response, "API Key quota exhausted");
            return false;
        }

        ApiKeyContext.set(keyInfo);
        log.debug("Auth passed: keyId={}, tenant={}, traceId={}",
                keyInfo.getId(), keyInfo.getTenantId(), traceId);
        return true;
        */
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TraceIdUtil.clearTraceId();
        ApiKeyContext.clear();
    }

    private String extractApiKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7).trim();
        String xApiKey = request.getHeader("x-api-key");
        if (xApiKey != null && !xApiKey.isBlank()) return xApiKey.trim();
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}"
        );
    }
}

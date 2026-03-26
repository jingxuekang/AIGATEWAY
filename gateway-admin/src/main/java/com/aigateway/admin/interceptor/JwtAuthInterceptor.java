package com.aigateway.admin.interceptor;

import com.aigateway.admin.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 管理后台认证拦截器
 *
 * 规则优先级：
 *   1. 内部接口（gateway-core 调用）→ 校验 X-Internal-Secret
 *   2. 公开白名单（登录等）→ 直接放行
 *   3. 其他接口 → 校验 JWT
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("${admin.internal-secret:}")
    private String internalSecret;

    /**
     * 公开白名单：无需任何认证直接放行
     */
    private static final String[] WHITE_LIST = {
            "/api/admin/auth/login",
            "/api/admin/auth/logout",
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator",
            "/doc.html",
            "/webjars",
            "/v3/api-docs",
            "/swagger"
    };

    /**
     * 内部接口规则：[method, pathPrefix, optionalSuffix]
     * 匹配条件：method 相同 AND path.startsWith(prefix) AND (无 suffix OR path.contains(suffix))
     * 精确限定 suffix 防止 POST /api/admin/keys/{id}/chat 被误判为内部接口
     */
    private static final String[][] INTERNAL_RULES = {
            {"POST", "/api/admin/logs"},                               // gateway-core 上报日志
            {"GET",  "/api/admin/keys/validate"},                      // gateway-core 验证 API Key
            {"POST", "/api/admin/keys/", "/deduct-quota"},            // gateway-core 扣减配额
            {"GET",  "/api/admin/models"},                             // gateway-core 获取模型列表
            {"GET",  "/api/admin/channels/internal"},                  // gateway-core 拉取启用渠道列表
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path   = request.getRequestURI();
        String method = request.getMethod().toUpperCase();

        // 0. OPTIONS 请求（CORS 预检）：直接放行
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // 1. 内部接口：匹配 method + pathPrefix [+ suffix]，校验 X-Internal-Secret
        for (String[] rule : INTERNAL_RULES) {
            boolean pathMatches = path.startsWith(rule[1])
                    && (rule.length < 3 || path.contains(rule[2]));
            if (rule[0].equals(method) && pathMatches) {
                String secret = request.getHeader("X-Internal-Secret");
                if (internalSecret != null && !internalSecret.isBlank() && internalSecret.equals(secret)) {
                    return true; // 合法内部调用，放行
                }
                // secret 不对：如果携带了合法 JWT 也放行（前端调用同一接口）
                String token = extractToken(request);
                if (token != null && jwtUtil.validateToken(token)) {
                    injectUser(request, token);
                    return true;
                }
                log.warn("[Auth] Rejected path={} method={}: no valid secret or JWT", path, method);
                sendError(response, 403, "Forbidden: authentication required");
                return false;
            }
        }

        // 2. 公开白名单：直接放行
        for (String w : WHITE_LIST) {
            if (path.startsWith(w)) return true;
        }

        // 3. 其他接口：校验 JWT
        String token = extractToken(request);
        if (token == null) {
            sendError(response, 401, "Missing or invalid Authorization header");
            return false;
        }
        if (!jwtUtil.validateToken(token)) {
            sendError(response, 401, "Token expired or invalid");
            return false;
        }
        injectUser(request, token);
        return true;
    }

    private void injectUser(HttpServletRequest request, String token) {
        request.setAttribute("userId",   jwtUtil.getUserId(token));
        request.setAttribute("username", jwtUtil.getUsername(token));
        request.setAttribute("role",     jwtUtil.getRole(token));
    }

    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7).trim();
        return null;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(Map.of("code", status, "message", message, "data", ""))
        );
    }
}

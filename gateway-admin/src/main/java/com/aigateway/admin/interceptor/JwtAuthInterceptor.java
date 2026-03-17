package com.aigateway.admin.interceptor;

import com.aigateway.admin.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 管理后台 JWT 认证拦截器
 * 白名单：/api/auth/login、/api/auth/refresh、/actuator/**、/doc.html、/v3/api-docs/**
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    private static final String[] WHITE_LIST = {
            "/api/admin/auth/login", "/api/admin/auth/logout",
            "/api/auth/login", "/api/auth/refresh",
            "/api/admin/logs",           // gateway-core 上报日志，无需 JWT
            "/api/admin/keys/validate",  // gateway-core 验证 API Key，无需 JWT
            "/api/admin/keys/",          // deduct-quota 等内部接口
            "/api/admin/models",         // gateway-core 获取模型列表，无需 JWT
            "/actuator", "/doc.html", "/webjars", "/v3/api-docs", "/swagger"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 白名单直接放行
        for (String w : WHITE_LIST) {
            if (path.startsWith(w)) return true;
        }

        // 提取 Token
        String token = extractToken(request);
        if (token == null) {
            sendError(response, 401, "Missing or invalid Authorization header");
            return false;
        }

        // 验证 Token
        if (!jwtUtil.validateToken(token)) {
            sendError(response, 401, "Token expired or invalid");
            return false;
        }

        // 将用户信息注入请求属性，供 Controller 使用
        request.setAttribute("userId", jwtUtil.getUserId(token));
        request.setAttribute("username", jwtUtil.getUsername(token));
        request.setAttribute("role", jwtUtil.getRole(token));
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
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

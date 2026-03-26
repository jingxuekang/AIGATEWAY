package com.aigateway.admin.config;

import com.aigateway.admin.interceptor.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    /**
     * 允许跨域的来源列表，从配置文件读取，支持动态配置。
     * 生产环境应配置为实际域名，如: https://admin.example.com
     * 默认只允许本地开发环境，避免 wildcard + credentials 安全漏洞。
     */
    @Value("${admin.cors.allowed-origins:http://localhost:3000,http://localhost:3001,http://localhost:5173}")
    private List<String> allowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/admin/auth/login",
                        "/api/admin/auth/logout"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With",
                        "Accept", "X-Internal-Secret", "X-Trace-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

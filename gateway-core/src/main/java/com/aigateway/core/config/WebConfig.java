package com.aigateway.core.config;

import com.aigateway.core.interceptor.AuthInterceptor;
import com.aigateway.core.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 允许跨域的来源列表，从配置文件读取，支持动态配置
     * 生产环境应配置为实际域名，如: https://admin.example.com
     */
    @Value("${gateway.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private List<String> allowedOrigins;

    @Value("${gateway.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private List<String> allowedMethods;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 限流拦截器（先于鉴权执行）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/v1/**")
                .order(1);

        // 2. 鉴权拦截器
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/doc.html",
                        "/webjars/**",
                        "/actuator/**",
                        "/v1/status/**"   // 网关状态监控接口，无需 API Key 鉴权
                )
                .order(2);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods(allowedMethods.toArray(new String[0]))
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With",
                        "Accept", "X-Internal-Secret", "X-Trace-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

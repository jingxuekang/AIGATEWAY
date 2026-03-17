package com.aigateway.core.config;

import com.aigateway.core.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 当前 AuthInterceptor.preHandle 已设置为放行所有请求（测试模式）
        // 需要开启鉴权时，还原 AuthInterceptor.preHandle 中的验证逻辑即可
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/doc.html",
                        "/webjars/**",
                        "/actuator/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:3001")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

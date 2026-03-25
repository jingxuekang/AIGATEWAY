package com.aigateway.admin.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置
 * 
 * MyBatis-Plus 3.5.9+ 移除了 PaginationInnerInterceptor，
 * 分页功能已内置到 MybatisPlusInterceptor，无需单独添加。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 插件（分页等功能已内置）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return new MybatisPlusInterceptor();
    }
}

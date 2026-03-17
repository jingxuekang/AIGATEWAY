package com.aigateway.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Admin 启动类
 */
@SpringBootApplication(scanBasePackages = "com.aigateway")
@MapperScan("com.aigateway.admin.mapper")
@EnableAsync
public class AdminApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}

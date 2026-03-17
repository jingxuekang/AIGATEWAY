package com.aigateway.admin.config;

import com.aigateway.admin.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化默认管理员账号（仅当 admin 不存在时创建）。
 */
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final AdminUserService adminUserService;

    @Value("${aigateway.admin.init-username:admin}")
    private String initUsername;

    @Value("${aigateway.admin.init-password:admin123456}")
    private String initPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserService.findByUsername(initUsername) != null) return;
        adminUserService.createAdmin(initUsername, initPassword, "admin");
        System.out.println("[AI-Gateway] Created default admin user: username=" + initUsername
                + ", password=" + initPassword + " (please change it ASAP)");
    }
}


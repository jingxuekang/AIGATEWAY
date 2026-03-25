package com.aigateway.admin.config;

import com.aigateway.admin.service.AdminUserService;
import com.aigateway.admin.entity.AdminUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始化默认管理员账号（仅当 admin 不存在时创建）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final AdminUserService adminUserService;
    private final PasswordEncoder passwordEncoder;

    @Value("${aigateway.admin.init-username:admin}")
    private String initUsername;

    // 不提供默认口令，避免把明文默认口令提交/推送
    @Value("${aigateway.admin.init-password:}")
    private String initPassword;

    @Value("${aigateway.admin.reset-password:false}")
    private boolean resetPassword;

    @Override
    public void run(ApplicationArguments args) {
        String pwd = initPassword == null ? "" : initPassword.trim();
        AdminUser existing = adminUserService.findByUsername(initUsername);
        if (existing == null) {
            if (pwd.isBlank()) {
                log.warn("[AI-Gateway] Skip default admin creation: missing property aigateway.admin.init-password");
                return;
            }
            adminUserService.createAdmin(initUsername, pwd, "admin");
            log.warn("[AI-Gateway] Created default admin user: username={}, password=****** (please change it ASAP)",
                    initUsername);
        } else if (resetPassword) {
            if (pwd.isBlank()) {
                log.warn("[AI-Gateway] Skip admin password reset: missing property aigateway.admin.init-password");
                return;
            }
            // 强制重置密码（设置 aigateway.admin.reset-password=true 触发）
            existing.setPassword(passwordEncoder.encode(pwd));
            adminUserService.updateById(existing);
            log.warn("[AI-Gateway] Reset admin password for username={}", initUsername);
        }
    }
}

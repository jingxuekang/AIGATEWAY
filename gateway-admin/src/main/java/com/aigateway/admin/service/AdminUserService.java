package com.aigateway.admin.service;

import com.aigateway.admin.entity.AdminUser;
import com.aigateway.admin.mapper.AdminUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdminUserService extends ServiceImpl<AdminUserMapper, AdminUser> {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AdminUser findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username)
                .eq(AdminUser::getDeleted, 0));
    }

    public AdminUser createAdmin(String username, String password, String role) {
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role != null ? role : "admin");
        user.setStatus(1);
        save(user);
        log.info("Admin user created: {}", username);
        return user;
    }
}

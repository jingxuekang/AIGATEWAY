package com.aigateway.admin.controller;

import com.aigateway.admin.dto.LoginRequest;
import com.aigateway.admin.dto.LoginResponse;
import com.aigateway.admin.entity.AdminUser;
import com.aigateway.admin.entity.User;
import com.aigateway.admin.service.AdminUserService;
import com.aigateway.admin.service.UserService;
import com.aigateway.admin.util.JwtUtil;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Auth", description = "登录认证")
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AdminUserService adminUserService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "登录", description = "用户名密码登录，返回 JWT Token。先查 admin_user 表，再查 user 表")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        // 1. 先查 admin_user 表
        AdminUser adminUser = adminUserService.findByUsername(request.getUsername());
        if (adminUser != null) {
            if (!passwordEncoder.matches(request.getPassword(), adminUser.getPassword())) {
                return Result.error(401, "用户名或密码错误");
            }
            if (adminUser.getStatus() != null && adminUser.getStatus() != 1) {
                return Result.error(403, "账号已被禁用");
            }
            String token = jwtUtil.generateToken(adminUser.getId(), adminUser.getUsername(), adminUser.getRole());
            LoginResponse resp = new LoginResponse(token, adminUser.getId(), adminUser.getUsername(), adminUser.getRole(), 86400000L);
            log.info("Admin user logged in: username={}, role={}", adminUser.getUsername(), adminUser.getRole());
            return Result.success(resp);
        }

        // 2. 再查普通用户表
        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            return Result.error(403, "账号已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginResponse resp = new LoginResponse(token, user.getId(), user.getUsername(), user.getRole(), 86400000L);
        log.info("User logged in: username={}, role={}", user.getUsername(), user.getRole());
        return Result.success(resp);
    }

    @Operation(summary = "获取当前用户信息（业务用户返回 user，管理员返回 admin）")
    @GetMapping("/me")
    public Result<Map<String, Object>> me(jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Object roleAttr = request.getAttribute("role");
        String role = roleAttr != null ? roleAttr.toString() : "user";

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("role", role);

        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
            payload.put("user", user);
            return Result.success(payload);
        }
        AdminUser admin = adminUserService.getById(userId);
        if (admin != null) {
            admin.setPassword(null);
            payload.put("admin", admin);
            return Result.success(payload);
        }
        return Result.success(payload);
    }

    @Operation(summary = "退出登录", description = "前端用于清理 token（服务端无状态，直接返回成功）")
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        return Result.success(true);
    }
}

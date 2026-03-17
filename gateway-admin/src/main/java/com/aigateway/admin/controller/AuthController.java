package com.aigateway.admin.controller;

import com.aigateway.admin.dto.LoginRequest;
import com.aigateway.admin.dto.LoginResponse;
import com.aigateway.admin.entity.AdminUser;
import com.aigateway.admin.service.AdminUserService;
import com.aigateway.admin.util.JwtUtil;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "登录认证")
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminUserService adminUserService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "登录", description = "用户名密码登录，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        AdminUser user = adminUserService.findByUsername(request.getUsername());
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
        long expiresIn = 86400000L;
        LoginResponse resp = new LoginResponse(token, user.getId(), user.getUsername(), user.getRole(), expiresIn);
        log.info("User logged in: {}", user.getUsername());
        return Result.success(resp);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<AdminUser> me(jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AdminUser user = adminUserService.getById(userId);
        if (user != null) user.setPassword(null);
        return Result.success(user);
    }

    @Operation(summary = "退出登录", description = "前端用于清理 token（服务端无状态，直接返回成功）")
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        return Result.success(true);
    }
}

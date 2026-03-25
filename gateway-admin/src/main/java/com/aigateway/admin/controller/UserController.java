package com.aigateway.admin.controller;

import com.aigateway.admin.entity.User;
import com.aigateway.admin.service.UserService;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Tag(name = "User Management", description = "用户管理")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (role == null || !"admin".equals(role.toString())) {
            // 使用 403 语义，避免前端误认为是参数错误
            throw new com.aigateway.common.exception.BusinessException(403, "Forbidden: admin role required");
        }
    }
    
    @Operation(summary = "获取用户列表")
    @GetMapping
    public Result<List<User>> listUsers(HttpServletRequest request) {
        requireAdmin(request);
        List<User> users = userService.listUsers();
        // 不返回密码
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }
    
    @Operation(summary = "创建用户")
    @PostMapping
    public Result<User> createUser(HttpServletRequest request, @RequestBody User user) {
        requireAdmin(request);
        User created = userService.createUser(user);
        return Result.success(created);
    }
    
    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<Boolean> updateUser(HttpServletRequest request, @PathVariable Long id, @RequestBody User user) {
        requireAdmin(request);
        return Result.success(userService.updateUser(id, user));
    }
    
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteUser(HttpServletRequest request, @PathVariable Long id) {
        requireAdmin(request);
        return Result.success(userService.softDelete(id));
    }
}

package com.aigateway.admin.controller;

import com.aigateway.admin.entity.User;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "User Management", description = "用户管理")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {
    
    @Operation(summary = "获取用户列表")
    @GetMapping
    public Result<List<Map<String, Object>>> listUsers() {
        // 返回模拟数据
        List<Map<String, Object>> users = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", i);
            user.put("username", "user" + i);
            user.put("email", "user" + i + "@example.com");
            user.put("role", i == 1 ? "admin" : "user");
            user.put("status", 1);
            user.put("quota", 1000000L);
            user.put("usedQuota", i * 10000L);
            user.put("createTime", LocalDateTime.now().minusDays(i));
            users.add(user);
        }
        return Result.success(users);
    }
    
    @Operation(summary = "创建用户")
    @PostMapping
    public Result<User> createUser(@RequestBody User user) {
        user.setId(System.currentTimeMillis());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return Result.success(user);
    }
    
    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<Boolean> updateUser(@PathVariable Long id, @RequestBody User user) {
        return Result.success(true);
    }
    
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteUser(@PathVariable Long id) {
        return Result.success(true);
    }
}

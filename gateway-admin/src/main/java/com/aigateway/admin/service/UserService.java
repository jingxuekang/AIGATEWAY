package com.aigateway.admin.service;

import com.aigateway.admin.entity.User;
import com.aigateway.admin.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;

    public List<User> listUsers() {
        // @TableLogic 会自动加 deleted=0，无需手动过滤
        return list(new LambdaQueryWrapper<User>()
                .orderByAsc(User::getId));
    }

    public User findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    public User createUser(User user) {
        user.setId(null);
        // 检查用户名重复
        if (user.getUsername() != null) {
            User existing = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, user.getUsername()));
            if (existing != null) throw new IllegalArgumentException("用户名已存在：" + user.getUsername());
        }
        // 检查邮箱重复
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            User existing = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, user.getEmail()));
            if (existing != null) throw new IllegalArgumentException("邮箱已被注册：" + user.getEmail());
        }
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        if (user.getRole() == null || user.getRole().isBlank()) user.setRole("user");
        if (user.getStatus() == null) user.setStatus(1);
        if (user.getQuota() == null) user.setQuota(0L);
        if (user.getUsedQuota() == null) user.setUsedQuota(0L);
        if (user.getDeleted() == null) user.setDeleted(0);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        save(user);
        user.setPassword(null);
        return user;
    }

    public boolean updateUser(Long id, User patch) {
        User existing = getById(id);
        if (existing == null) return false;
        if (patch.getUsername() != null) existing.setUsername(patch.getUsername());
        if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
        if (patch.getRole() != null) existing.setRole(patch.getRole());
        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        if (patch.getQuota() != null) existing.setQuota(patch.getQuota());
        if (patch.getUsedQuota() != null) existing.setUsedQuota(patch.getUsedQuota());
        if (patch.getPassword() != null && !patch.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(patch.getPassword()));
        }
        existing.setUpdateTime(LocalDateTime.now());
        return updateById(existing);
    }

    public boolean softDelete(Long id) {
        User existing = getById(id);
        if (existing == null) return false;
        existing.setDeleted(1);
        existing.setUpdateTime(LocalDateTime.now());
        return updateById(existing);
    }
}

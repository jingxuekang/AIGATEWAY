package com.aigateway.admin.service;

import com.aigateway.admin.entity.ApiKey;
import com.aigateway.admin.mapper.ApiKeyMapper;
import com.aigateway.common.constant.CommonConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key 服务
 */
@Slf4j
@Service
public class ApiKeyService extends ServiceImpl<ApiKeyMapper, ApiKey> {
    
    public String generateKey() {
        return CommonConstants.API_KEY_PREFIX + IdUtil.fastSimpleUUID();
    }
    
    public ApiKey createKey(ApiKey apiKey) {
        String keyValue = generateKey();
        apiKey.setKeyValue(keyValue);
        apiKey.setStatus(CommonConstants.KEY_STATUS_ENABLED);
        apiKey.setUsedQuota(0L);
        save(apiKey);
        log.info("Created API Key: {}", keyValue);
        return apiKey;
    }
    
    public List<ApiKey> listByUser(Long userId) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getUserId, userId);
        wrapper.orderByDesc(ApiKey::getCreateTime);
        return list(wrapper);
    }
    
    public boolean revokeKey(Long keyId) {
        ApiKey apiKey = getById(keyId);
        if (apiKey != null) {
            apiKey.setStatus(CommonConstants.KEY_STATUS_DISABLED);
            boolean result = updateById(apiKey);
            log.info("Revoked API Key: {}", keyId);
            return result;
        }
        return false;
    }

    public ApiKey validateKey(String keyValue) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getKeyValue, keyValue);
        wrapper.eq(ApiKey::getStatus, CommonConstants.KEY_STATUS_ENABLED);
        return getOne(wrapper);
    }

    /**
     * 扣减配额（原子性 UPDATE，防止并发超用）
     * total_quota = 0 表示不限配额
     * @param keyId  API Key ID
     * @param tokens 本次消耗的 token 数
     * @return true=扣减成功，false=配额不足或 Key 不存在
     */
    /**
     * 修改 Key 显示名称（用户只能改自己的；admin 可改任意）
     */
    public void updateKeyName(Long keyId, String newName, Long operatorUserId, boolean isAdmin) {
        if (newName == null || newName.isBlank()) {
            throw new com.aigateway.common.exception.BusinessException(400, "keyName is required");
        }
        String trimmed = newName.trim();
        if (trimmed.length() > 255) {
            throw new com.aigateway.common.exception.BusinessException(400, "keyName too long (max 255)");
        }
        ApiKey key = getById(keyId);
        if (key == null) {
            throw new com.aigateway.common.exception.BusinessException(404, "Key not found");
        }
        if (!isAdmin && (operatorUserId == null || !operatorUserId.equals(key.getUserId()))) {
            throw new com.aigateway.common.exception.BusinessException(403, "Access denied");
        }
        key.setKeyName(trimmed);
        key.setUpdateTime(LocalDateTime.now());
        updateById(key);
        log.info("Updated API Key name: id={}, name={}", keyId, trimmed);
    }

    public boolean deductQuota(Long keyId, long tokens) {
        if (tokens <= 0) return true;
        // 单条原子 SQL：total_quota=0 不限额，否则检查 used_quota+tokens<=total_quota
        int affected = baseMapper.incrementUsedQuota(keyId, tokens);
        if (affected == 0) {
            log.warn("Quota insufficient or key not found: keyId={}, tokens={}", keyId, tokens);
            return false;
        }
        log.debug("Deducted quota: keyId={}, tokens={}", keyId, tokens);
        return true;
    }
}

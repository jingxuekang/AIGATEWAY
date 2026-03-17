package com.aigateway.admin.service;

import com.aigateway.admin.entity.ApiKey;
import com.aigateway.admin.mapper.ApiKeyMapper;
import com.aigateway.common.constant.CommonConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     * 扣减配额（原子性 UPDATE，防止超用）
     * @param keyId  API Key ID
     * @param tokens 本次消耗的 token 数
     * @return true=扣减成功，false=配额不足或 Key 不存在
     */
    public boolean deductQuota(Long keyId, long tokens) {
        if (tokens <= 0) return true;
        ApiKey apiKey = getById(keyId);
        if (apiKey == null) return false;
        // totalQuota == 0 表示不限配额
        if (apiKey.getTotalQuota() != null && apiKey.getTotalQuota() > 0) {
            long remaining = apiKey.getTotalQuota() - (apiKey.getUsedQuota() == null ? 0 : apiKey.getUsedQuota());
            if (remaining < tokens) {
                log.warn("Quota insufficient: keyId={}, remaining={}, requested={}", keyId, remaining, tokens);
                return false;
            }
        }
        // 用 UPDATE 语句原子性递增，避免并发问题
        baseMapper.incrementUsedQuota(keyId, tokens);
        log.debug("Deducted quota: keyId={}, tokens={}", keyId, tokens);
        return true;
    }
}

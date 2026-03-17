package com.aigateway.admin.service;

import com.aigateway.admin.entity.ModelSubscription;
import com.aigateway.admin.mapper.ModelSubscriptionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型订阅服务
 */
@Service
public class ModelSubscriptionService extends ServiceImpl<ModelSubscriptionMapper, ModelSubscription> {

    public void subscribe(Long modelId, Long userId, String tenantId, String appId) {
        LambdaQueryWrapper<ModelSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelSubscription::getModelId, modelId)
                .eq(ModelSubscription::getUserId, userId)
                .eq(ModelSubscription::getTenantId, tenantId)
                .eq(ModelSubscription::getAppId, appId);
        ModelSubscription existing = getOne(wrapper);
        if (existing == null) {
            ModelSubscription sub = new ModelSubscription();
            sub.setModelId(modelId);
            sub.setUserId(userId);
            sub.setTenantId(tenantId);
            sub.setAppId(appId);
            sub.setStatus(1);
            sub.setCreateTime(LocalDateTime.now());
            save(sub);
        } else {
            existing.setStatus(1);
            existing.setUpdateTime(LocalDateTime.now());
            updateById(existing);
        }
    }

    public void unsubscribe(Long modelId, Long userId, String tenantId, String appId) {
        LambdaQueryWrapper<ModelSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelSubscription::getModelId, modelId)
                .eq(ModelSubscription::getUserId, userId)
                .eq(ModelSubscription::getTenantId, tenantId)
                .eq(ModelSubscription::getAppId, appId);
        ModelSubscription existing = getOne(wrapper);
        if (existing != null) {
            existing.setStatus(0);
            existing.setUpdateTime(LocalDateTime.now());
            updateById(existing);
        }
    }

    public List<ModelSubscription> listUserSubscriptions(Long userId, String tenantId, String appId) {
        LambdaQueryWrapper<ModelSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelSubscription::getUserId, userId)
                .eq(ModelSubscription::getTenantId, tenantId)
                .eq(ModelSubscription::getAppId, appId)
                .eq(ModelSubscription::getStatus, 1);
        return list(wrapper);
    }
}


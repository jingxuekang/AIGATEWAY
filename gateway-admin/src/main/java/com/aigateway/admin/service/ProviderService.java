package com.aigateway.admin.service;

import com.aigateway.admin.entity.Provider;
import com.aigateway.admin.mapper.ProviderMapper;
import com.aigateway.admin.util.ApiKeyEncryptor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderService extends ServiceImpl<ProviderMapper, Provider> {

    @Autowired
    private ApiKeyEncryptor apiKeyEncryptor;

    public List<Provider> listAll() {
        List<Provider> providers = list(new LambdaQueryWrapper<Provider>()
                .eq(Provider::getDeleted, 0)
                .orderByAsc(Provider::getId));
        // 前端只返回脱敏的 apiKey，不暴露真实密钥
        providers.forEach(p -> p.setApiKey(maskApiKey(p.getApiKey())));
        return providers;
    }

    /**
     * 获取解密后的真实 apiKey（仅供后端内部使用，不对外暴露）
     */
    public Provider getWithDecryptedKey(Long id) {
        Provider provider = getById(id);
        if (provider == null) return null;
        decryptApiKey(provider);
        return provider;
    }

    private static String maskApiKey(String encrypted) {
        if (encrypted == null || encrypted.length() < 8) return "****";
        // 只显示前4位，其余用 * 代替
        return encrypted.substring(0, 4) + "****" + encrypted.substring(encrypted.length() - 4);
    }
    
    @Override
    public boolean save(Provider entity) {
        // 加密 API Key 后存储
        if (entity.getApiKey() != null) {
            entity.setApiKey(apiKeyEncryptor.encrypt(entity.getApiKey()));
        }
        return super.save(entity);
    }
    
    @Override
    public boolean updateById(Provider entity) {
        // 如果更新了 API Key，需要加密
        Provider existing = getById(entity.getId());
        if (existing != null && entity.getApiKey() != null && 
            !entity.getApiKey().equals(existing.getApiKey())) {
            entity.setApiKey(apiKeyEncryptor.encrypt(entity.getApiKey()));
        }
        return super.updateById(entity);
    }
    
    /**
     * 解密 API Key（用于显示）
     */
    private void decryptApiKey(Provider provider) {
        if (provider.getApiKey() != null) {
            try {
                provider.setApiKey(apiKeyEncryptor.decrypt(provider.getApiKey()));
            } catch (Exception e) {
                // 解密失败时返回脱敏显示
                provider.setApiKey("****-****-****-****");
            }
        }
    }
}


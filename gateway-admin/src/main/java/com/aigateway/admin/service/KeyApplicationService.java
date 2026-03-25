package com.aigateway.admin.service;

import com.aigateway.admin.entity.ApiKey;
import com.aigateway.admin.entity.KeyApplication;
import com.aigateway.admin.entity.Model;
import com.aigateway.admin.mapper.KeyApplicationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key 申请审批服务
 * 状态流转：0=待审批 PENDING -> 1=已批准 APPROVED / 2=已拒绝 REJECTED
 * 审批通过后自动生成 API Key
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyApplicationService extends ServiceImpl<KeyApplicationMapper, KeyApplication> {

    private static final int STATUS_PENDING  = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private final ApiKeyService apiKeyService;
    private final ModelService modelService;
    private final ModelSubscriptionService modelSubscriptionService;

    /** 提交申请 */
    public KeyApplication submitApplication(KeyApplication application) {
        if (application.getTenantId() == null || application.getTenantId().isBlank()) {
            application.setTenantId(generateTenantId());
        }
        application.setApprovalStatus(STATUS_PENDING);
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        save(application);
        log.info("Key application submitted: id={}, user={}, keyName={}, tenantId={}",
                application.getId(), application.getUserId(), application.getKeyName(), application.getTenantId());
        return application;
    }

    /** 租户 ID：申请时自动生成，审批通过后写入 API Key，调用日志「租户」列可展示 */
    public static String generateTenantId() {
        return "tnt-" + IdUtil.fastSimpleUUID();
    }

    /** 审批通过 - 自动生成 API Key，支持配置配额和有效期 */
    @Transactional
    public ApiKey approve(Long applicationId, Long approverId, String comment,
                          Long totalQuota, String expireTimeStr) {
        return approve(applicationId, approverId, comment, totalQuota, expireTimeStr, null);
    }

    /**
     * 审批通过 - 支持：
     * 1) 不传 targetKeyId：生成新 Key
     * 2) 传 targetKeyId：把 allowedModels 做并集追加到已有 Key，并同步订阅新增模型
     */
    @Transactional
    public ApiKey approve(Long applicationId, Long approverId, String comment,
                          Long totalQuota, String expireTimeStr, Long targetKeyId) {
        KeyApplication application = getById(applicationId);
        if (application == null)
            throw new RuntimeException("Application not found: " + applicationId);
        if (application.getApprovalStatus() != STATUS_PENDING)
            throw new RuntimeException("Application is not in PENDING status");

        // 更新申请状态
        application.setApprovalStatus(STATUS_APPROVED);
        application.setApproverId(approverId);
        application.setApprovalComment(comment);
        application.setApprovalTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        updateById(application);

        // targetKeyId 不为空：合并到已有 Key
        if (targetKeyId != null) {
            ApiKey targetKey = apiKeyService.getById(targetKeyId);
            if (targetKey == null) throw new RuntimeException("Target Key not found: " + targetKeyId);
            if (!application.getUserId().equals(targetKey.getUserId())) {
                throw new RuntimeException("Target Key user mismatch, application.userId=" + application.getUserId()
                        + ", targetKey.userId=" + targetKey.getUserId());
            }

            String existingAllowed = targetKey.getAllowedModels();
            java.util.Set<String> merged = new java.util.LinkedHashSet<>();
            if (existingAllowed != null && !existingAllowed.isBlank()) {
                for (String s : existingAllowed.split(",")) {
                    String t = s.trim();
                    if (!t.isEmpty()) merged.add(t);
                }
            }

            String incomingAllowed = application.getAllowedModels();
            java.util.List<String> incomingList = new java.util.ArrayList<>();
            if (incomingAllowed != null && !incomingAllowed.isBlank()) {
                for (String s : incomingAllowed.split(",")) {
                    String t = s.trim();
                    if (!t.isEmpty()) incomingList.add(t);
                }
            }

            boolean changedModels = false;
            for (String modelName : incomingList) {
                if (merged.add(modelName)) changedModels = true;
            }

            // 1) 覆盖更新 totalQuota/expireTime（不管 models 有没有新增）
            targetKey.setTotalQuota(totalQuota != null ? totalQuota : 0L);
            if (expireTimeStr == null || expireTimeStr.isBlank()) {
                // 管理员输入空则清空有效期（覆盖更新）
                targetKey.setExpireTime(null);
            } else {
                try {
                    targetKey.setExpireTime(LocalDateTime.parse(expireTimeStr.replace(" ", "T")));
                } catch (Exception e) {
                    log.warn("Invalid expireTime format: {}", expireTimeStr);
                }
            }

            // 2) 合并 allowedModels（仅当确实新增了模型才更新 allowedModels 字段）
            if (changedModels) {
                String mergedStr = String.join(",", merged);
                targetKey.setAllowedModels(mergedStr);
            }

            targetKey.setUpdateTime(LocalDateTime.now());
            apiKeyService.updateById(targetKey);

            // 3) 同步订阅：只订阅新增的模型
            // 无论 changedModels=false 还是 true，incomingList 里只要是“新模型”，都要订阅
            String tenantId = targetKey.getTenantId() != null ? targetKey.getTenantId() : "";
            String appId = targetKey.getAppId() != null ? targetKey.getAppId() : "";
            for (String modelName : incomingList) {
                // 已存在的模型跳过，新模型才订阅
                if (existingAllowedContains(existingAllowed, modelName)) continue;
                Model model = modelService.getByName(modelName);
                if (model != null) {
                    modelSubscriptionService.subscribe(model.getId(), application.getUserId(), tenantId, appId);
                    log.info("Merged & subscribed model={} into keyId={} for user={}",
                            modelName, targetKeyId, application.getUserId());
                } else {
                    log.warn("Model not found for merged auto-subscribe: {}", modelName);
                }
            }

            // 合并场景不需要把真实 keyValue 返回给前端
            targetKey.setKeyValue(null);
            log.info("Application merged approved: id={}, targetKeyId={}, quota={}, expireTime={}",
                    applicationId, targetKeyId, totalQuota, expireTimeStr);
            return targetKey;
        }

        // 生成新 API Key
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyName(application.getKeyName());
        apiKey.setUserId(application.getUserId());
        apiKey.setTenantId(application.getTenantId());
        apiKey.setAppId(application.getAppId());
        apiKey.setAllowedModels(application.getAllowedModels());
        apiKey.setTotalQuota(totalQuota != null && totalQuota > 0 ? totalQuota : 0L);
        if (expireTimeStr != null && !expireTimeStr.isBlank()) {
            try {
                apiKey.setExpireTime(LocalDateTime.parse(expireTimeStr.replace(" ", "T")));
            } catch (Exception e) {
                log.warn("Invalid expireTime format: {}", expireTimeStr);
            }
        }
        apiKey.setCreateTime(LocalDateTime.now());
        apiKey.setUpdateTime(LocalDateTime.now());
        ApiKey created = apiKeyService.createKey(apiKey);

        // 审批通过后自动订阅 allowedModels 中的模型
        String allowedModels = application.getAllowedModels();
        if (allowedModels != null && !allowedModels.isBlank()) {
            String tenantId = application.getTenantId() != null ? application.getTenantId() : "";
            String appId    = application.getAppId()    != null ? application.getAppId()    : "";
            for (String modelName : allowedModels.split(",")) {
                modelName = modelName.trim();
                if (modelName.isEmpty()) continue;
                Model model = modelService.getByName(modelName);
                if (model != null) {
                    modelSubscriptionService.subscribe(model.getId(), application.getUserId(), tenantId, appId);
                    log.info("Auto-subscribed model={} for user={}", modelName, application.getUserId());
                } else {
                    log.warn("Model not found for auto-subscribe: {}", modelName);
                }
            }
        }

        log.info("Application approved: id={}, keyId={}, quota={}, expireTime={}",
                applicationId, created.getId(), totalQuota, expireTimeStr);
        return created;
    }

    private static boolean existingAllowedContains(String existingAllowed, String modelName) {
        if (existingAllowed == null || existingAllowed.isBlank()) return false;
        for (String s : existingAllowed.split(",")) {
            String t = s.trim();
            if (t.equals(modelName)) return true;
        }
        return false;
    }

    /** 审批拒绝 */
    public void reject(Long applicationId, Long approverId, String comment) {
        KeyApplication application = getById(applicationId);
        if (application == null)
            throw new RuntimeException("Application not found: " + applicationId);
        if (application.getApprovalStatus() != STATUS_PENDING)
            throw new RuntimeException("Application is not in PENDING status");
        application.setApprovalStatus(STATUS_REJECTED);
        application.setApproverId(approverId);
        application.setApprovalComment(comment);
        application.setApprovalTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        updateById(application);
        log.info("Application rejected: id={}", applicationId);
    }

    /** 查询待审批列表 */
    public List<KeyApplication> listPending() {
        return list(new LambdaQueryWrapper<KeyApplication>()
                .eq(KeyApplication::getApprovalStatus, STATUS_PENDING)
                .orderByAsc(KeyApplication::getCreateTime));
    }

    /** 查询用户自己的申请 */
    public List<KeyApplication> listByUser(Long userId) {
        return list(new LambdaQueryWrapper<KeyApplication>()
                .eq(KeyApplication::getUserId, userId)
                .orderByDesc(KeyApplication::getCreateTime));
    }
}

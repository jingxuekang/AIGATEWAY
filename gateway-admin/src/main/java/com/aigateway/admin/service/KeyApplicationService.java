package com.aigateway.admin.service;

import com.aigateway.admin.entity.ApiKey;
import com.aigateway.admin.entity.KeyApplication;
import com.aigateway.admin.mapper.KeyApplicationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

    /**
     * 提交申请
     */
    public KeyApplication submitApplication(KeyApplication application) {
        application.setApprovalStatus(STATUS_PENDING);
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        save(application);
        log.info("Key application submitted: id={}, user={}, keyName={}",
                application.getId(), application.getUserId(), application.getKeyName());
        return application;
    }

    /**
     * 审批通过 - 自动生成 API Key
     */
    @Transactional
    public ApiKey approve(Long applicationId, Long approverId, String comment) {
        KeyApplication application = getById(applicationId);
        if (application == null) {
            throw new RuntimeException("Application not found: " + applicationId);
        }
        if (application.getApprovalStatus() != STATUS_PENDING) {
            throw new RuntimeException("Application is not in PENDING status");
        }

        // 更新申请状态
        application.setApprovalStatus(STATUS_APPROVED);
        application.setApproverId(approverId);
        application.setApprovalComment(comment);
        application.setApprovalTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        updateById(application);

        // 自动生成 API Key
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyName(application.getKeyName());
        apiKey.setUserId(application.getUserId());
        apiKey.setTenantId(application.getTenantId());
        apiKey.setAppId(application.getAppId());
        apiKey.setAllowedModels(application.getAllowedModels());
        apiKey.setTotalQuota(0L); // 默认不限配额
        apiKey.setCreateTime(LocalDateTime.now());
        apiKey.setUpdateTime(LocalDateTime.now());
        ApiKey created = apiKeyService.createKey(apiKey);

        log.info("Application approved: id={}, generated keyId={}, keyValue={}",
                applicationId, created.getId(), created.getKeyValue());
        return created;
    }

    /**
     * 审批拒绝
     */
    public void reject(Long applicationId, Long approverId, String comment) {
        KeyApplication application = getById(applicationId);
        if (application == null) {
            throw new RuntimeException("Application not found: " + applicationId);
        }
        if (application.getApprovalStatus() != STATUS_PENDING) {
            throw new RuntimeException("Application is not in PENDING status");
        }
        application.setApprovalStatus(STATUS_REJECTED);
        application.setApproverId(approverId);
        application.setApprovalComment(comment);
        application.setApprovalTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        updateById(application);
        log.info("Application rejected: id={}", applicationId);
    }

    /**
     * 查询待审批列表
     */
    public List<KeyApplication> listPending() {
        return list(new LambdaQueryWrapper<KeyApplication>()
                .eq(KeyApplication::getApprovalStatus, STATUS_PENDING)
                .orderByAsc(KeyApplication::getCreateTime));
    }

    /**
     * 查询用户自己的申请
     */
    public List<KeyApplication> listByUser(Long userId) {
        return list(new LambdaQueryWrapper<KeyApplication>()
                .eq(KeyApplication::getUserId, userId)
                .orderByDesc(KeyApplication::getCreateTime));
    }
}

package com.aigateway.common.constant;

/**
 * 通用常量
 */
public class CommonConstants {
    
    /**
     * API Key 前缀
     */
    public static final String API_KEY_PREFIX = "sk-";
    
    /**
     * 请求头 - API Key
     */
    public static final String HEADER_API_KEY = "Authorization";
    
    /**
     * 请求头 - Trace ID
     */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    
    /**
     * 请求头 - Request ID
     */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    
    /**
     * OpenAI 格式
     */
    public static final String FORMAT_OPENAI = "openai";
    
    /**
     * Claude 格式
     */
    public static final String FORMAT_CLAUDE = "claude";
    
    /**
     * Gemini 格式
     */
    public static final String FORMAT_GEMINI = "gemini";
    
    /**
     * 成功状态
     */
    public static final String STATUS_SUCCESS = "success";
    
    /**
     * 失败状态
     */
    public static final String STATUS_ERROR = "error";
    
    /**
     * API Key 状态 - 启用
     */
    public static final Integer KEY_STATUS_ENABLED = 1;
    
    /**
     * API Key 状态 - 禁用
     */
    public static final Integer KEY_STATUS_DISABLED = 0;
    
    /**
     * 审批状态 - 待审批
     */
    public static final Integer APPROVAL_STATUS_PENDING = 0;
    
    /**
     * 审批状态 - 已通过
     */
    public static final Integer APPROVAL_STATUS_APPROVED = 1;
    
    /**
     * 审批状态 - 已拒绝
     */
    public static final Integer APPROVAL_STATUS_REJECTED = 2;
}

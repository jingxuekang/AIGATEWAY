package com.aigateway.common.context;

import com.aigateway.common.model.ApiKeyInfo;

/**
 * API Key 上下文，保存当前请求使用的 Key 信息
 */
public class ApiKeyContext {

    private static final ThreadLocal<ApiKeyInfo> HOLDER = new ThreadLocal<>();

    public static void set(ApiKeyInfo info) {
        HOLDER.set(info);
    }

    public static ApiKeyInfo get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}


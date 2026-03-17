package com.aigateway.common.util;

import cn.hutool.core.util.IdUtil;
import org.slf4j.MDC;

/**
 * Trace ID 工具类
 */
public class TraceIdUtil {
    
    private static final String TRACE_ID_KEY = "traceId";
    
    /**
     * 生成 Trace ID
     */
    public static String generateTraceId() {
        return IdUtil.fastSimpleUUID();
    }
    
    /**
     * 设置 Trace ID
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }
    
    /**
     * 获取 Trace ID
     */
    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }
    
    /**
     * 清除 Trace ID
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }
}

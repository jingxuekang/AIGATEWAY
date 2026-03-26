package com.aigateway.core.handler;

import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
import com.aigateway.common.util.TraceIdUtil;
import com.aigateway.provider.adapter.ProviderException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一将异常转换为标准 JSON 响应，并设置正确的 HTTP 状态码
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 — 使用异常中的 code 作为 HTTP 状态码
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("[Business] code={}, message={}", e.getCode(), e.getMessage());
        HttpStatus status = resolveHttpStatus(e.getCode());
        return ResponseEntity.status(status)
            .body(Result.error(e.getCode(), e.getMessage()));
    }

    /**
     * Provider 异常 — 上游 API 调用失败
     * 原始上游错误只记录日志，不直接暴露给客户端
     */
    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<Result<Void>> handleProviderException(ProviderException e) {
        log.error("[Provider] errorCode={}, message={}", e.getErrorCode(), e.getMessage());
        if ("CIRCUIT_BREAKER_OPEN".equals(e.getErrorCode())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.error(503, "Service temporarily unavailable. The upstream provider is currently unavailable, please retry after 30 seconds."));
        }
        // 解析上游 HTTP 状态码，给出更具体的用户提示
        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("401") || msg.contains("Unauthorized") || msg.contains("authentication")) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Result.error(502, "Upstream authentication failed. Please check the channel API key configuration."));
            }
            if (msg.contains("403") || msg.contains("Forbidden") || msg.contains("permission")) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Result.error(502, "Upstream access denied. The API key may not have permission for this model."));
            }
            if (msg.contains("404")) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Result.error(502, "Upstream resource not found. Please check the channel baseUrl and deployment configuration."));
            }
            if (msg.contains("429") || msg.contains("rate limit") || msg.contains("RateLimitError")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body(Result.error(429, "Upstream rate limit exceeded. Please retry after a moment."));
            }
            if (msg.contains("context length") || msg.contains("maximum context") || msg.contains("1210")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.error(400, "Request exceeds the model's maximum context length. Please reduce the number of messages or message length."));
            }
            if (msg.contains("timeout") || msg.contains("timed out")) {
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(Result.error(504, "Upstream request timed out. Please retry."));
            }
            if (msg.contains("502") || msg.contains("Bad Gateway")) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Result.error(502, "Upstream gateway error. Please retry."));
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Result.error(502, "Upstream provider call failed. Please retry or contact support."));
    }

    /**
     * Resilience4j 限流异常
     */
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Result<Void>> handleRateLimitException(RequestNotPermitted e) {
        log.warn("[RateLimit] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", "60")
            .body(Result.error(429, "Rate limit exceeded. Please try again later."));
    }

    /**
     * Resilience4j 熔断异常
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Result<Void>> handleCircuitBreakerException(CallNotPermittedException e) {
        log.warn("[CircuitBreaker] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Retry-After", "30")
            .body(Result.error(503, "Service temporarily unavailable. Please retry after 30 seconds."));
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[Validation] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Result.error(400, e.getMessage()));
    }

    /**
     * 客户端已断开连接（常见于 SSE/流式响应中用户主动取消）
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Void> handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
        log.debug("[ClientAbort] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 避免在 openmetrics 等文本响应上下文中继续写 JSON 导致二次异常
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Void> handleNotWritable(HttpMessageNotWritableException e) {
        log.warn("[NotWritable] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 兜底异常处理 — 带 TraceId 方便定位
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
        String traceId = TraceIdUtil.getTraceId();
        log.error("[Unexpected] traceId={}, {}", traceId, e.getMessage(), e);
        String accept = request != null ? request.getHeader("Accept") : null;
        if (accept != null && accept.contains("application/openmetrics-text")) {
            // prometheus/openmetrics 场景返回空 body，避免 JSON converter 冲突
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .build();
        }
        String msg = traceId != null
                ? "Internal server error. TraceId: " + traceId
                : "Internal server error.";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error(500, msg));
    }

    private HttpStatus resolveHttpStatus(Integer code) {
        if (code == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}

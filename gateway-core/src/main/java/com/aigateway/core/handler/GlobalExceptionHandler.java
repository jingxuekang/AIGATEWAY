package com.aigateway.core.handler;

import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
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
     */
    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<Result<Void>> handleProviderException(ProviderException e) {
        log.error("[Provider] errorCode={}, message={}", e.getErrorCode(), e.getMessage());
        if ("CIRCUIT_BREAKER_OPEN".equals(e.getErrorCode())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.error(503, e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Result.error(502, "Upstream provider error: " + e.getMessage()));
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
     * 兜底异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
        log.error("[Unexpected] {}", e.getMessage(), e);
        String accept = request != null ? request.getHeader("Accept") : null;
        if (accept != null && accept.contains("application/openmetrics-text")) {
            // prometheus/openmetrics 场景返回空 body，避免 JSON converter 冲突
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error(500, "Internal server error."));
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

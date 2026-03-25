package com.aigateway.admin.config;

import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("[BusinessException] code={}, message={}", e.getCode(), e.getMessage());
        HttpStatus httpStatus = HttpStatus.resolve(e.getCode());
        if (httpStatus == null) httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(httpStatus).body(Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[ValidationException] {}", message);
        return Result.error(400, "Validation failed: " + message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[HttpMessageNotReadable] {}", e.getMessage());
        return Result.error(400, "Invalid request body: " + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[IllegalArgumentException] {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String msg = e.getMostSpecificCause().getMessage();
        log.warn("[DataIntegrityViolation] {}", msg);
        if (msg != null && msg.contains("uk_email")) {
            return Result.error(400, "该邮箱已被注册，请更换邮箱");
        }
        if (msg != null && msg.contains("uk_username")) {
            return Result.error(400, "该用户名已存在，请更换用户名");
        }
        return Result.error(400, "数据冲突：" + msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("[UnhandledException] {}", e.getMessage(), e);
        return Result.error(500, "Internal server error: " + e.getMessage());
    }
}

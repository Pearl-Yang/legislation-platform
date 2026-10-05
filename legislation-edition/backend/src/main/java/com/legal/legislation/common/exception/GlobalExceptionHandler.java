package com.legal.legislation.common.exception;

import com.legal.legislation.common.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * 设计目标:任何未捕获异常最终都翻译成 Result&lt;?&gt; 风格 JSON,
 * 避免 Spring 默认错误页打断前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException ex, HttpServletRequest req) {
        log.warn("[BizException] {} {} -> code={} msg={}", req.getMethod(), req.getRequestURI(), ex.getCode(), ex.getMessage());
        return ResponseEntity.ok(Result.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining("; "));
        return ResponseEntity.ok(Result.error(40001, msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining("; "));
        return ResponseEntity.ok(Result.error(40001, msg));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.ok(Result.error(40001, "缺少必填参数: " + ex.getParameterName()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(40300, "无权限访问"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(40100, "未登录或登录已过期"));
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Result<Void>> handleJson(JsonProcessingException ex, HttpServletRequest req) {
        // 典型场景:把 SSE 流当成 JSON 解析 — 给前端一个明确提示,而不是裸 500
        log.warn("[JsonParse] {} {} -> {}", req.getMethod(), req.getRequestURI(), ex.getOriginalMessage());
        return ResponseEntity.ok(Result.error(50001,
            "上游返回非 JSON 内容,已切换为本地兜底: " + ex.getOriginalMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("[UnhandledException] {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.ok(Result.error(50000, "系统异常: " + ex.getMessage()));
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + " " + (fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage());
    }
}

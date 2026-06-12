package com.RD.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>统一处理：</p>
 * <ul>
 *   <li>BusinessException（业务异常，4xx/5xx）</li>
 *   <li>MethodArgumentNotValidException（@Valid @RequestBody 校验失败）</li>
 *   <li>BindException（@Valid 表单绑定失败）</li>
 *   <li>AccessDeniedException（Spring Security 拒绝）</li>
 *   <li>AuthenticationException（未认证）</li>
 *   <li>NoHandlerFoundException（404）</li>
 *   <li>其他未捕获异常（500）</li>
 * </ul>
 *
 * <p>所有异常都会被转为 {@link Result} 格式返回，与正常接口响应保持一致</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] {} {} -> code={}, msg={}",
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * @Valid @RequestBody 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[参数校验] {}", msg);
        return Result.badRequest(msg);
    }

    /**
     * @Valid 表单绑定失败
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[参数绑定] {}", msg);
        return Result.badRequest(msg);
    }

    /**
     * Spring Security 拒绝访问
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("[权限拒绝] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.forbidden("无权访问"));
    }

    /**
     * 未认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("[未认证] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.unauthorized("认证失败，请重新登录"));
    }

    /**
     * 404 未找到
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoHandlerFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.notFound("接口不存在: " + e.getRequestURL()));
    }

    /**
     * 未捕获异常（兜底）
     */
    @ExceptionHandler(Throwable.class)
    public Result<Void> handleThrowable(Throwable e, HttpServletRequest request) {
        log.error("[系统异常] {} {}", request.getMethod(), request.getRequestURI(), e);
        return Result.error(500, "系统内部错误: " + e.getMessage());
    }
}

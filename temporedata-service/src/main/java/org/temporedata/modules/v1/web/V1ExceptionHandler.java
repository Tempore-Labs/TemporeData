package org.temporedata.modules.v1.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.contract.ApiResponse;
import org.temporedata.api.contract.ErrorCode;

import javax.servlet.http.HttpServletRequest;

/**
 * v1 exception -> {ApiResponse} mapping, scoped to v1 controllers only
 * (basePackages = each controllers live under org.temporedata.modules.v1),
 * so it never conflicts with the legacy {@code GlobalExceptionHandler}.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "org.temporedata.modules.v1")
public class V1ExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("v1 biz error {} at {}", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.SYS_PARAM, req.getHeader("X-Request-Id"), req.getHeader("X-Trace-Id")).withMsg(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getField() + ": " + f.getDefaultMessage()).orElse("validation error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.SYS_PARAM, req.getHeader("X-Request-Id"), req.getHeader("X-Trace-Id")).withMsg(msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("v1 unexpected error at {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.SYS_INTERNAL, req.getHeader("X-Request-Id"), req.getHeader("X-Trace-Id")));
    }
}
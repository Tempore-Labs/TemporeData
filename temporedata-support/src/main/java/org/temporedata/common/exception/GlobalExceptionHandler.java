package org.temporedata.common.exception;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.exceptions.ZyException;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;

/**
 * Global exception handler: translates exceptions into the unified BaseResponse
 * contract (v2.0 §20).
 *
 * <p>Contract notes:
 * <ul>
 *   <li>BusinessException/ZyException keep HTTP 200 with <code>code != 0</code>
 *       because the frontend Axios interceptor treats <code>code !== 0</code> as
 *       the business-failure signal and reads <code>data.msg</code>.</li>
 *   <li>Framework-level failures (validation / not-found / access-denied / system)
 *       are mapped to the appropriate HTTP status codes.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusiness(BusinessException e) {
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ZyException.class)
    public BaseResponse<Void> handleZy(ZyException e) {
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public BaseResponse<Void> handleAccessDenied(AccessDeniedException e) {
        return BaseResponse.error(403, "无权限访问");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<Void> handleMethodValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("参数校验失败");
        return BaseResponse.error(400, msg);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<Void> handleConstraintValidation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .orElse("参数校验失败");
        return BaseResponse.error(400, msg);
    }

    /** Missing required query/path parameter (e.g. GET /xxx?param required but absent). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return BaseResponse.error(400, "缺少必要参数: " + e.getParameterName());
    }

    /** Type mismatch / date-format parse error on a bound parameter. */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<Void> handleTypeMismatch(Exception e) {
        return BaseResponse.error(400, "参数格式错误");
    }

    @ExceptionHandler({EntityNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public BaseResponse<Void> handleNotFound(EntityNotFoundException e) {
        return BaseResponse.error(404, "资源不存在");
    }

    /** Wrong HTTP method on a known path: map to 405 instead of a generic 500. */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public BaseResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return BaseResponse.error(405, "请求方法不支持: " + e.getMethod());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<Void> handleOther(Exception e) {
        // Log the full stack server-side, never expose exception internals to clients.
        log.error("Unhandled exception", e);
        return BaseResponse.error(500, "系统异常，请稍后重试");
    }
}
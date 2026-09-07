package org.temporedata.api.contract;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Unified v1 response envelope for every {@code /api/v1/**} endpoint.
 * Shape: { code, msg, data, requestId, traceId } (see refactor/04-api-contract.md).
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    /** 0 = success; otherwise an {@link ErrorCode} code. */
    private final int code;
    private final String msg;
    private final T data;
    private final String requestId;
    private final String traceId;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static ApiResponse<Void> error(ErrorCode ec, String requestId, String traceId) {
        return new ApiResponse<>(ec.getCode(), ec.getMsg(), null, requestId, traceId);
    }

    public static ApiResponse<Void> error(int code, String msg, String requestId, String traceId) {
        return new ApiResponse<>(code, msg, null, requestId, traceId);
    }

    /** Return a copy with a different message (e.g. concrete business error text). */
    public ApiResponse<T> withMsg(String msg) {
        return new ApiResponse<>(this.code, msg, this.data, this.requestId, this.traceId);
    }
}
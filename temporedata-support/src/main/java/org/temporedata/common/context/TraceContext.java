package org.temporedata.common.context;

import java.util.UUID;

/**
 * Request-scoped trace context holding the correlation request id (ThreadLocal).
 *
 * <p>Populated by the trace filter and cleared after each request. The same id
 * is also placed into the Mapped Diagnostic Context (MDC) as {@code requestId}
 * / {@code traceId} so every log line can be correlated (v2.0 §21).</p>
 */
public final class TraceContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private TraceContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    /** The current request id, or {@code "unknown"} when no request is active. */
    public static String getRequestId() {
        return REQUEST_ID.get() == null ? "unknown" : REQUEST_ID.get();
    }

    /** Generate a fresh correlation id (used when the client did not provide one). */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
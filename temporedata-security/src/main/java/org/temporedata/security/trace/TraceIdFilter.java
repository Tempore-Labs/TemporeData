package org.temporedata.security.trace;

import org.temporedata.common.context.TraceContext;
import org.temporedata.security.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Trace filter: establishes a per-request correlation id (RequestId / TraceId),
 * echoes it back via {@code X-Request-Id}, and enriches the MDC so every log line
 * carries {@code requestId/traceId/tenantId/userId} (v2.0 §21).
 *
 * <p>Registers after the JWT filter so tenant and user context are already
 * resolved when this filter populates the MDC.</p>
 */
@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 1. Resolve or generate the correlation id.
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = TraceContext.generate();
        }
        TraceContext.setRequestId(requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        // 2. Enrich MDC (tenant/user resolved by the preceding JWT filter).
        MDC.put("requestId", requestId);
        MDC.put("traceId", requestId);
        MDC.put("tenantId", orEmpty(TenantContext.getTenantId()));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            MDC.put("userId", auth.getName());
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("traceId");
            MDC.remove("tenantId");
            MDC.remove("userId");
            TraceContext.clear();
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
package org.temporedata.support.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Idempotency filter: for non-GET requests carrying an {@code Idempotency-Key} header,
 * deduplicate against {@link IdempotencyStore}. A repeated key replays the first response
 * (status + JSON body + request id). Only successful (2xx, non-empty) responses are cached.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_KEY = "Idempotency-Key";
    public static final String HEADER_RID = "X-Request-Id";

    private final IdempotencyStore store;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String method = req.getMethod();
        String key = req.getHeader(HEADER_KEY);
        if (key == null || key.isBlank() || "GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(req, res);
            return;
        }

        var cached = store.get(key);
        if (cached.isPresent()) {
            var c = cached.get();
            res.setStatus(c.getStatus());
            res.setCharacterEncoding("UTF-8");
            res.setContentType("application/json;charset=UTF-8");
            res.setHeader(HEADER_RID, c.getRequestId());
            res.getOutputStream().write(c.getBody());
            return;
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(res);
        chain.doFilter(req, wrapper);

        int status = wrapper.getStatus();
        byte[] body = wrapper.getContentAsByteArray();
        if (status >= 200 && status < 300 && body.length > 0) {
            store.put(key, new IdempotencyStore.CachedResponse(status, body, req.getHeader(HEADER_RID)));
        }
        wrapper.copyBodyToResponse();
    }
}
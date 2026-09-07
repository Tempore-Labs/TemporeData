package org.temporedata.support.idempotency;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * In-memory idempotency key store (Caffeine-backed, bounded). A duplicate request key
 * returns the cached status+body of the first call. Default TTL 24h; bounded to 20k keys.
 */
@Component
public class IdempotencyStore {

    private final Cache<String, CachedResponse> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .maximumSize(20_000)
            .build();

    @Getter
    public static class CachedResponse {
        private final int status;
        private final byte[] body;
        private final String requestId;

        public CachedResponse(int status, byte[] body, String requestId) {
            this.status = status;
            this.body = body;
            this.requestId = requestId;
        }
    }

    public Optional<CachedResponse> get(String key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    public void put(String key, CachedResponse response) {
        cache.put(key, response);
        // touch to keep the entry fresh
        cache.get(key, k -> response);
    }

    public void evict(String key) {
        cache.invalidate(key);
    }
}
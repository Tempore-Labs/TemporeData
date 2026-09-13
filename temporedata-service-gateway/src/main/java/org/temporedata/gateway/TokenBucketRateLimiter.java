package org.temporedata.gateway;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * [ENT-P1] Thread-safe token-bucket rate limiter (in-memory; per-key buckets).
 *
 * <p>Used by the Data API gateway to enforce per-consumer rate limits. Each key holds a bucket
 * of {@code burst} tokens refilled at {@code ratePerSec} tokens/second; {@code tryAcquire}
 * succeeds only when a token is available.
 */
@Component
public class TokenBucketRateLimiter {

    private static final class Bucket {
        private final double tokens;
        private final long lastNanos;

        Bucket(double tokens, long lastNanos) {
            this.tokens = tokens;
            this.lastNanos = lastNanos;
        }

        double tokens() {
            return tokens;
        }

        long lastNanos() {
            return lastNanos;
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param key         consumer/API identifier
     * @param ratePerSec  sustained refill rate (tokens/sec)
     * @param burst       bucket capacity (burst allowance)
     * @return true if a token was consumed within the limit
     */
    public boolean tryAcquire(String key, double ratePerSec, int burst) {
        long now = System.nanoTime();
        Bucket b = buckets.compute(key, (k, cur) -> {
            if (cur == null) {
                return new Bucket(burst, now);
            }
            long elapsedNs = now - cur.lastNanos();
            double refill = elapsedNs / 1_000_000_000.0 * ratePerSec;
            double tokens = Math.min(burst, cur.tokens() + refill);
            return new Bucket(tokens, now);
        });
        if (b.tokens() >= 1.0) {
            double remaining = b.tokens() - 1.0;
            if (buckets.replace(key, b, new Bucket(remaining, b.lastNanos()))) {
                return true;
            }
            return tryAcquire(key, ratePerSec, burst); // retry once under contention
        }
        return false;
    }

    /** Expose the approximate current token count (for observability). */
    public double currentTokens(String key) {
        Bucket b = buckets.get(key);
        return b == null ? 0 : b.tokens();
    }

    /** Reset state for a key (e.g. after a quota reset). */
    public void reset(String key) {
        buckets.remove(key);
    }
}
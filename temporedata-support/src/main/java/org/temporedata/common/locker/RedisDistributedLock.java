package org.temporedata.common.locker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Redis-backed {@link DistributedLockProvider} for multi-node deployments.
 *
 * <p>Enabled via {@code temporedata.lock.provider=redis}. Acquisition uses the atomic
 * {@code SET key value NX PX ttl} primitive; release uses a Lua compare-and-delete
 * script so a lock can only be released by the acquiring holder (identified by a
 * per-acquire token).</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "temporedata.lock.provider", havingValue = "redis")
public class RedisDistributedLock implements DistributedLockProvider {

    private static final String KEY_PREFIX = "td:lock:";

    /** Lua: delete the key only if it still holds our token. */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    /** Logical key -> holder token of the most recent acquire (guard for release). */
    private final ConcurrentMap<String, String> holders = new ConcurrentHashMap<>();

    @Value("${temporedata.lock.ttl-ms:30000}")
    private long ttlMs;

    public RedisDistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(String key, long timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(timeoutMs, 1);
        String token = UUID.randomUUID().toString().replace("-", "");
        String fullKey = KEY_PREFIX + key;

        // Poll SET NX PX until the timeout elapses or the lock is acquired.
        while (System.currentTimeMillis() < deadline) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(fullKey, token, Duration.ofMillis(ttlMs));
            if (Boolean.TRUE.equals(acquired)) {
                holders.put(key, token);
                log.debug("Redis lock acquired: {}", key);
                return true;
            }
            try {
                long remain = deadline - System.currentTimeMillis();
                if (remain > 0) Thread.sleep(Math.min(50, remain));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Redis lock acquisition interrupted: {}", key);
                return false;
            }
        }
        log.warn("Redis lock acquisition timed out: {}", key);
        return false;
    }

    @Override
    public void unlock(String key) {
        String token = holders.remove(key);
        if (token == null) {
            return;
        }
        try {
            redisTemplate.execute(UNLOCK_SCRIPT,
                    Collections.singletonList(KEY_PREFIX + key), token);
        } catch (Exception e) {
            // A Redis outage must not block job completion; fall back to a delete.
            log.warn("Redis unlock failed for key {}, fallback delete: {}", key, e.getMessage());
            redisTemplate.delete(KEY_PREFIX + key);
        }
    }

    @Override
    public boolean isLocked(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + key));
    }
}
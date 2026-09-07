package org.temporedata.common.locker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory {@link DistributedLockProvider} for single-node deployment.
 * Prevents concurrent execution of the same job within one JVM.
 *
 * <p>Active by default (temporedata.lock.provider=memory). For multi-node deployments
 * set {@code temporedata.lock.provider=redis} and a {@link RedisDistributedLock} bean is
 * created instead. Business code only depends on {@link DistributedLockProvider}.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "temporedata.lock.provider", havingValue = "memory", matchIfMissing = true)
public class DistributedLock implements DistributedLockProvider {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key, long timeoutMs) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            boolean acquired = lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            if (acquired) {
                log.debug("Lock acquired: {}", key);
            } else {
                log.warn("Lock acquisition timed out: {}", key);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock acquisition interrupted: {}", key);
            return false;
        }
    }

    /**
     * Release the lock.
     */
    @Override
    public void unlock(String key) {
        ReentrantLock lock = locks.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Lock released: {}", key);
            // Remove lock if no longer used to prevent memory leak
            if (!lock.hasQueuedThreads()) {
                locks.remove(key);
            }
        }
    }

    /**
     * Check if lock is currently held.
     */
    public boolean isLocked(String key) {
        ReentrantLock lock = locks.get(key);
        return lock != null && lock.isLocked();
    }

    @PreDestroy
    public void cleanup() {
        locks.forEach((key, lock) -> {
            if (lock.isLocked()) {
                log.warn("Force unlocking on shutdown: {}", key);
            }
        });
        locks.clear();
        log.info("All distributed locks cleaned up");
    }
}
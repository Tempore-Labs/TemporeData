package org.temporedata.common.locker;

/**
 * Abstration over the distributed / inter-process lock used to prevent concurrent
 * execution of the same job across nodes (v2.0 §9.2).
 *
 * <p>Business code must depend on this interface, never on a concrete
 * (in-memory / Redis) implementation, so the provider can be swapped without
 * touching callers.</p>
 */
public interface DistributedLockProvider {

    /**
     * Try to acquire the lock for {@code key} within {@code timeoutMs}.
     *
     * @return true if acquired, false on timeout / failure
     */
    boolean tryLock(String key, long timeoutMs);

    /** Release the lock held by the current thread for {@code key}. */
    void unlock(String key);

    /** Whether the lock for {@code key} is currently held by any thread. */
    boolean isLocked(String key);
}
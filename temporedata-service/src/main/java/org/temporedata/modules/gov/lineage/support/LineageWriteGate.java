package org.temporedata.modules.gov.lineage.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.temporedata.common.locker.DistributedLockProvider;

/**
 * Guards lineage edge writes against concurrent duplicate creation.
 * <p>Uses the platform's {@link DistributedLockProvider} (memory by default, Redis on
 * multi-node) with a per-edge lock key {@code td:lock:lineage:{from}:{to}}. If the lock
 * cannot be acquired within the timeout the caller is expected to abort/retry rather
 * than write a duplicate edge.</p>
 */
@Component
@RequiredArgsConstructor
public class LineageWriteGate {

    private static final String PREFIX = "td:lock:lineage:";
    private static final long LOCK_TIMEOUT_MS = 5_000L;

    private final DistributedLockProvider lockProvider;

    public boolean tryLock(String from, String to) {
        return lockProvider.tryLock(key(from, to), LOCK_TIMEOUT_MS);
    }

    public void unlock(String from, String to) {
        lockProvider.unlock(key(from, to));
    }

    private String key(String from, String to) {
        return PREFIX + from + ":" + to;
    }
}
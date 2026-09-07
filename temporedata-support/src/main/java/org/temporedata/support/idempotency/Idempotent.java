package org.temporedata.support.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a mutating endpoint as idempotent. The {@link IdempotencyKeyFilter} keys off the
 * {@code Idempotency-Key} header at the filter level; this annotation documents intent and
 * gives a slot for future per-endpoint TTL/scope tuning. Endpoints without a header are
 * simply not deduplicated.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Idempotent {
    /** Time-to-live of a dedup entry (e.g. "24h"); default 24 hours. */
    String ttl() default "24h";
}
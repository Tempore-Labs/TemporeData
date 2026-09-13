package org.temporedata.gateway;

/**
 * [ENT-P1] Data API gateway capability namespace root.
 *
 * <p>Hosts the low-code "SQL to API" / "Table to API" exposure tier with OAuth2/AppKey
 * authentication, token-bucket rate limiting, quota enforcement, Redis response caching and
 * per-request call audit logging. It reuses temporedata-security RBAC/token primitives so the
 * exposed surface is governed by the same policies as direct calls.
 *
 * <p>Boundary contract: the gateway only fronts/aggregates APIs; it performs no storage I/O.
 */
public final class Gateway {

    private Gateway() {
        // namespace marker only
    }
}
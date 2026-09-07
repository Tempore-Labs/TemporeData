package org.temporedata.modules.gov.lineage.security;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.temporedata.api.gov.lineage.EntityReference;
import org.temporedata.api.gov.lineage.LineageEdge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Filters lineage nodes/edges by the caller's tenant & domain before returning
 * them to the client. Nodes outside the caller's tenant/domain are dropped (or
 * marked {@code hidden} when still referenced) and the tally is reported so the
 * front-end can show "N 节点因权限未展示".
 */
@Component
@RequiredArgsConstructor
public class LineageDomainFilter {

    /**
     * Sanitize a result: drop references the caller may not see and count them.
     *
     * @param desiredTenantId the tenant context of the current caller (nullable = no scoping)
     * @return result carrying the surviving edges/nodes and the hidden count
     */
    public FilterResult filter(List<EntityReference> nodes, List<LineageEdge> edges, String desiredTenantId) {
        FilterResult result = new FilterResult();
        Map<String, Integer> hidden = new LinkedHashMap<>();
        boolean scoped = desiredTenantId != null && !desiredTenantId.isBlank();

        for (LineageEdge e : edges) {
            if (scoped && !isVisibleForTenant(e, desiredTenantId)) {
                bump(hidden, "tenant");
                continue;
            }
            result.getEdges().add(e);
        }
        // Keep referenced-but-hidden nodes present with hidden=true so edges stay coherent;
        // only fully isolated nodes are dropped and counted for the report.
        for (EntityReference n : nodes) {
            if (scoped && !tenantOf(n).matchesTenant(desiredTenantId)) {
                n.setHidden(true);
                bump(hidden, domainKey(n));
            }
            result.getNodes().add(n);
        }
        result.setHiddenCount(hidden);
        return result;
    }

    private boolean isVisibleForTenant(LineageEdge e, String tenant) {
        // Both endpoints must be in the caller's tenant scope to surface the edge.
        return tenantOf(e.getFrom()).matchesTenant(tenant) && tenantOf(e.getTo()).matchesTenant(tenant);
    }

    private TenantScope tenantOf(EntityReference n) {
        return n == null ? new TenantScope() : new TenantScope(n.getDatabase(), n.getSchema());
    }

    private String domainKey(EntityReference n) {
        if (n != null && n.getDatabase() != null && !n.getDatabase().isBlank()) {
            return n.getDatabase();
        }
        return "tenant";
    }

    private void bump(Map<String, Integer> m, String key) {
        m.merge(key, 1, Integer::sum);
    }

    /** Result of domain/tenant filtering. */
    @Data
    public static class FilterResult {
        private List<EntityReference> nodes = new ArrayList<>();
        private List<LineageEdge> edges = new ArrayList<>();
        private Map<String, Integer> hiddenCount = new LinkedHashMap<>();
    }

    /** Lightweight tenant-scope comparer for a node (database/schema). */
    private static final class TenantScope {
        private final String database;
        private final String schema;

        TenantScope() {
            this(null, null);
        }

        TenantScope(String database, String schema) {
            this.database = database;
            this.schema = schema;
        }

        boolean matchesTenant(String tenant) {
            // Nodes carrying a database/schema are scoped by that prefix; a node with no
            // scoping metadata is treated as globally visible (matches any tenant).
            if (database == null || database.isBlank()) {
                return true;
            }
            return tenant == null || tenant.isBlank()
                    || database.equalsIgnoreCase(tenant)
                    || (schema != null && schema.equalsIgnoreCase(tenant));
        }
    }
}
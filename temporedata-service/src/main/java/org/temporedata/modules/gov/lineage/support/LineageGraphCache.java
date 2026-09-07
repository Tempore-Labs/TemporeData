package org.temporedata.modules.gov.lineage.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Value;
import org.springframework.stereotype.Component;
import org.temporedata.modules.gov.lineage.config.LineageProperties;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Dedicated in-memory graph cache for lineage. Bounded by
 * {@link LineageProperties#getGraphCacheMax()} and keyed by the tenant+params string;
 * each entry also records the set of involved node ids so a targeted
 * {@link LineageGraphCache#invalidateEdge} can evict exactly the subgraphs touching an
 * edited endpoint. Very large graphs (beyond the cap) are not cached — they are paged
 * by the explorer instead (see {@code LineageStrategySelector}).
 *
 * <p>This lives on a dedicated Caffeine instance (not the global {@code CACHE_LINEAGE}
 * region) to avoid generic Redis/Caffeine manager serialization issues with the DTOs.</p>
 */
@Component
public class LineageGraphCache {

    private final Cache<String, CachedEntry> cache;

    public LineageGraphCache(LineageProperties properties) {
        this.cache = Caffeine.newBuilder().maximumSize(properties.getGraphCacheMax()).build();
    }

    public void put(String key, Object value, Set<String> nodeIds) {
        cache.put(key, new CachedEntry(value, nodeIds));
    }

    public Object get(String key) {
        CachedEntry e = cache.getIfPresent(key);
        return e == null ? null : e.getValue();
    }

    /** Evict entries whose subgraph touches either endpoint of the edited edge. */
    public void invalidateEdge(String from, String to) {
        ConcurrentMap<String, CachedEntry> map = cache.asMap();
        map.entrySet().removeIf(en ->
                en.getValue().getNodeIds().contains(from) || en.getValue().getNodeIds().contains(to));
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Value
    private static final class CachedEntry {
        Object value;
        Set<String> nodeIds;
    }
}
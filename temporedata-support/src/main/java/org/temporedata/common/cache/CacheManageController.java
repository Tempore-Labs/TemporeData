package org.temporedata.common.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for cache management.
 * Provides endpoints to view cache stats, clear caches, and refresh cache entries.
 *
 * Endpoints:
 *   GET  /api/cache/stats      - View all cache statistics
 *   GET  /api/cache/{name}     - View specific cache details
 *   DELETE /api/cache/{name}   - Clear a specific cache
 *   DELETE /api/cache          - Clear all caches
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheManageController {

    private final CacheManager cacheManager;

    /**
     * Get statistics for all caches.
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cacheManagerType", cacheManager.getClass().getSimpleName());
        result.put("totalCaches", cacheManager.getCacheNames().size());

        List<Map<String, Object>> cacheDetails = new ArrayList<>();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("name", name);
                detail.put("nativeCache", cache.getNativeCache().getClass().getSimpleName());

                // Caffeine-specific stats
                if (cache instanceof CaffeineCache) {
                    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                            (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
                                    ((CaffeineCache) cache).getNativeCache();
                    CacheStats stats = nativeCache.stats();
                    detail.put("estimatedSize", nativeCache.estimatedSize());
                    detail.put("hitCount", stats.hitCount());
                    detail.put("missCount", stats.missCount());
                    detail.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
                    detail.put("evictionCount", stats.evictionCount());
                    detail.put("loadCount", stats.loadCount());
                    detail.put("totalLoadTime", stats.totalLoadTime() / 1_000_000 + "ms");
                }

                cacheDetails.add(detail);
            }
        }
        result.put("caches", cacheDetails);
        return result;
    }

    /**
     * Get details for a specific cache.
     */
    @GetMapping("/{name}")
    public Map<String, Object> getCache(@PathVariable String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Cache not found: " + name);
            err.put("availableCaches", cacheManager.getCacheNames());
            return err;
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("name", name);
        detail.put("nativeCache", cache.getNativeCache().getClass().getSimpleName());

        if (cache instanceof CaffeineCache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
                            ((CaffeineCache) cache).getNativeCache();
            CacheStats stats = nativeCache.stats();
            detail.put("estimatedSize", nativeCache.estimatedSize());
            detail.put("hitCount", stats.hitCount());
            detail.put("missCount", stats.missCount());
            detail.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
            detail.put("evictionCount", stats.evictionCount());
            detail.put("loadCount", stats.loadCount());
            detail.put("evictionWeight", stats.evictionWeight());
            detail.put("requestCount", stats.requestCount());
        }

        return detail;
    }

    /**
     * Clear a specific cache by name.
     */
    @DeleteMapping("/{name}")
    public Map<String, Object> evictCache(@PathVariable String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Cache not found: " + name);
            err.put("availableCaches", cacheManager.getCacheNames());
            return err;
        }
        cache.clear();
        log.info("Cache cleared: {}", name);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("message", "Cache '" + name + "' has been cleared");
        return result;
    }

    /**
     * Clear all caches.
     */
    @DeleteMapping
    public Map<String, Object> evictAll() {
        List<String> cleared = new ArrayList<>();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
                cleared.add(name);
            }
        }
        log.info("All caches cleared: {}", cleared);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("message", "All caches have been cleared");
        result.put("clearedCaches", cleared);
        result.put("totalCleared", cleared.size());
        return result;
    }
}
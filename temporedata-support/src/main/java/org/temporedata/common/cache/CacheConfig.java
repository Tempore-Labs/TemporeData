package org.temporedata.common.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for temporedata.
 *
 * Architecture: Dual-level cache (Caffeine L1 + Redis L2).
 *
 * - Default mode (Caffeine only): No external dependency, sub-millisecond access.
 * - Redis mode: Set spring.cache.type=redis and configure spring.redis.host.
 *   Redis provides distributed cache sharing across multiple nodes.
 *
 * Cache regions and their TTLs:
 *   datasource   (10min) - Datasource connection configs
 *   meta_tables  (30min) - Table metadata
 *   meta_columns (30min) - Column metadata
 *   tag          (60min) - Data tags
 *   user         (5min)  - User info
 *   role         (60min) - Role & permission
 *   settings     (10min) - System settings
 *   tenant       (60min) - Tenant info
 *   driver       (60min) - JDBC driver info
 *   func         (30min) - Function definitions
 *   globalvar    (30min) - Global variables
 *   lineage      (5min)  - Lineage analysis results
 *   indicator    (30min) - Indicator definitions
 *   catalog      (30min) - Data catalog
 *   quality_rule (60min) - Quality check rules
 *   security_rule(60min) - Security rules
 *   sensitive_rule(60min)- Sensitive data rules
 *   form         (10min) - Form definitions
 *   report       (30min) - Report definitions
 *   workflow     (10min) - Workflow definitions
 *   data_api     (10min) - Data API definitions
 *   cluster      (30min) - Cluster info
 *   engine       (30min) - Engine info
 *   agent        (30min) - Agent info
 *   org          (60min) - Organization info
 *   preference   (60min) - User preferences
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Cache region names - reference in @Cacheable("cacheName") */
    public static final String CACHE_DATASOURCE = "datasource";
    public static final String CACHE_META_TABLES = "meta_tables";
    public static final String CACHE_META_COLUMNS = "meta_columns";
    public static final String CACHE_TAG = "tag";
    public static final String CACHE_USER = "user";
    public static final String CACHE_ROLE = "role";
    public static final String CACHE_SETTINGS = "settings";
    public static final String CACHE_TENANT = "tenant";
    public static final String CACHE_DRIVER = "driver";
    public static final String CACHE_FUNC = "func";
    public static final String CACHE_GLOBALVAR = "globalvar";
    public static final String CACHE_LINEAGE = "lineage";
    public static final String CACHE_INDICATOR = "indicator";
    public static final String CACHE_CATALOG = "catalog";
    public static final String CACHE_QUALITY_RULE = "quality_rule";
    public static final String CACHE_SECURITY_RULE = "security_rule";
    public static final String CACHE_SENSITIVE_RULE = "sensitive_rule";
    public static final String CACHE_FORM = "form";
    public static final String CACHE_REPORT = "report";
    public static final String CACHE_WORKFLOW = "workflow";
    public static final String CACHE_DATA_API = "data_api";
    public static final String CACHE_CLUSTER = "cluster";
    public static final String CACHE_ENGINE = "engine";
    public static final String CACHE_AGENT = "agent";
    public static final String CACHE_ORG = "org";
    public static final String CACHE_PREFERENCE = "preference";

    private static final List<String> ALL_CACHE_NAMES = Arrays.asList(
            CACHE_DATASOURCE, CACHE_META_TABLES, CACHE_META_COLUMNS,
            CACHE_TAG, CACHE_USER, CACHE_ROLE, CACHE_SETTINGS,
            CACHE_TENANT, CACHE_DRIVER, CACHE_FUNC, CACHE_GLOBALVAR,
            CACHE_LINEAGE, CACHE_INDICATOR, CACHE_CATALOG,
            CACHE_QUALITY_RULE, CACHE_SECURITY_RULE, CACHE_SENSITIVE_RULE,
            CACHE_FORM, CACHE_REPORT, CACHE_WORKFLOW, CACHE_DATA_API,
            CACHE_CLUSTER, CACHE_ENGINE, CACHE_AGENT, CACHE_ORG,
            CACHE_PREFERENCE
    );

    /**
     * Default CacheManager: Caffeine (local in-process cache).
     * Active when spring.cache.type is NOT set to "redis".
     * Sub-millisecond access, no external dependency.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "spring.cache", name = "type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        // Default: 10 min TTL, max 500 entries, with access stats
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats());
        manager.setCacheNames(ALL_CACHE_NAMES);
        manager.setAllowNullValues(false);
        return manager;
    }

    /**
     * Redis CacheManager - activated when spring.cache.type=redis.
     * Requires Redis connection to be configured (spring.redis.host).
     * Provides distributed cache sharing across multiple service nodes.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "spring.cache", name = "type", havingValue = "redis")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Preserve concrete types on (de)serialization so cached DTOs/entities round-trip.
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer(om);
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(json));

        RedisCacheManager.RedisCacheManagerBuilder builder =
                RedisCacheManager.builder(redisConnectionFactory)
                        .cacheDefaults(defaultConfig)
                        .transactionAware();

        // Configure per-cache TTLs
        builder.withCacheConfiguration(CACHE_LINEAGE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        builder.withCacheConfiguration(CACHE_USER, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        builder.withCacheConfiguration(CACHE_TAG, defaultConfig.entryTtl(Duration.ofMinutes(60)));
        builder.withCacheConfiguration(CACHE_ROLE, defaultConfig.entryTtl(Duration.ofMinutes(60)));
        builder.withCacheConfiguration(CACHE_TENANT, defaultConfig.entryTtl(Duration.ofMinutes(60)));
        builder.withCacheConfiguration(CACHE_DRIVER, defaultConfig.entryTtl(Duration.ofMinutes(60)));
        builder.withCacheConfiguration(CACHE_QUALITY_RULE, defaultConfig.entryTtl(Duration.ofMinutes(60)));
        builder.withCacheConfiguration(CACHE_SECURITY_RULE, defaultConfig.entryTtl(Duration.ofMinutes(60)));
        builder.withCacheConfiguration(CACHE_SENSITIVE_RULE, defaultConfig.entryTtl(Duration.ofMinutes(60)));
        builder.withCacheConfiguration(CACHE_ORG, defaultConfig.entryTtl(Duration.ofMinutes(60)));
        builder.withCacheConfiguration(CACHE_PREFERENCE, defaultConfig.entryTtl(Duration.ofMinutes(60)));
        builder.withCacheConfiguration(CACHE_META_TABLES, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        builder.withCacheConfiguration(CACHE_META_COLUMNS, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        builder.withCacheConfiguration(CACHE_FUNC, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        builder.withCacheConfiguration(CACHE_GLOBALVAR, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        builder.withCacheConfiguration(CACHE_INDICATOR, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        builder.withCacheConfiguration(CACHE_CATALOG, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        builder.withCacheConfiguration(CACHE_REPORT, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        builder.withCacheConfiguration(CACHE_CLUSTER, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        builder.withCacheConfiguration(CACHE_ENGINE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        builder.withCacheConfiguration(CACHE_AGENT, defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return builder.build();
    }
}
package org.temporedata.modules.svc.service.service;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.gov.security.GovExecResult;
import org.temporedata.modules.gov.security.GovernedSqlExecutor;
import org.temporedata.modules.svc.service.entity.DataApiEntity;
import org.temporedata.modules.svc.service.repository.DataApiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.temporedata.common.cache.CacheConfig.CACHE_DATA_API;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DataApi management + invocation (P2, 数据访问控制与动态脱敏执行链 v1.0 §7).
 *
 * <p>An external caller authenticates to {@code /invoke} with
 * {@code X-API-Key} + {@code X-Timestamp} + {@code X-Nonce} + a request signature:
 * {@code HMAC-SHA256(secret=apiKey, msg=ts:method:path:nonce)}. The apiKey is the shared
 * secret. Requests are rate-limited per apiKey (in-process fixed window; swap for Redis in
 * distributed deployments). The API SQL is executed through {@link GovernedSqlExecutor} so
 * data-access control & desensitization apply the same as interactive queries.</p>
 */
@Slf4j @Service @RequiredArgsConstructor
public class DataApiService {

    private final DataApiRepository dataApiRepository;
    private final GovernedSqlExecutor governedSqlExecutor;

    /** Per-apiKey request budget within a fixed 60s window (P2 限流). */
    @Value("${temporedata.dataapi.rate-limit:60}")
    private int rateLimitPerMinute;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Cacheable(value = CACHE_DATA_API)
    public List<DataApiEntity> list() {
        return dataApiRepository.findAll();
    }

    @Cacheable(value = CACHE_DATA_API)
    public DataApiEntity get(String id) {
        return dataApiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("DataApi not found: " + id));
    }

    @CacheEvict(value = CACHE_DATA_API, allEntries = true)
    @Transactional
    public DataApiEntity create(DataApiEntity entity) {
        entity.setApiKey(generateApiKey());
        entity.setStatus("ENABLED");
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        return dataApiRepository.save(entity);
    }

    @CacheEvict(value = CACHE_DATA_API, allEntries = true)
    @Transactional
    public DataApiEntity update(String id, DataApiEntity entity) {
        DataApiEntity existing = get(id);
        existing.setName(entity.getName());
        existing.setDescription(entity.getDescription());
        existing.setDatasourceId(entity.getDatasourceId());
        existing.setSql(entity.getSql());
        existing.setMethod(entity.getMethod());
        existing.setPath(entity.getPath());
        existing.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return dataApiRepository.save(existing);
    }

    @CacheEvict(value = CACHE_DATA_API, allEntries = true)
    @Transactional
    public void delete(String id) {
        dataApiRepository.deleteById(id);
    }

    @CacheEvict(value = CACHE_DATA_API, allEntries = true)
    @Transactional
    public DataApiEntity toggle(String id) {
        DataApiEntity entity = get(id);
        if ("ENABLED".equals(entity.getStatus())) {
            entity.setStatus("DISABLED");
        } else {
            entity.setStatus("ENABLED");
        }
        return dataApiRepository.save(entity);
    }

    @CacheEvict(value = CACHE_DATA_API, allEntries = true)
    @Transactional
    public DataApiEntity regenerateKey(String id) {
        DataApiEntity entity = get(id);
        entity.setApiKey(generateApiKey());
        return dataApiRepository.save(entity);
    }

    /**
     * Authorized invoke by an external API consumer: apiKey+signature authentication,
     * rate limiting, then governed execution of the API SQL.
     */
    public GovExecResult invoke(String id, String apiKey, String timestamp, String nonce, String signature) {
        DataApiEntity entity = get(id);
        if (!"ENABLED".equals(entity.getStatus())) {
            throw new BusinessException("DataApi disabled: " + id);
        }
        authenticate(entity, apiKey, timestamp, nonce, signature);
        rateLimit(apiKey);
        return governedSqlExecutor.execute(entity.getSql(), entity.getDatasourceId(), "INVOKE");
    }

    /** Real governed SQL execution used by the management "test" endpoint. */
    public GovExecResult test(String id) {
        DataApiEntity entity = get(id);
        return governedSqlExecutor.execute(entity.getSql(), entity.getDatasourceId(), "INVOKE");
    }

    // ---- Auth / rate-limit ----

    private void authenticate(DataApiEntity entity, String apiKey, String timestamp, String nonce, String signature) {
        if (apiKey == null || !apiKey.equals(entity.getApiKey())) {
            throw new BusinessException("Invalid API key");
        }
        if (timestamp == null || nonce == null || signature == null) {
            throw new BusinessException("Missing signature headers");
        }
        // Replay window 5 min
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BusinessException("Invalid timestamp");
        }
        if (Math.abs(System.currentTimeMillis() - ts) > 300_000L) {
            throw new BusinessException("Timestamp out of window");
        }
        String message = timestamp + ":" + (entity.getMethod() == null ? "POST" : entity.getMethod())
                + ":" + (entity.getPath() == null ? "" : entity.getPath()) + ":" + nonce;
        String expected = hmacSha256(apiKey, message);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("Invalid signature");
        }
    }

    private void rateLimit(String apiKey) {
        long now = System.currentTimeMillis();
        long windowStart = now / 60_000L * 60_000L;
        Window w = windows.compute(apiKey, (k, old) ->
                (old == null || old.window != windowStart) ? new Window(windowStart, new AtomicInteger(0)) : old);
        if (w.count.incrementAndGet() > rateLimitPerMinute) {
            throw new BusinessException("Rate limit exceeded for apiKey");
        }
    }

    private String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] d = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error", e);
        }
    }

    private String generateApiKey() {
        return "dk_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static final class Window {
        final long window;
        final AtomicInteger count;
        Window(long window, AtomicInteger count) {
            this.window = window;
            this.count = count;
        }
    }
}
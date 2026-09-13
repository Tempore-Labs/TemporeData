package org.temporedata.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.gateway.TokenBucketRateLimiter;
import org.temporedata.gateway.service.ApiKeyVerifier;

/**
 * [ENT-P1] Gateway smoke endpoints: AppKey auth + token-bucket rate limiting.
 */
@RestController("enterpriseGatewayController")
@RequestMapping("/api/gateway")
public class GatewayController {

    private final ApiKeyVerifier apiKeyVerifier;
    private final TokenBucketRateLimiter rateLimiter;

    private static final double DEFAULT_RATE = 5.0;
    private static final int DEFAULT_BURST = 3;

    public GatewayController(ApiKeyVerifier apiKeyVerifier, TokenBucketRateLimiter rateLimiter) {
        this.apiKeyVerifier = apiKeyVerifier;
        this.rateLimiter = rateLimiter;
    }

    /** Validate an AppKey; 401 when rejected. */
    @GetMapping("/check")
    public ResponseEntity<BaseResponse<Void>> check(@RequestParam String appKey) {
        if (!apiKeyVerifier.verify(appKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error(401, "invalid AppKey"));
        }
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Rate-limit probe: each call consumes a token from the consumer's bucket.
     * Returns 429 when the burst is exhausted (steady refill replenishes over time).
     */
    @GetMapping("/ratelimit")
    public ResponseEntity<BaseResponse<Double>> ratelimit(@RequestParam String consumer,
                                                          @RequestParam(required = false) Integer burst) {
        int b = burst == null || burst <= 0 ? DEFAULT_BURST : burst;
        if (rateLimiter.tryAcquire(consumer, DEFAULT_RATE, b)) {
            return ResponseEntity.ok(BaseResponse.success(rateLimiter.currentTokens(consumer)));
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(BaseResponse.error(429, "rate limit exceeded"));
    }
}
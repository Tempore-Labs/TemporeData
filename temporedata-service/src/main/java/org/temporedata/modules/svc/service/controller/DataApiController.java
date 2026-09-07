package org.temporedata.modules.svc.service.controller;

import org.temporedata.modules.svc.service.entity.DataApiEntity;
import org.temporedata.modules.svc.service.service.DataApiService;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.gov.security.GovExecResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class DataApiController {

    private final DataApiService dataApiService;

    @GetMapping
    public BaseResponse<List<DataApiEntity>> list() {
        return BaseResponse.success(dataApiService.list());
    }

    @GetMapping("/{id}")
    public BaseResponse<DataApiEntity> get(@PathVariable String id) {
        return BaseResponse.success(dataApiService.get(id));
    }

    @PostMapping
    public BaseResponse<DataApiEntity> create(@RequestBody DataApiEntity entity) {
        return BaseResponse.success(dataApiService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<DataApiEntity> update(@PathVariable String id, @RequestBody DataApiEntity entity) {
        return BaseResponse.success(dataApiService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        dataApiService.delete(id);
        return BaseResponse.success();
    }

    @PutMapping("/{id}/toggle")
    public BaseResponse<DataApiEntity> toggle(@PathVariable String id) {
        return BaseResponse.success(dataApiService.toggle(id));
    }

    @PutMapping("/{id}/regenerate-key")
    public BaseResponse<DataApiEntity> regenerateKey(@PathVariable String id) {
        return BaseResponse.success(dataApiService.regenerateKey(id));
    }

    @PostMapping("/{id}/test")
    public BaseResponse<GovExecResult> test(@PathVariable String id) {
        return BaseResponse.success(dataApiService.test(id));
    }

    /**
     * External caller invocation. Authenticated by X-API-Key + X-Timestamp + X-Nonce +
     * X-Signature (HMAC-SHA256 over {@code ts:method:path:nonce} using apiKey as secret),
     * rate-limited per apiKey, then runs the API SQL through the governance chain.
     */
    @PostMapping("/{id}/invoke")
    public BaseResponse<GovExecResult> invoke(@PathVariable String id,
                                              @RequestHeader("X-API-Key") String apiKey,
                                              @RequestHeader("X-Timestamp") String timestamp,
                                              @RequestHeader("X-Nonce") String nonce,
                                              @RequestHeader("X-Signature") String signature) {
        return BaseResponse.success(dataApiService.invoke(id, apiKey, timestamp, nonce, signature));
    }
}
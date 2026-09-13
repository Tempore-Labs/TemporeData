package org.temporedata.integration.core.secret.controller;

import org.temporedata.api.integration.secret.SecretReq;
import org.temporedata.api.integration.secret.SecretRes;
import org.temporedata.integration.core.secret.service.SecretService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/secret")
@RequiredArgsConstructor
public class SecretController {

    private final SecretService secretService;

    @GetMapping
    public BaseResponse<List<SecretRes>> list() {
        return BaseResponse.success(secretService.listRes());
    }

    @PostMapping
    public BaseResponse<SecretRes> create(@RequestBody SecretReq req) {
        return BaseResponse.success(secretService.create(req));
    }

    @PutMapping("/{id}")
    public BaseResponse<SecretRes> update(@PathVariable String id, @RequestBody SecretReq req) {
        return BaseResponse.success(secretService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        secretService.delete(id);
        return BaseResponse.success();
    }
}
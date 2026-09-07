package org.temporedata.modules.gov.sensitive.controller;

import org.temporedata.modules.gov.sensitive.entity.SensitiveEntity;
import org.temporedata.modules.gov.sensitive.service.SensitiveService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensitive")
@RequiredArgsConstructor
public class SensitiveController {

    private final SensitiveService sensitiveService;

    @GetMapping("/page")
    public BaseResponse<Page<SensitiveEntity>> page(Pageable pageable) { return BaseResponse.success(sensitiveService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<SensitiveEntity>> list() { return BaseResponse.success(sensitiveService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<SensitiveEntity> get(@PathVariable String id) { return BaseResponse.success(sensitiveService.get(id)); }

    @PostMapping
    public BaseResponse<SensitiveEntity> create(@RequestBody SensitiveEntity entity) { return BaseResponse.success(sensitiveService.create(entity)); }

    @PutMapping
    public BaseResponse<SensitiveEntity> update(@RequestBody SensitiveEntity entity) { return BaseResponse.success(sensitiveService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { sensitiveService.delete(id); return BaseResponse.success(); }
}

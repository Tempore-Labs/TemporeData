package org.temporedata.modules.svc.apilog.controller;

import org.temporedata.modules.svc.apilog.entity.ApilogEntity;
import org.temporedata.modules.svc.apilog.service.ApilogService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apilog")
@RequiredArgsConstructor
public class ApilogController {

    private final ApilogService apilogService;

    @GetMapping("/page")
    public BaseResponse<Page<ApilogEntity>> page(Pageable pageable) { return BaseResponse.success(apilogService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<ApilogEntity>> list() { return BaseResponse.success(apilogService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<ApilogEntity> get(@PathVariable String id) { return BaseResponse.success(apilogService.get(id)); }

    @PostMapping
    public BaseResponse<ApilogEntity> create(@RequestBody ApilogEntity entity) { return BaseResponse.success(apilogService.create(entity)); }

    @PutMapping
    public BaseResponse<ApilogEntity> update(@RequestBody ApilogEntity entity) { return BaseResponse.success(apilogService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { apilogService.delete(id); return BaseResponse.success(); }
}

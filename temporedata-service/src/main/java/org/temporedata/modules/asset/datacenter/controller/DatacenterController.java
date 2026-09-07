package org.temporedata.modules.asset.datacenter.controller;

import org.temporedata.modules.asset.datacenter.entity.DatacenterEntity;
import org.temporedata.modules.asset.datacenter.service.DatacenterService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datacenter")
@RequiredArgsConstructor
public class DatacenterController {

    private final DatacenterService datacenterService;

    @GetMapping("/page")
    public BaseResponse<Page<DatacenterEntity>> page(Pageable pageable) { return BaseResponse.success(datacenterService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<DatacenterEntity>> list() { return BaseResponse.success(datacenterService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<DatacenterEntity> get(@PathVariable String id) { return BaseResponse.success(datacenterService.get(id)); }

    @PostMapping
    public BaseResponse<DatacenterEntity> create(@RequestBody DatacenterEntity entity) { return BaseResponse.success(datacenterService.create(entity)); }

    @PutMapping
    public BaseResponse<DatacenterEntity> update(@RequestBody DatacenterEntity entity) { return BaseResponse.success(datacenterService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { datacenterService.delete(id); return BaseResponse.success(); }
}

package org.temporedata.modules.svc.service.controller;

import org.temporedata.modules.svc.service.entity.ServiceEntity;
import org.temporedata.modules.svc.service.service.ServiceService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dataapi")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping("/page")
    public BaseResponse<Page<ServiceEntity>> page(Pageable pageable) { return BaseResponse.success(serviceService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<ServiceEntity>> list() { return BaseResponse.success(serviceService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<ServiceEntity> get(@PathVariable String id) { return BaseResponse.success(serviceService.get(id)); }

    @PostMapping
    public BaseResponse<ServiceEntity> create(@RequestBody ServiceEntity entity) { return BaseResponse.success(serviceService.create(entity)); }

    @PutMapping
    public BaseResponse<ServiceEntity> update(@RequestBody ServiceEntity entity) { return BaseResponse.success(serviceService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { serviceService.delete(id); return BaseResponse.success(); }
}

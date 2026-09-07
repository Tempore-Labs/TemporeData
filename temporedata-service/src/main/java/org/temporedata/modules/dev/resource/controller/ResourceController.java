package org.temporedata.modules.dev.resource.controller;

import org.temporedata.modules.dev.resource.entity.ResourceEntity;
import org.temporedata.modules.dev.resource.service.ResourceService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    @GetMapping("/page") public BaseResponse<Page<ResourceEntity>> page(Pageable p) { return BaseResponse.success(resourceService.page(p)); }

    @GetMapping("/list") public BaseResponse<List<ResourceEntity>> list() { return BaseResponse.success(resourceService.list()); }

    @GetMapping("/{id}") public BaseResponse<ResourceEntity> get(@PathVariable String id) { return BaseResponse.success(resourceService.get(id)); }

    @PostMapping public BaseResponse<ResourceEntity> create(@RequestBody ResourceEntity e) { return BaseResponse.success(resourceService.create(e)); }

    @PutMapping public BaseResponse<ResourceEntity> update(@RequestBody ResourceEntity e) { return BaseResponse.success(resourceService.update(e)); }

    @DeleteMapping("/{id}") public BaseResponse<Void> delete(@PathVariable String id) { resourceService.delete(id); return BaseResponse.success(); }
}

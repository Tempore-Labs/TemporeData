package org.temporedata.modules.ops.container.controller;

import org.temporedata.modules.ops.container.entity.ContainerEntity;
import org.temporedata.modules.ops.container.service.ContainerService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/container")
@RequiredArgsConstructor
public class ContainerController {
    private final ContainerService containerService;

    @GetMapping("/page") public BaseResponse<Page<ContainerEntity>> page(Pageable p) { return BaseResponse.success(containerService.page(p)); }

    @GetMapping("/list") public BaseResponse<List<ContainerEntity>> list() { return BaseResponse.success(containerService.list()); }

    @GetMapping("/{id}") public BaseResponse<ContainerEntity> get(@PathVariable String id) { return BaseResponse.success(containerService.get(id)); }

    @PostMapping public BaseResponse<ContainerEntity> create(@RequestBody ContainerEntity e) { return BaseResponse.success(containerService.create(e)); }

    @PutMapping public BaseResponse<ContainerEntity> update(@RequestBody ContainerEntity e) { return BaseResponse.success(containerService.update(e)); }

    @DeleteMapping("/{id}") public BaseResponse<Void> delete(@PathVariable String id) { containerService.delete(id); return BaseResponse.success(); }
}

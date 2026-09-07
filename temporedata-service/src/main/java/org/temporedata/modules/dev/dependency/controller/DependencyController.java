package org.temporedata.modules.dev.dependency.controller;

import org.temporedata.modules.dev.dependency.entity.DependencyEntity;
import org.temporedata.modules.dev.dependency.service.DependencyService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dependency")
@RequiredArgsConstructor
public class DependencyController {
    private final DependencyService dependencyService;

    @GetMapping("/page") public BaseResponse<Page<DependencyEntity>> page(Pageable p) { return BaseResponse.success(dependencyService.page(p)); }

    @GetMapping("/list") public BaseResponse<List<DependencyEntity>> list() { return BaseResponse.success(dependencyService.list()); }

    @GetMapping("/{id}") public BaseResponse<DependencyEntity> get(@PathVariable String id) { return BaseResponse.success(dependencyService.get(id)); }

    @PostMapping public BaseResponse<DependencyEntity> create(@RequestBody DependencyEntity e) { return BaseResponse.success(dependencyService.create(e)); }

    @PutMapping public BaseResponse<DependencyEntity> update(@RequestBody DependencyEntity e) { return BaseResponse.success(dependencyService.update(e)); }

    @DeleteMapping("/{id}") public BaseResponse<Void> delete(@PathVariable String id) { dependencyService.delete(id); return BaseResponse.success(); }
}

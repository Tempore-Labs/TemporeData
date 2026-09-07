package org.temporedata.modules.ops.ha.controller;

import org.temporedata.modules.ops.ha.entity.HaEntity;
import org.temporedata.modules.ops.ha.service.HaService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ha")
@RequiredArgsConstructor
public class HaController {
    private final HaService haService;

    @GetMapping("/page") public BaseResponse<Page<HaEntity>> page(Pageable p) { return BaseResponse.success(haService.page(p)); }

    @GetMapping("/list") public BaseResponse<List<HaEntity>> list() { return BaseResponse.success(haService.list()); }

    @GetMapping("/{id}") public BaseResponse<HaEntity> get(@PathVariable String id) { return BaseResponse.success(haService.get(id)); }

    @PostMapping public BaseResponse<HaEntity> create(@RequestBody HaEntity e) { return BaseResponse.success(haService.create(e)); }

    @PutMapping public BaseResponse<HaEntity> update(@RequestBody HaEntity e) { return BaseResponse.success(haService.update(e)); }

    @DeleteMapping("/{id}") public BaseResponse<Void> delete(@PathVariable String id) { haService.delete(id); return BaseResponse.success(); }
}

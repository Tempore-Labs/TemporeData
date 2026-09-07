package org.temporedata.modules.dev.func.controller;

import org.temporedata.modules.dev.func.entity.FuncEntity;
import org.temporedata.modules.dev.func.service.FuncService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/func")
@RequiredArgsConstructor
public class FuncController {

    private final FuncService funcService;

    @GetMapping("/page")
    public BaseResponse<Page<FuncEntity>> page(Pageable pageable) { return BaseResponse.success(funcService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<FuncEntity>> list() { return BaseResponse.success(funcService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<FuncEntity> get(@PathVariable String id) { return BaseResponse.success(funcService.get(id)); }

    @PostMapping
    public BaseResponse<FuncEntity> create(@RequestBody FuncEntity entity) { return BaseResponse.success(funcService.create(entity)); }

    @PutMapping
    public BaseResponse<FuncEntity> update(@RequestBody FuncEntity entity) { return BaseResponse.success(funcService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { funcService.delete(id); return BaseResponse.success(); }
}

package org.temporedata.modules.asset.mydata.controller;

import org.temporedata.modules.asset.mydata.entity.MydataEntity;
import org.temporedata.modules.asset.mydata.service.MydataService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mydata")
@RequiredArgsConstructor
public class MydataController {

    private final MydataService mydataService;

    @GetMapping("/page")
    public BaseResponse<Page<MydataEntity>> page(Pageable pageable) { return BaseResponse.success(mydataService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<MydataEntity>> list() { return BaseResponse.success(mydataService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<MydataEntity> get(@PathVariable String id) { return BaseResponse.success(mydataService.get(id)); }

    @PostMapping
    public BaseResponse<MydataEntity> create(@RequestBody MydataEntity entity) { return BaseResponse.success(mydataService.create(entity)); }

    @PutMapping
    public BaseResponse<MydataEntity> update(@RequestBody MydataEntity entity) { return BaseResponse.success(mydataService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { mydataService.delete(id); return BaseResponse.success(); }
}

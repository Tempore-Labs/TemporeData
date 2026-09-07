package org.temporedata.modules.dev.globalvar.controller;

import org.temporedata.modules.dev.globalvar.entity.GlobalvarEntity;
import org.temporedata.modules.dev.globalvar.service.GlobalvarService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/globalvar")
@RequiredArgsConstructor
public class GlobalvarController {
    private final GlobalvarService globalvarService;

    @GetMapping("/page") public BaseResponse<Page<GlobalvarEntity>> page(Pageable p) { return BaseResponse.success(globalvarService.page(p)); }

    @GetMapping("/list") public BaseResponse<List<GlobalvarEntity>> list() { return BaseResponse.success(globalvarService.list()); }

    @GetMapping("/{id}") public BaseResponse<GlobalvarEntity> get(@PathVariable String id) { return BaseResponse.success(globalvarService.get(id)); }

    @PostMapping public BaseResponse<GlobalvarEntity> create(@RequestBody GlobalvarEntity e) { return BaseResponse.success(globalvarService.create(e)); }

    @PutMapping public BaseResponse<GlobalvarEntity> update(@RequestBody GlobalvarEntity e) { return BaseResponse.success(globalvarService.update(e)); }

    @DeleteMapping("/{id}") public BaseResponse<Void> delete(@PathVariable String id) { globalvarService.delete(id); return BaseResponse.success(); }
}

package org.temporedata.modules.svc.blacklist.controller;

import org.temporedata.modules.svc.blacklist.entity.BlacklistEntity;
import org.temporedata.modules.svc.blacklist.service.BlacklistService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistService blacklistService;

    @GetMapping("/page")
    public BaseResponse<Page<BlacklistEntity>> page(Pageable pageable) { return BaseResponse.success(blacklistService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<BlacklistEntity>> list() { return BaseResponse.success(blacklistService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<BlacklistEntity> get(@PathVariable String id) { return BaseResponse.success(blacklistService.get(id)); }

    @PostMapping
    public BaseResponse<BlacklistEntity> create(@RequestBody BlacklistEntity entity) { return BaseResponse.success(blacklistService.create(entity)); }

    @PutMapping
    public BaseResponse<BlacklistEntity> update(@RequestBody BlacklistEntity entity) { return BaseResponse.success(blacklistService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { blacklistService.delete(id); return BaseResponse.success(); }
}

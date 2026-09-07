package org.temporedata.modules.ops.sync.controller;

import org.temporedata.modules.ops.sync.entity.SyncEntity;
import org.temporedata.modules.ops.sync.service.SyncService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @GetMapping
    public BaseResponse<List<SyncEntity>> list() {
        return BaseResponse.success(syncService.list());
    }

    @PostMapping
    public BaseResponse<SyncEntity> create(@RequestBody SyncEntity entity) {
        return BaseResponse.success(syncService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<SyncEntity> update(@PathVariable String id, @RequestBody SyncEntity entity) {
        return BaseResponse.success(syncService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        syncService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/execute")
    public BaseResponse<SyncEntity> execute(@PathVariable String id) {
        return BaseResponse.success(syncService.execute(id));
    }
}
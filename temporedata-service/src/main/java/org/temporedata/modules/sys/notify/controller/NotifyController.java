package org.temporedata.modules.sys.notify.controller;

import org.temporedata.modules.sys.notify.entity.NotifyEntity;
import org.temporedata.modules.sys.notify.service.NotifyService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    @GetMapping
    public BaseResponse<List<NotifyEntity>> list(@RequestParam(required = false) String tenantId) {
        return BaseResponse.success(notifyService.list(tenantId));
    }

    @PostMapping
    public BaseResponse<NotifyEntity> create(@RequestBody NotifyEntity entity) {
        return BaseResponse.success(notifyService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<NotifyEntity> update(@PathVariable String id, @RequestBody NotifyEntity entity) {
        return BaseResponse.success(notifyService.update(id, entity));
    }

    @PutMapping("/{id}/toggle")
    public BaseResponse<NotifyEntity> toggle(@PathVariable String id) {
        return BaseResponse.success(notifyService.toggle(id));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        notifyService.delete(id);
        return BaseResponse.success();
    }
}
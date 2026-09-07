package org.temporedata.modules.ops.monitor.controller;

import org.temporedata.modules.ops.monitor.entity.MonitorEntity;
import org.temporedata.modules.ops.monitor.service.MonitorService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {
    private final MonitorService monitorService;

    @GetMapping("/page") public BaseResponse<Page<MonitorEntity>> page(Pageable p) { return BaseResponse.success(monitorService.page(p)); }

    @GetMapping("/list") public BaseResponse<List<MonitorEntity>> list() { return BaseResponse.success(monitorService.list()); }

    @GetMapping("/{id}") public BaseResponse<MonitorEntity> get(@PathVariable String id) { return BaseResponse.success(monitorService.get(id)); }

    @PostMapping public BaseResponse<MonitorEntity> create(@RequestBody MonitorEntity e) { return BaseResponse.success(monitorService.create(e)); }

    @PutMapping public BaseResponse<MonitorEntity> update(@RequestBody MonitorEntity e) { return BaseResponse.success(monitorService.update(e)); }

    @DeleteMapping("/{id}") public BaseResponse<Void> delete(@PathVariable String id) { monitorService.delete(id); return BaseResponse.success(); }
}

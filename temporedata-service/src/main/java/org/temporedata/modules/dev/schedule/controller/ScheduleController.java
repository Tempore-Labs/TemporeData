package org.temporedata.modules.dev.schedule.controller;

import org.temporedata.modules.dev.schedule.entity.ScheduleEntity;
import org.temporedata.modules.dev.schedule.service.ScheduleService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/page")
    public BaseResponse<Page<ScheduleEntity>> page(Pageable pageable) { return BaseResponse.success(scheduleService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<ScheduleEntity>> list() { return BaseResponse.success(scheduleService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<ScheduleEntity> get(@PathVariable String id) { return BaseResponse.success(scheduleService.get(id)); }

    @PostMapping
    public BaseResponse<ScheduleEntity> create(@RequestBody ScheduleEntity entity) { return BaseResponse.success(scheduleService.create(entity)); }

    @PutMapping
    public BaseResponse<ScheduleEntity> update(@RequestBody ScheduleEntity entity) { return BaseResponse.success(scheduleService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { scheduleService.delete(id); return BaseResponse.success(); }
}

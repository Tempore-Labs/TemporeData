package org.temporedata.modules.ops.alarm.controller;

import org.temporedata.modules.ops.alarm.entity.AlarmEntity;
import org.temporedata.modules.ops.alarm.service.AlarmService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alarm/config")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping
    public BaseResponse<List<AlarmEntity>> list() {
        return BaseResponse.success(alarmService.list());
    }

    @PostMapping
    public BaseResponse<AlarmEntity> create(@RequestBody AlarmEntity entity) {
        return BaseResponse.success(alarmService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<AlarmEntity> update(@PathVariable String id, @RequestBody AlarmEntity entity) {
        return BaseResponse.success(alarmService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        alarmService.delete(id);
        return BaseResponse.success();
    }

    @GetMapping("/{id}")
    public BaseResponse<AlarmEntity> get(@PathVariable String id) {
        return BaseResponse.success(alarmService.get(id));
    }

    @GetMapping("/{id}/records")
    public BaseResponse<List<Map<String, Object>>> getRecords(@PathVariable String id) {
        return BaseResponse.success(alarmService.getRecords(id));
    }

    @PostMapping("/{id}/test")
    public BaseResponse<Map<String, Object>> test(@PathVariable String id) {
        return BaseResponse.success(alarmService.test(id));
    }
}
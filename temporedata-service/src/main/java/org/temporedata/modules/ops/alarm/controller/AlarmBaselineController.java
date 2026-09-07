package org.temporedata.modules.ops.alarm.controller;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.ops.alarm.entity.AlarmBaselineEntity;
import org.temporedata.modules.ops.alarm.entity.AlarmRecordEntity;
import org.temporedata.modules.ops.alarm.service.AlarmBaselineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * P2-8 baseline alarm REST API (基线与告警记录).
 */
@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmBaselineController {

    private final AlarmBaselineService alarmBaselineService;

    @GetMapping("/baselines")
    public BaseResponse<List<AlarmBaselineEntity>> list() {
        return BaseResponse.success(alarmBaselineService.list());
    }

    @PostMapping("/baselines")
    public BaseResponse<AlarmBaselineEntity> create(@RequestBody AlarmBaselineEntity b) {
        return BaseResponse.success(alarmBaselineService.create(b));
    }

    @PutMapping("/baselines/{id}")
    public BaseResponse<AlarmBaselineEntity> update(@PathVariable String id, @RequestBody AlarmBaselineEntity b) {
        return BaseResponse.success(alarmBaselineService.update(id, b));
    }

    @DeleteMapping("/baselines/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        alarmBaselineService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/baselines/{id}/enable")
    public BaseResponse<AlarmBaselineEntity> toggle(@PathVariable String id, @RequestParam boolean enabled) {
        return BaseResponse.success(alarmBaselineService.toggle(id, enabled));
    }

    @PostMapping("/baselines/{id}/test")
    public BaseResponse<String> test(@PathVariable String id) {
        return BaseResponse.success(alarmBaselineService.test(id));
    }

    @GetMapping("/records")
    public BaseResponse<List<AlarmRecordEntity>> records(
            @RequestParam(required = false) String baselineId,
            @RequestParam(required = false) String status) {
        return BaseResponse.success(alarmBaselineService.records(baselineId, status));
    }

    @PostMapping("/records/{id}/ack")
    public BaseResponse<AlarmRecordEntity> ack(@PathVariable String id) {
        return BaseResponse.success(alarmBaselineService.ack(id));
    }

    @PostMapping("/records/{id}/close")
    public BaseResponse<AlarmRecordEntity> close(@PathVariable String id) {
        return BaseResponse.success(alarmBaselineService.close(id));
    }

    @PostMapping("/run-check")
    public BaseResponse<Map<String, Object>> runCheck() {
        return BaseResponse.success(alarmBaselineService.checkBaselines());
    }
}
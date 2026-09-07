package org.temporedata.modules.dev.schedule.controller;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.dev.schedule.*;
import org.temporedata.modules.dev.schedule.service.TaskScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * P0-1 scheduler engine REST API. Standalone path to avoid clashing with the
 * legacy /api/schedule workflow scheduling controller.
 */
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class TaskScheduleController {

    private final TaskScheduleService taskScheduleService;

    // ---- Task definitions ----

    @GetMapping("/tasks")
    public BaseResponse<List<TaskDefineRes>> tasks() {
        return BaseResponse.success(taskScheduleService.list());
    }

    @GetMapping("/tasks/{id}")
    public BaseResponse<TaskDefineRes> task(@PathVariable String id) {
        return BaseResponse.success(taskScheduleService.get(id));
    }

    @PostMapping("/tasks")
    public BaseResponse<TaskDefineRes> create(@RequestBody TaskDefineReq req) {
        return BaseResponse.success(taskScheduleService.create(req));
    }

    @PutMapping("/tasks/{id}")
    public BaseResponse<TaskDefineRes> update(@PathVariable String id, @RequestBody TaskDefineReq req) {
        return BaseResponse.success(taskScheduleService.update(id, req));
    }

    @DeleteMapping("/tasks/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        taskScheduleService.delete(id);
        return BaseResponse.success();
    }

    @PutMapping("/tasks/{id}/enabled")
    public BaseResponse<TaskDefineRes> setEnabled(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        return BaseResponse.success(taskScheduleService.setEnabled(id, enabled));
    }

    // ---- Trigger & instances ----

    @PostMapping("/tasks/{id}/trigger")
    public BaseResponse<TaskInstanceRes> trigger(@PathVariable String id) {
        return BaseResponse.success(taskScheduleService.manualTrigger(id));
    }

    @GetMapping("/tasks/{id}/instances")
    public BaseResponse<List<TaskInstanceRes>> instances(@PathVariable String id) {
        return BaseResponse.success(taskScheduleService.instances(id));
    }

    @GetMapping("/instances/{instanceId}/logs")
    public BaseResponse<List<TaskLogRes>> logs(@PathVariable String instanceId) {
        return BaseResponse.success(taskScheduleService.logs(instanceId));
    }

    // ---- P1-4: business date preview & backfill ----

    @GetMapping("/tasks/{id}/bizdates")
    public BaseResponse<List<BizDatePreviewItem>> bizDates(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(taskScheduleService.bizDatePreview(id, from, to));
    }

    @PostMapping("/tasks/{id}/backfill")
    public BaseResponse<Integer> backfill(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(taskScheduleService.backfill(id, from, to));
    }
}
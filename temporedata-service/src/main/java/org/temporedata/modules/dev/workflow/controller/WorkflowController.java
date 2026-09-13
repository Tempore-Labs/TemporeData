package org.temporedata.modules.dev.workflow.controller;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.dev.workflow.*;
import org.temporedata.modules.dev.workflow.runner.WorkflowRunner;
import org.temporedata.modules.dev.workflow.service.WorkflowRuntimeService;
import org.temporedata.modules.dev.workflow.service.WorkflowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowRunner workflowRunner;
    private final WorkflowRuntimeService runtimeService;

    @GetMapping
    public BaseResponse<List<WorkflowRes>> list() {
        return BaseResponse.success(workflowService.list());
    }

    @GetMapping("/{id}")
    public BaseResponse<WorkflowRes> get(@PathVariable String id) {
        return BaseResponse.success(workflowService.get(id));
    }

    @PostMapping
    public BaseResponse<WorkflowRes> create(@RequestBody WorkflowReq req) {
        return BaseResponse.success(workflowService.create(req));
    }

    @PutMapping("/{id}")
    public BaseResponse<WorkflowRes> update(@PathVariable String id, @RequestBody WorkflowReq req) {
        return BaseResponse.success(workflowService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        workflowService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/execute")
    public BaseResponse<WorkflowExecuteRes> execute(@PathVariable String id) {
        return BaseResponse.success(workflowService.execute(id));
    }

    @GetMapping("/{id}/versions")
    public BaseResponse<List<org.temporedata.modules.dev.workflow.entity.WorkflowVersionEntity>> versions(@PathVariable String id) {
        return BaseResponse.success(workflowService.versions(id));
    }

    @PostMapping("/{id}/versions/{versionId}/rollback")
    public BaseResponse<WorkflowRes> rollback(@PathVariable String id, @PathVariable String versionId) {
        return BaseResponse.success(workflowService.rollback(id, versionId));
    }

    @PostMapping("/{id}/online")
    public BaseResponse<WorkflowRes> online(@PathVariable String id) {
        return BaseResponse.success(workflowService.online(id));
    }

    @PostMapping("/{id}/offline")
    public BaseResponse<WorkflowRes> offline(@PathVariable String id) {
        return BaseResponse.success(workflowService.offline(id));
    }

    @PutMapping("/{id}/schedule")
    public BaseResponse<WorkflowRes> schedule(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String cronExpression = (String) body.get("cronExpression");
        Boolean enabled = body.containsKey("enabled") ? Boolean.valueOf(String.valueOf(body.get("enabled"))) : null;
        String policy = body.get("schedulePolicy") == null ? null : String.valueOf(body.get("schedulePolicy"));
        String missfire = body.get("scheduleMissfire") == null ? null : String.valueOf(body.get("scheduleMissfire"));
        return BaseResponse.success(workflowService.schedule(id, cronExpression, enabled, policy, missfire));
    }

    @GetMapping("/{id}/executions")
    public BaseResponse<List<WorkflowExecuteRes>> executions(@PathVariable String id) {
        return BaseResponse.success(workflowService.executions(id));
    }

    // ---- P0-2: real DAG runtime ----

    @PostMapping("/{id}/run")
    public BaseResponse<WorkflowRunRes> run(@PathVariable String id) {
        return BaseResponse.success(workflowRunner.run(id, "MANUAL", null));
    }

    @PostMapping("/{id}/backfill")
    public BaseResponse<Map<String, Object>> backfill(@PathVariable String id,
                                                      @RequestBody Map<String, Object> body) {
        String start = body.get("start") == null ? null : String.valueOf(body.get("start"));
        String end = body.get("end") == null ? null : String.valueOf(body.get("end"));
        String interval = body.get("interval") == null ? "DAILY" : String.valueOf(body.get("interval"));
        String concurrency = body.get("concurrency") == null ? "SERIAL" : String.valueOf(body.get("concurrency"));
        if (start == null || end == null) {
            throw new BusinessException("start and end are required");
        }
        return BaseResponse.success(runtimeService.backfill(id, start, end, interval, concurrency));
    }

    @GetMapping("/{id}/instances")
    public BaseResponse<List<WorkflowInstanceRes>> instances(@PathVariable String id) {
        return BaseResponse.success(workflowRunner.instances(id));
    }

    @GetMapping("/instances/{instanceId}")
    public BaseResponse<WorkflowInstanceRes> instanceDetail(@PathVariable String instanceId) {
        return BaseResponse.success(workflowRunner.instanceDetail(instanceId));
    }

    @GetMapping("/instances/{instanceId}/logs")
    public BaseResponse<List<InstanceLogRes>> logs(@PathVariable String instanceId) {
        return BaseResponse.success(workflowRunner.logs(instanceId));
    }

    // ---- P0-3: runtime management ----

    @PostMapping("/instances/{instanceId}/pause")
    public BaseResponse<Void> pause(@PathVariable String instanceId) {
        runtimeService.pause(instanceId, "admin");
        return BaseResponse.success();
    }

    @PostMapping("/instances/{instanceId}/resume")
    public BaseResponse<Void> resume(@PathVariable String instanceId) {
        runtimeService.resume(instanceId, "admin");
        return BaseResponse.success();
    }

    @PostMapping("/instances/{instanceId}/stop")
    public BaseResponse<Void> stop(@PathVariable String instanceId) {
        runtimeService.stop(instanceId, "admin");
        return BaseResponse.success();
    }

    @PostMapping("/instances/{instanceId}/force-success")
    public BaseResponse<Void> forceSuccess(@PathVariable String instanceId,
                                           @RequestBody(required = false) Map<String, String> body) {
        String nodeInstanceId = body == null ? null : body.get("nodeInstanceId");
        if (nodeInstanceId == null || nodeInstanceId.isBlank()) {
            throw new org.temporedata.api.base.exceptions.BusinessException("nodeInstanceId is required");
        }
        runtimeService.forceSuccess(instanceId, nodeInstanceId, "admin");
        return BaseResponse.success();
    }

    @PostMapping("/instances/{instanceId}/recover-failed")
    public BaseResponse<WorkflowRunRes> recoverFailed(@PathVariable String instanceId) {
        return BaseResponse.success(runtimeService.recoverFailed(instanceId, "admin"));
    }

    @PostMapping("/instances/{instanceId}/rerun")
    public BaseResponse<WorkflowRunRes> rerun(@PathVariable String instanceId, @RequestBody(required = false) Map<String, String> body) {
        String scope = body == null ? null : body.get("scope");
        String fromNodeId = body == null ? null : body.get("fromNodeId");
        return BaseResponse.success(runtimeService.rerun(instanceId, scope, fromNodeId, "admin"));
    }

    @PutMapping("/instances/{instanceId}/priority")
    public BaseResponse<Void> setPriority(@PathVariable String instanceId, @RequestBody Map<String, Integer> body) {
        runtimeService.setPriority(instanceId, body.getOrDefault("priority", 5));
        return BaseResponse.success();
    }

    @PutMapping("/instances/{instanceId}/pool")
    public BaseResponse<Void> setPool(@PathVariable String instanceId, @RequestBody Map<String, String> body) {
        runtimeService.setPool(instanceId, body.getOrDefault("pool", "default"));
        return BaseResponse.success();
    }

    @GetMapping("/instances/{instanceId}/commands")
    public BaseResponse<List<RuntimeCommandRes>> commands(@PathVariable String instanceId) {
        return BaseResponse.success(runtimeService.commands(instanceId));
    }
}
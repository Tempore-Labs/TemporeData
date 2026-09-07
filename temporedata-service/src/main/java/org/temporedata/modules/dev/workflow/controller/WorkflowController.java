package org.temporedata.modules.dev.workflow.controller;

import lombok.RequiredArgsConstructor;
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

    @PutMapping("/{id}/schedule")
    public BaseResponse<WorkflowRes> schedule(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String cronExpression = (String) body.get("cronExpression");
        Boolean enabled = body.containsKey("enabled") ? Boolean.valueOf(String.valueOf(body.get("enabled"))) : null;
        return BaseResponse.success(workflowService.schedule(id, cronExpression, enabled));
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
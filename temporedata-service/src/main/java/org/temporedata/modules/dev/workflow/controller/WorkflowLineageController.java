package org.temporedata.modules.dev.workflow.controller;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.dev.workflow.SqlParseRes;
import org.temporedata.modules.dev.workflow.service.WorkflowLineageService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * P1-5 task-level SQL lineage REST API (IDE parse + workflow sync/graph).
 * Separate base path to avoid clashing with the existing lineage module.
 */
@RestController
@RequestMapping("/api/workflow-lineage")
@RequiredArgsConstructor
public class WorkflowLineageController {

    private final WorkflowLineageService workflowLineageService;

    @PostMapping("/parse")
    public BaseResponse<SqlParseRes> parse(@RequestBody Map<String, String> body) {
        String datasourceType = body.get("datasourceType");
        if (datasourceType == null || datasourceType.isBlank()) {
            return BaseResponse.success(workflowLineageService.parseSql(body.get("sql")));
        }
        return BaseResponse.success(workflowLineageService.parseSql(body.get("sql"), datasourceType));
    }

    @PostMapping("/validate")
    public BaseResponse<java.util.List<String>> validate(@RequestBody Map<String, String> body) {
        return BaseResponse.success(workflowLineageService.validateSql(body.get("sql"), body.get("datasourceType")));
    }

    @PostMapping("/{workflowId}/sync")
    public BaseResponse<Integer> sync(@PathVariable String workflowId) {
        return BaseResponse.success(workflowLineageService.sync(workflowId));
    }

    @GetMapping("/{workflowId}")
    public BaseResponse<Map<String, Object>> graph(@PathVariable String workflowId) {
        return BaseResponse.success(workflowLineageService.graph(workflowId));
    }
}
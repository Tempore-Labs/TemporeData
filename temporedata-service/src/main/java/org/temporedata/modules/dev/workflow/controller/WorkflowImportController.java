package org.temporedata.modules.dev.workflow.controller;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.dev.workflow.WorkflowReq;
import org.temporedata.api.dev.workflow.WorkflowRes;
import org.temporedata.modules.dev.workflow.service.WorkflowImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * P1-7 programmatic / tabular modeling: JSON (Python SDK) and Excel import/export.
 * Separate base path to avoid clashing with the main workflow controller.
 */
@RestController
@RequestMapping("/api/workflow-import")
@RequiredArgsConstructor
public class WorkflowImportController {

    private final WorkflowImportService workflowImportService;

    @PostMapping("/json")
    public BaseResponse<WorkflowRes> importJson(@RequestBody WorkflowReq req) {
        return BaseResponse.success(workflowImportService.importJson(req));
    }

    @GetMapping("/{id}/export-json")
    public BaseResponse<Map<String, Object>> exportJson(@PathVariable String id) {
        return BaseResponse.success(workflowImportService.exportJson(id));
    }

    @PostMapping("/excel")
    public BaseResponse<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        return BaseResponse.success(workflowImportService.importExcel(file));
    }

    @GetMapping("/{id}/export-excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable String id) {
        byte[] bytes = workflowImportService.exportExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=workflow_" + id + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
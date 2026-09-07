package org.temporedata.modules.dev.work.controller;

import org.temporedata.modules.dev.work.entity.WorkEntity;
import org.temporedata.modules.dev.work.service.WorkService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    /**
     * List all works.
     * GET /api/work/list
     */
    @GetMapping("/list")
    public BaseResponse<List<WorkEntity>> list() {
        return BaseResponse.success(workService.list());
    }

    /**
     * Paginated work list, optionally filtered by workflowId.
     * GET /api/work/page?workflowId=&page=0&pageSize=20
     */
    @GetMapping("/page")
    public BaseResponse<Page<WorkEntity>> page(
            @RequestParam(required = false) String workflowId,
            Pageable pageable) {
        return BaseResponse.success(workService.page(workflowId, pageable));
    }

    /**
     * Add a new work.
     * POST /api/work/add
     */
    @PostMapping("/add")
    public BaseResponse<WorkEntity> add(@RequestBody WorkEntity entity) {
        return BaseResponse.success(workService.add(entity));
    }

    /**
     * Update an existing work.
     * POST /api/work/update
     */
    @PostMapping("/update")
    public BaseResponse<WorkEntity> update(@RequestBody WorkEntity entity) {
        return BaseResponse.success(workService.update(entity));
    }

    /**
     * Delete a work by workId.
     * POST /api/work/delete
     * Request body: { "workId": "..." }
     */
    @PostMapping("/delete")
    public BaseResponse<Void> delete(@RequestBody Map<String, String> body) {
        String workId = body.get("workId");
        if (workId == null || workId.isBlank()) {
            return BaseResponse.error(400, "workId is required");
        }
        workService.delete(workId);
        return BaseResponse.success();
    }

    /**
     * Run / execute a work.
     * POST /api/work/run
     * Request body: { "workId": "..." }
     */
    @PostMapping("/run")
    public BaseResponse<WorkEntity> run(@RequestBody Map<String, String> body) {
        String workId = body.get("workId");
        if (workId == null || workId.isBlank()) {
            return BaseResponse.error(400, "workId is required");
        }
        return BaseResponse.success(workService.run(workId));
    }

    /**
     * Stop a running work.
     * POST /api/work/stop
     * Request body: { "workId": "..." }
     */
    @PostMapping("/stop")
    public BaseResponse<WorkEntity> stop(@RequestBody Map<String, String> body) {
        String workId = body.get("workId");
        if (workId == null || workId.isBlank()) {
            return BaseResponse.error(400, "workId is required");
        }
        return BaseResponse.success(workService.stop(workId));
    }

    /**
     * Get work detail by ID.
     * GET /api/work/{workId}
     */
    @GetMapping("/{workId}")
    public BaseResponse<WorkEntity> getDetail(@PathVariable String workId) {
        return BaseResponse.success(workService.get(workId));
    }

    /**
     * Copy a work.
     * POST /api/work/copy
     * Request body: { "workId": "..." }
     */
    @PostMapping("/copy")
    public BaseResponse<WorkEntity> copy(@RequestBody Map<String, String> body) {
        String workId = body.get("workId");
        if (workId == null || workId.isBlank()) {
            return BaseResponse.error(400, "workId is required");
        }
        return BaseResponse.success(workService.copy(workId));
    }

    /**
     * Top / pin a work.
     * POST /api/work/top
     * Request body: { "workId": "..." }
     */
    @PostMapping("/top")
    public BaseResponse<WorkEntity> top(@RequestBody Map<String, String> body) {
        String workId = body.get("workId");
        if (workId == null || workId.isBlank()) {
            return BaseResponse.error(400, "workId is required");
        }
        return BaseResponse.success(workService.top(workId));
    }

    /**
     * Get current status of a work.
     * GET /api/work/status?workId=
     */
    @GetMapping("/status")
    public BaseResponse<Map<String, Object>> status(@RequestParam String workId) {
        return BaseResponse.success(workService.status(workId));
    }

    /**
     * Get instance list for a work.
     * GET /api/work/instances?workId=
     */
    @GetMapping("/instances")
    public BaseResponse<List<Map<String, Object>>> instances(@RequestParam String workId) {
        return BaseResponse.success(workService.instances(workId));
    }
}
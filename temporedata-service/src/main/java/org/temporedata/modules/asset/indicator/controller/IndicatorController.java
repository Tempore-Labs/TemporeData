package org.temporedata.modules.asset.indicator.controller;

import org.temporedata.modules.asset.indicator.entity.IndicatorEntity;
import org.temporedata.modules.asset.indicator.service.IndicatorService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/indicator")
@RequiredArgsConstructor
public class IndicatorController {

    private final IndicatorService indicatorService;

    /**
     * Paginated indicator list.
     * GET /api/indicator/list?page=0&size=20
     */
    @GetMapping("/list")
    public BaseResponse<Page<IndicatorEntity>> list(Pageable pageable) {
        return BaseResponse.success(indicatorService.page(pageable));
    }

    /**
     * Indicator statistics.
     * GET /api/indicator/stats
     */
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> stats() {
        return BaseResponse.success(indicatorService.stats());
    }

    /**
     * Get indicator by ID.
     * GET /api/indicator/{id}
     */
    @GetMapping("/{id}")
    public BaseResponse<IndicatorEntity> get(@PathVariable String id) {
        return BaseResponse.success(indicatorService.get(id));
    }

    /**
     * Create a new indicator.
     * POST /api/indicator/create
     */
    @PostMapping("/create")
    public BaseResponse<IndicatorEntity> create(@RequestBody IndicatorEntity entity) {
        return BaseResponse.success(indicatorService.create(entity));
    }

    /**
     * Update an existing indicator.
     * PUT /api/indicator/{id}
     */
    @PutMapping("/{id}")
    public BaseResponse<IndicatorEntity> update(@PathVariable String id, @RequestBody IndicatorEntity entity) {
        return BaseResponse.success(indicatorService.update(id, entity));
    }

    /**
     * Delete an indicator.
     * DELETE /api/indicator/{id}
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        indicatorService.delete(id);
        return BaseResponse.success();
    }

    /**
     * Execute indicator calculation.
     * POST /api/indicator/{id}/execute
     */
    @PostMapping("/{id}/execute")
    public BaseResponse<IndicatorEntity> execute(@PathVariable String id) {
        return BaseResponse.success(indicatorService.execute(id));
    }

    /**
     * Get execution history for an indicator.
     * GET /api/indicator/{id}/runs
     */
    @GetMapping("/{id}/runs")
    public BaseResponse<List<Map<String, Object>>> getRuns(@PathVariable String id) {
        return BaseResponse.success(indicatorService.getRuns(id));
    }

    /**
     * Get indicator lineage (upstream/downstream).
     * GET /api/indicator/{id}/lineage
     */
    @GetMapping("/{id}/lineage")
    public BaseResponse<Map<String, Object>> getLineage(@PathVariable String id) {
        return BaseResponse.success(indicatorService.getLineage(id));
    }
}
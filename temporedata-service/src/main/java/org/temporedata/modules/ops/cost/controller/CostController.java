package org.temporedata.modules.ops.cost.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.ops.cost.entity.CostEntity;
import org.temporedata.modules.ops.cost.service.CostService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * [ENT-P2] FinOps cost REST endpoints.
 */
@RestController
@RequestMapping("/api/cost")
@RequiredArgsConstructor
public class CostController {

    private final CostService costService;

    @PostMapping("/record")
    public BaseResponse<CostEntity> record(@RequestParam Long datasetId,
                                           @RequestParam(required = false) BigDecimal compute,
                                           @RequestParam(required = false) BigDecimal storage,
                                           @RequestParam(required = false) BigDecimal query,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return BaseResponse.success(costService.record(datasetId, compute, storage, query, date));
    }

    @GetMapping("/dataset/{datasetId}")
    public BaseResponse<List<CostEntity>> list(@PathVariable Long datasetId) {
        return BaseResponse.success(costService.list(datasetId));
    }

    @GetMapping("/dataset/{datasetId}/latest")
    public BaseResponse<CostEntity> latest(@PathVariable Long datasetId) {
        return BaseResponse.success(costService.latest(datasetId));
    }

    @GetMapping("/summarize")
    public BaseResponse<CostService.CostSummary> summarize(@RequestParam Long datasetId,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(costService.summarize(datasetId, from, to));
    }
}
package org.temporedata.modules.gov.quality.controller;

import org.temporedata.modules.gov.quality.entity.QualityEntity;
import org.temporedata.modules.gov.quality.service.QualityService;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.gov.quality.QualityRunRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;

    @GetMapping("/api/quality")
    public BaseResponse<List<QualityEntity>> list() {
        return BaseResponse.success(qualityService.list());
    }

    @PostMapping("/api/quality")
    public BaseResponse<QualityEntity> create(@RequestBody QualityEntity entity) {
        return BaseResponse.success(qualityService.create(entity));
    }

    @PutMapping("/api/quality/{id}")
    public BaseResponse<QualityEntity> update(@PathVariable String id, @RequestBody QualityEntity entity) {
        return BaseResponse.success(qualityService.update(id, entity));
    }

    @DeleteMapping("/api/quality/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        qualityService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/api/quality/{id}/execute")
    public BaseResponse<QualityEntity> execute(@PathVariable String id) {
        return BaseResponse.success(qualityService.execute(id));
    }

    @PostMapping("/api/quality/{id}/run")
    public BaseResponse<QualityRunRes> run(@PathVariable String id) {
        return BaseResponse.success(qualityService.run(id));
    }

    @GetMapping("/api/audit/report")
    public BaseResponse<Map<String, Object>> getReport(@RequestParam(required = false) String datasourceId,
                                                       @RequestParam(required = false) String tableName) {
        return BaseResponse.success(qualityService.getReport(datasourceId, tableName));
    }

    @PostMapping("/api/audit/execute")
    public BaseResponse<List<QualityEntity>> batchExecute(@RequestParam(required = false) String datasourceId,
                                                          @RequestParam(required = false) String tableName) {
        return BaseResponse.success(qualityService.batchExecute(datasourceId, tableName));
    }
}
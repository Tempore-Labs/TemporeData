package org.temporedata.modules.svc.report.controller;

import org.temporedata.modules.svc.report.entity.ReportEntity;
import org.temporedata.modules.svc.report.service.ReportService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/list")
    public BaseResponse<List<ReportEntity>> list() {
        return BaseResponse.success(reportService.list());
    }

    @PostMapping("/create")
    public BaseResponse<ReportEntity> create(@RequestBody ReportEntity entity) {
        return BaseResponse.success(reportService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<ReportEntity> update(@PathVariable String id, @RequestBody ReportEntity entity) {
        return BaseResponse.success(reportService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        reportService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/publish")
    public BaseResponse<ReportEntity> publish(@PathVariable String id) {
        return BaseResponse.success(reportService.publish(id));
    }

    @PostMapping("/{id}/unpublish")
    public BaseResponse<ReportEntity> unpublish(@PathVariable String id) {
        return BaseResponse.success(reportService.unpublish(id));
    }
}
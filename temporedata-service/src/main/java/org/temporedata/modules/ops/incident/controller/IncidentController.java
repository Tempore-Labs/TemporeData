package org.temporedata.modules.ops.incident.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.ops.incident.entity.IncidentEntity;
import org.temporedata.modules.ops.incident.service.IncidentService;

import java.util.List;

/**
 * [ENT-P2] Incident &amp; RCA REST endpoints.
 */
@RestController
@RequestMapping("/api/incident")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping("/open")
    public BaseResponse<IncidentEntity> open(@RequestParam String title,
                                             @RequestParam(required = false) String sourceType,
                                             @RequestParam(required = false) Long datasetId,
                                             @RequestParam(required = false) String severity,
                                             @RequestParam(required = false) String rca) {
        return BaseResponse.success(incidentService.open(title, sourceType, datasetId, severity, rca));
    }

    @PutMapping("/{id}/status")
    public BaseResponse<IncidentEntity> status(@PathVariable Long id, @RequestParam String status) {
        return BaseResponse.success(incidentService.updateStatus(id, status));
    }

    @PutMapping("/{id}/rca")
    public BaseResponse<IncidentEntity> rca(@PathVariable Long id, @RequestParam String analysis) {
        return BaseResponse.success(incidentService.setRca(id, analysis));
    }

    @GetMapping("/{id}")
    public BaseResponse<IncidentEntity> get(@PathVariable Long id) {
        return incidentService.get(id)
                .map(BaseResponse::success)
                .orElseGet(() -> BaseResponse.error(404, "incident not found"));
    }

    @GetMapping("/dataset/{datasetId}")
    public BaseResponse<List<IncidentEntity>> byDataset(@PathVariable Long datasetId) {
        return BaseResponse.success(incidentService.listByDataset(datasetId));
    }

    @GetMapping("/status/{status}")
    public BaseResponse<List<IncidentEntity>> byStatus(@PathVariable String status) {
        return BaseResponse.success(incidentService.listByStatus(status));
    }

    @GetMapping("/open/count")
    public BaseResponse<Long> openCount() {
        return BaseResponse.success(incidentService.countOpen());
    }
}
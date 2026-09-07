package org.temporedata.modules.ops.dashboard.controller;

import org.temporedata.modules.ops.dashboard.entity.DashboardEntity;
import org.temporedata.modules.ops.dashboard.service.DashboardOverviewService;
import org.temporedata.modules.ops.dashboard.service.DashboardService;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.ops.dashboard.DashboardRes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardOverviewService dashboardOverviewService;

    @GetMapping
    public BaseResponse<DashboardRes> overview() { return BaseResponse.success(dashboardOverviewService.overview()); }

    @GetMapping("/page")
    public BaseResponse<Page<DashboardEntity>> page(Pageable pageable) { return BaseResponse.success(dashboardService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<DashboardEntity>> list() { return BaseResponse.success(dashboardService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<DashboardEntity> get(@PathVariable String id) { return BaseResponse.success(dashboardService.get(id)); }

    @PostMapping
    public BaseResponse<DashboardEntity> create(@RequestBody DashboardEntity entity) { return BaseResponse.success(dashboardService.create(entity)); }

    @PutMapping
    public BaseResponse<DashboardEntity> update(@RequestBody DashboardEntity entity) { return BaseResponse.success(dashboardService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { dashboardService.delete(id); return BaseResponse.success(); }
}

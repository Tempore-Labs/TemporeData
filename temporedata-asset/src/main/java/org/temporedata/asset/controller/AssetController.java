package org.temporedata.asset.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.asset.entity.AssetCatalogEntity;
import org.temporedata.asset.entity.MetricEntity;
import org.temporedata.asset.service.AssetService;

import java.util.List;

/**
 * [ENT-P0] Asset catalog &amp; metric REST endpoints.
 */
@RestController
@RequestMapping("/api/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    // ---- catalog ----

    @GetMapping("/catalog/page")
    public BaseResponse<Page<AssetCatalogEntity>> pageCatalog(Pageable pageable) {
        return BaseResponse.success(assetService.pageCatalog(pageable));
    }

    @GetMapping("/catalog/{datasetId}")
    public BaseResponse<AssetCatalogEntity> getCatalog(@PathVariable Long datasetId) {
        return assetService.getCatalog(datasetId)
                .map(BaseResponse::success)
                .orElseGet(() -> BaseResponse.error(404, "asset not found"));
    }

    @PostMapping("/catalog")
    public BaseResponse<AssetCatalogEntity> upsertCatalog(@RequestBody AssetCatalogEntity catalog) {
        return BaseResponse.success(assetService.upsertCatalog(catalog));
    }

    @GetMapping("/catalog/search")
    public BaseResponse<List<AssetCatalogEntity>> search(@RequestParam String tag) {
        return BaseResponse.success(assetService.searchByTag(tag));
    }

    @PostMapping("/catalog/{datasetId}/touch")
    public BaseResponse<AssetCatalogEntity> touch(@PathVariable Long datasetId) {
        return BaseResponse.success(assetService.touch(datasetId));
    }

    // ---- metrics ----

    @GetMapping("/metric/page")
    public BaseResponse<Page<MetricEntity>> pageMetrics(Pageable pageable) {
        return BaseResponse.success(assetService.pageMetrics(pageable));
    }

    @GetMapping("/metric/{code}")
    public BaseResponse<MetricEntity> getMetric(@PathVariable String code) {
        return assetService.getMetricByCode(code)
                .map(BaseResponse::success)
                .orElseGet(() -> BaseResponse.error(404, "metric not found"));
    }

    @PostMapping("/metric")
    public BaseResponse<MetricEntity> createMetric(@RequestBody MetricEntity metric) {
        return BaseResponse.success(assetService.createMetric(metric));
    }

    @PutMapping("/metric/{id}")
    public BaseResponse<MetricEntity> updateMetric(@PathVariable Long id, @RequestBody MetricEntity metric) {
        return BaseResponse.success(assetService.updateMetric(id, metric));
    }

    @DeleteMapping("/metric/{id}")
    public BaseResponse<Void> deleteMetric(@PathVariable Long id) {
        assetService.deleteMetric(id);
        return BaseResponse.success();
    }
}
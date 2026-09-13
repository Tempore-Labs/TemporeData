package org.temporedata.metadata.controller;

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
import org.temporedata.metadata.entity.DatasetEntity;
import org.temporedata.metadata.entity.MetadataVersionEntity;
import org.temporedata.metadata.service.DatasetService;
import org.temporedata.metadata.service.MetadataVersionService;

import java.util.List;

/**
 * [ENT-P0] Dataset &amp; schema-snapshot REST endpoints.
 */
@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;
    private final MetadataVersionService metadataVersionService;

    @GetMapping("/page")
    public BaseResponse<Page<DatasetEntity>> page(Pageable pageable) {
        return BaseResponse.success(datasetService.page(pageable.getPageNumber(), pageable.getPageSize()));
    }

    @GetMapping("/{id}")
    public BaseResponse<DatasetEntity> get(@PathVariable Long id) {
        return BaseResponse.success(datasetService.getById(id));
    }

    @GetMapping("/byCode/{code}")
    public BaseResponse<DatasetEntity> byCode(@PathVariable String code) {
        return datasetService.getByCode(code)
                .map(BaseResponse::success)
                .orElseGet(() -> BaseResponse.error(404, "dataset not found"));
    }

    @GetMapping("/domain/{domainId}")
    public BaseResponse<List<DatasetEntity>> byDomain(@PathVariable Long domainId) {
        return BaseResponse.success(datasetService.listByDomain(domainId));
    }

    @PostMapping
    public BaseResponse<DatasetEntity> create(@RequestBody DatasetEntity dataset) {
        return BaseResponse.success(datasetService.create(dataset));
    }

    @PutMapping("/{id}")
    public BaseResponse<DatasetEntity> update(@PathVariable Long id, @RequestBody DatasetEntity dataset) {
        return BaseResponse.success(datasetService.update(id, dataset));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        datasetService.delete(id);
        return BaseResponse.success();
    }

    /** Record a new schema snapshot for the dataset (version bumped per dataset). */
    @PostMapping("/{id}/versions")
    public BaseResponse<MetadataVersionEntity> snapshot(@PathVariable Long id,
                                                         @RequestParam String schemaJson,
                                                         @RequestParam(required = false) String driftType) {
        return BaseResponse.success(metadataVersionService.snapshot(id, schemaJson, driftType));
    }

    @GetMapping("/{id}/versions")
    public BaseResponse<List<MetadataVersionEntity>> versions(@PathVariable Long id) {
        return BaseResponse.success(metadataVersionService.list(id));
    }
}
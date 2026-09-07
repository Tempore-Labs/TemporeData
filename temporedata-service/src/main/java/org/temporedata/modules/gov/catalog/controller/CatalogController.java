package org.temporedata.modules.gov.catalog.controller;

import org.temporedata.modules.gov.catalog.entity.CatalogEntity;
import org.temporedata.modules.gov.catalog.service.CatalogService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public BaseResponse<List<CatalogEntity>> list() {
        return BaseResponse.success(catalogService.list());
    }

    @GetMapping("/{id}")
    public BaseResponse<CatalogEntity> get(@PathVariable String id) {
        return BaseResponse.success(catalogService.get(id));
    }

    @GetMapping("/tree")
    public BaseResponse<List<Map<String, Object>>> getTree() {
        return BaseResponse.success(catalogService.getTree());
    }

    @PostMapping("/sync-all")
    public BaseResponse<List<CatalogEntity>> syncAll() {
        return BaseResponse.success(catalogService.syncAll());
    }

    @GetMapping("/lineage/{id}")
    public BaseResponse<Map<String, Object>> getLineage(@PathVariable String id) {
        return BaseResponse.success(catalogService.getLineage(id));
    }

    @PutMapping("/comment")
    public BaseResponse<CatalogEntity> updateComment(@RequestBody Map<String, Object> body) {
        return BaseResponse.success(catalogService.updateComment(body));
    }

    @PutMapping("/governance")
    public BaseResponse<CatalogEntity> updateGovernance(@RequestBody Map<String, Object> body) {
        return BaseResponse.success(catalogService.updateGovernance(body));
    }
}
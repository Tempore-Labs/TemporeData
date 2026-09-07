package org.temporedata.modules.integration.datasource.controller;

import org.temporedata.api.integration.datasource.DatasourceReq;
import org.temporedata.api.integration.datasource.DatasourceRes;
import org.temporedata.modules.integration.datasource.service.DatasourceService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasource")
@RequiredArgsConstructor
public class DatasourceController {

    private final DatasourceService datasourceService;

    @GetMapping("/page")
    public BaseResponse<Page<DatasourceRes>> page(Pageable pageable) {
        return BaseResponse.success(datasourceService.page(pageable));
    }

    @GetMapping("/list")
    public BaseResponse<List<DatasourceRes>> list() {
        return BaseResponse.success(datasourceService.list());
    }

    /** Bare GET list — the frontend datasourceApi.list() calls GET /api/datasource. */
    @GetMapping
    public BaseResponse<List<DatasourceRes>> listAll() {
        return BaseResponse.success(datasourceService.list());
    }

    @GetMapping("/{id}")
    public BaseResponse<DatasourceRes> get(@PathVariable String id) {
        return BaseResponse.success(datasourceService.get(id));
    }

    /** Supported dialect types dictionary, consumed by the frontend type dropdown. */
    @GetMapping("/plugins/types")
    public BaseResponse<List<String>> pluginTypes() {
        return BaseResponse.success(datasourceService.pluginTypes());
    }

    /** Read-only list of all currently supported datasource plugins (built-in + uploaded). */
    @GetMapping("/plugins")
    public BaseResponse<List<org.temporedata.api.datasource.DatasourcePluginInfo>> plugins() {
        return BaseResponse.success(datasourceService.plugins());
    }

    @PostMapping
    public BaseResponse<DatasourceRes> create(@RequestBody DatasourceReq req) {
        return BaseResponse.success(datasourceService.create(req));
    }

    @PutMapping("/{id}")
    public BaseResponse<DatasourceRes> update(@PathVariable String id, @RequestBody DatasourceReq req) {
        return BaseResponse.success(datasourceService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        datasourceService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/test")
    public BaseResponse<Void> testConnection(@PathVariable String id) {
        datasourceService.testConnection(id);
        return BaseResponse.success();
    }
}
package org.temporedata.modules.integration.datasource.driver.controller;

import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.integration.datasource.driver.service.DriverPluginReq;
import org.temporedata.modules.integration.datasource.driver.service.DriverPluginRes;
import org.temporedata.modules.integration.datasource.driver.service.DriverPluginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Data source driver plugin management. The frontend "数据源插件" page.
 * (Route stays on /api/driver for backward compatibility with the old page.)
 */
@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverPluginController {

    private final DriverPluginService driverPluginService;

    @GetMapping
    public BaseResponse<List<DriverPluginRes>> list() {
        return BaseResponse.success(driverPluginService.list());
    }

    @PostMapping
    public BaseResponse<DriverPluginRes> create(@RequestBody DriverPluginReq req) {
        return BaseResponse.success(driverPluginService.create(req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        driverPluginService.delete(id);
        return BaseResponse.success();
    }
}
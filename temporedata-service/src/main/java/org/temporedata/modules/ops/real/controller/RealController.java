package org.temporedata.modules.ops.real.controller;

import org.temporedata.modules.ops.real.entity.RealEntity;
import org.temporedata.modules.ops.real.service.RealService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
public class RealController {

    private final RealService realService;

    @GetMapping
    public BaseResponse<List<RealEntity>> list() {
        return BaseResponse.success(realService.list());
    }

    @GetMapping("/{id}")
    public BaseResponse<RealEntity> get(@PathVariable String id) {
        return BaseResponse.success(realService.get(id));
    }

    @PostMapping
    public BaseResponse<RealEntity> create(@RequestBody RealEntity entity) {
        return BaseResponse.success(realService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<RealEntity> update(@PathVariable String id, @RequestBody RealEntity entity) {
        return BaseResponse.success(realService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        realService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/start")
    public BaseResponse<RealEntity> start(@PathVariable String id) {
        return BaseResponse.success(realService.start(id));
    }

    @PostMapping("/{id}/stop")
    public BaseResponse<RealEntity> stop(@PathVariable String id) {
        return BaseResponse.success(realService.stop(id));
    }

    @PostMapping("/{id}/savepoint")
    public BaseResponse<Map<String, Object>> savepoint(@PathVariable String id) {
        return BaseResponse.success(realService.savepoint(id));
    }

    @GetMapping("/{id}/logs")
    public BaseResponse<List<String>> getLogs(@PathVariable String id) {
        return BaseResponse.success(realService.getLogs(id));
    }
}
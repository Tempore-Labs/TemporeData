package org.temporedata.modules.ops.engine.controller;

import org.temporedata.modules.ops.engine.entity.EngineEntity;
import org.temporedata.modules.ops.engine.service.EngineService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/engine")
@RequiredArgsConstructor
public class EngineController {

    private final EngineService engineService;

    // ========== Spark ==========

    @GetMapping("/spark")
    public BaseResponse<List<EngineEntity>> listSpark() {
        return BaseResponse.success(engineService.listByType("SPARK"));
    }

    @PostMapping("/spark")
    public BaseResponse<EngineEntity> createSpark(@RequestBody EngineEntity entity) {
        entity.setType("SPARK");
        return BaseResponse.success(engineService.create(entity));
    }

    @PutMapping("/spark/{id}")
    public BaseResponse<EngineEntity> updateSpark(@PathVariable String id, @RequestBody EngineEntity entity) {
        entity.setType("SPARK");
        return BaseResponse.success(engineService.update(id, entity));
    }

    @DeleteMapping("/spark/{id}")
    public BaseResponse<Void> deleteSpark(@PathVariable String id) {
        engineService.delete(id);
        return BaseResponse.success();
    }

    // ========== Flink ==========

    @GetMapping("/flink")
    public BaseResponse<List<EngineEntity>> listFlink() {
        return BaseResponse.success(engineService.listByType("FLINK"));
    }

    @PostMapping("/flink")
    public BaseResponse<EngineEntity> createFlink(@RequestBody EngineEntity entity) {
        entity.setType("FLINK");
        return BaseResponse.success(engineService.create(entity));
    }

    @PutMapping("/flink/{id}")
    public BaseResponse<EngineEntity> updateFlink(@PathVariable String id, @RequestBody EngineEntity entity) {
        entity.setType("FLINK");
        return BaseResponse.success(engineService.update(id, entity));
    }

    @DeleteMapping("/flink/{id}")
    public BaseResponse<Void> deleteFlink(@PathVariable String id) {
        engineService.delete(id);
        return BaseResponse.success();
    }
}
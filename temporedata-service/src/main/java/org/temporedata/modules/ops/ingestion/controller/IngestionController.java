package org.temporedata.modules.ops.ingestion.controller;

import org.temporedata.modules.ops.ingestion.entity.IngestionEntity;
import org.temporedata.modules.ops.ingestion.service.IngestionService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @GetMapping
    public BaseResponse<List<IngestionEntity>> list() {
        return BaseResponse.success(ingestionService.list());
    }

    @PostMapping
    public BaseResponse<IngestionEntity> create(@RequestBody IngestionEntity entity) {
        return BaseResponse.success(ingestionService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<IngestionEntity> update(@PathVariable String id, @RequestBody IngestionEntity entity) {
        return BaseResponse.success(ingestionService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        ingestionService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/execute")
    public BaseResponse<IngestionEntity> execute(@PathVariable String id) {
        return BaseResponse.success(ingestionService.execute(id));
    }
}
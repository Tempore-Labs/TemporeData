package org.temporedata.modules.sys.view.controller;

import org.temporedata.modules.sys.view.entity.ViewEntity;
import org.temporedata.modules.sys.view.service.ViewService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/view")
@RequiredArgsConstructor
public class ViewController {

    private final ViewService viewService;

    @GetMapping
    public BaseResponse<List<ViewEntity>> list() {
        return BaseResponse.success(viewService.list());
    }

    @GetMapping("/{id}")
    public BaseResponse<ViewEntity> get(@PathVariable String id) {
        return BaseResponse.success(viewService.get(id));
    }

    @PostMapping
    public BaseResponse<ViewEntity> create(@RequestBody ViewEntity entity) {
        return BaseResponse.success(viewService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<ViewEntity> update(@PathVariable String id, @RequestBody ViewEntity entity) {
        return BaseResponse.success(viewService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        viewService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/publish")
    public BaseResponse<ViewEntity> publish(@PathVariable String id) {
        return BaseResponse.success(viewService.publish(id));
    }

    @PostMapping("/{id}/execute")
    public BaseResponse<String> execute(@PathVariable String id) {
        return BaseResponse.success(viewService.execute(id));
    }
}
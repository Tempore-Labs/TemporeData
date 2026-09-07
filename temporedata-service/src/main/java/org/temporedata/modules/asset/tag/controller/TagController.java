package org.temporedata.modules.asset.tag.controller;

import org.temporedata.modules.asset.tag.entity.TagEntity;
import org.temporedata.modules.asset.tag.service.TagService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/page")
    public BaseResponse<Page<TagEntity>> page(Pageable pageable) { return BaseResponse.success(tagService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<TagEntity>> list() { return BaseResponse.success(tagService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<TagEntity> get(@PathVariable String id) { return BaseResponse.success(tagService.get(id)); }

    @PostMapping
    public BaseResponse<TagEntity> create(@RequestBody TagEntity entity) { return BaseResponse.success(tagService.create(entity)); }

    @PutMapping
    public BaseResponse<TagEntity> update(@RequestBody TagEntity entity) { return BaseResponse.success(tagService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { tagService.delete(id); return BaseResponse.success(); }
}

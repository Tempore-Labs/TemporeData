package org.temporedata.modules.sys.org.controller;

import org.temporedata.modules.sys.org.entity.OrgEntity;
import org.temporedata.modules.sys.org.service.OrgService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    @GetMapping("/page")
    public BaseResponse<Page<OrgEntity>> page(Pageable pageable) { return BaseResponse.success(orgService.page(pageable)); }

    @GetMapping("/list")
    public BaseResponse<List<OrgEntity>> list() { return BaseResponse.success(orgService.list()); }

    @GetMapping("/{id}")
    public BaseResponse<OrgEntity> get(@PathVariable String id) { return BaseResponse.success(orgService.get(id)); }

    @PostMapping
    public BaseResponse<OrgEntity> create(@RequestBody OrgEntity entity) { return BaseResponse.success(orgService.create(entity)); }

    @PutMapping
    public BaseResponse<OrgEntity> update(@RequestBody OrgEntity entity) { return BaseResponse.success(orgService.update(entity)); }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) { orgService.delete(id); return BaseResponse.success(); }
}

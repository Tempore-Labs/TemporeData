package org.temporedata.modules.sys.preference.controller;

import org.temporedata.modules.sys.preference.entity.PreferenceEntity;
import org.temporedata.modules.sys.preference.service.PreferenceService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preference")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping
    public BaseResponse<List<PreferenceEntity>> getAll() {
        return BaseResponse.success(preferenceService.getAll());
    }

    @PostMapping
    public BaseResponse<PreferenceEntity> save(@RequestBody PreferenceEntity entity) {
        return BaseResponse.success(preferenceService.save(entity));
    }

    @PutMapping
    public BaseResponse<PreferenceEntity> update(@RequestBody PreferenceEntity entity) {
        return BaseResponse.success(preferenceService.update(entity));
    }

    @DeleteMapping
    public BaseResponse<Void> delete(@RequestParam String id) {
        preferenceService.delete(id);
        return BaseResponse.success();
    }
}
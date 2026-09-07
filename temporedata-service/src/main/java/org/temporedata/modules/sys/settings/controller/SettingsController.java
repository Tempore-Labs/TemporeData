package org.temporedata.modules.sys.settings.controller;

import org.temporedata.modules.sys.settings.entity.SettingsEntity;
import org.temporedata.modules.sys.settings.service.SettingsService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public BaseResponse<List<SettingsEntity>> getAll() {
        return BaseResponse.success(settingsService.getAll());
    }

    @GetMapping("/group/{groupName}")
    public BaseResponse<List<SettingsEntity>> getByGroup(@PathVariable String groupName) {
        return BaseResponse.success(settingsService.getByGroup(groupName));
    }

    @GetMapping("/{key}")
    public BaseResponse<SettingsEntity> getByKey(@PathVariable String key) {
        return BaseResponse.success(settingsService.getByKey(key));
    }

    @PostMapping("/group/{groupName}")
    public BaseResponse<Void> saveGroup(@PathVariable String groupName, @RequestBody List<SettingsEntity> settings) {
        settingsService.saveGroup(groupName, settings);
        return BaseResponse.success();
    }

    @PutMapping("/{key}")
    public BaseResponse<SettingsEntity> setValue(@PathVariable String key, @RequestBody SettingsEntity entity) {
        return BaseResponse.success(settingsService.setValue(key, entity));
    }

    @DeleteMapping("/{key}")
    public BaseResponse<Void> deleteByKey(@PathVariable String key) {
        settingsService.deleteByKey(key);
        return BaseResponse.success();
    }
}
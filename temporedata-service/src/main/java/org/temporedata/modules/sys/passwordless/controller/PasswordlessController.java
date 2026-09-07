package org.temporedata.modules.sys.passwordless.controller;

import org.temporedata.modules.sys.passwordless.entity.PasswordlessEntity;
import org.temporedata.modules.sys.passwordless.service.PasswordlessService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passwordless")
@RequiredArgsConstructor
public class PasswordlessController {

    private final PasswordlessService passwordlessService;

    @GetMapping("/config")
    public BaseResponse<List<PasswordlessEntity>> getConfig() {
        return BaseResponse.success(passwordlessService.getConfig());
    }

    @PostMapping("/config")
    public BaseResponse<PasswordlessEntity> saveConfig(@RequestBody PasswordlessEntity entity) {
        return BaseResponse.success(passwordlessService.saveConfig(entity));
    }

    @PostMapping("/send")
    public BaseResponse<String> send(@RequestBody Map<String, String> request) {
        return BaseResponse.success(passwordlessService.sendCode(request));
    }

    @PostMapping("/verify")
    public BaseResponse<Boolean> verify(@RequestBody Map<String, String> request) {
        return BaseResponse.success(passwordlessService.verify(request));
    }
}
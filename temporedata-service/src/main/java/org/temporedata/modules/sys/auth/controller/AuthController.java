package org.temporedata.modules.sys.auth.controller;

import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.sys.auth.LoginReq;
import org.temporedata.api.sys.auth.LoginRes;
import org.temporedata.modules.sys.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public BaseResponse<LoginRes> login(@RequestBody LoginReq req) {
        return BaseResponse.success(authService.login(req));
    }

    @PostMapping("/logout")
    public BaseResponse<Void> logout() {
        authService.logout();
        return BaseResponse.success();
    }
}
package org.temporedata.modules.sys.user.controller;

import org.temporedata.api.sys.account.ChangePasswordReq;
import org.temporedata.api.sys.account.ContactUpdateReq;
import org.temporedata.api.sys.account.UpdateProfileReq;
import org.temporedata.api.sys.auth.UserInfoRes;
import org.temporedata.api.sys.menu.MenuGroup;
import org.temporedata.api.sys.user.UserRes;
import org.temporedata.modules.sys.user.entity.UserEntity;
import org.temporedata.modules.sys.user.service.UserService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Safe user list (DTO, no password). */
    @GetMapping
    public BaseResponse<List<UserRes>> list() {
        return BaseResponse.success(userService.listRes());
    }

    /** Safe single user (DTO, no password). */
    @GetMapping("/{id}")
    public BaseResponse<UserRes> get(@PathVariable String id) {
        return BaseResponse.success(userService.getRes(id));
    }

    /** Current authenticated user identity (roles, permissions, super-admin flag, no password). */
    @GetMapping("/me")
    public BaseResponse<UserInfoRes> me() {
        return BaseResponse.success(userService.me());
    }

    /** Update own basic profile (nickname / phone / email). */
    @PutMapping("/me/profile")
    public BaseResponse<UserRes> updateProfile(@RequestBody UpdateProfileReq req) {
        return BaseResponse.success(userService.updateProfile(req));
    }

    /** Change own password (old password verification). */
    @PostMapping("/me/password")
    public BaseResponse<Void> changePassword(@RequestBody ChangePasswordReq req) {
        userService.changePassword(req);
        return BaseResponse.success();
    }

    /** Change own phone number. */
    @PutMapping("/me/phone")
    public BaseResponse<UserRes> changePhone(@RequestBody ContactUpdateReq req) {
        return BaseResponse.success(userService.changePhone(req));
    }

    /** Change own email address. */
    @PutMapping("/me/email")
    public BaseResponse<UserRes> changeEmail(@RequestBody ContactUpdateReq req) {
        return BaseResponse.success(userService.changeEmail(req));
    }

    /** Permission-driven sidebar navigation for the current user (v2.0 §19). */
    @GetMapping("/menus")
    public BaseResponse<List<MenuGroup>> menus() {
        return BaseResponse.success(userService.menus());
    }

    @PostMapping
    public BaseResponse<UserRes> create(@RequestBody UserEntity entity) {
        return BaseResponse.success(userService.toRes(userService.create(entity)));
    }

    @PutMapping("/{id}")
    public BaseResponse<UserRes> update(@PathVariable String id, @RequestBody UserEntity entity) {
        return BaseResponse.success(userService.toRes(userService.update(id, entity)));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return BaseResponse.success();
    }

    @PutMapping("/{id}/roles")
    public BaseResponse<Void> setRoles(@PathVariable String id, @RequestBody List<String> roleIds) {
        userService.setRoles(id, roleIds);
        return BaseResponse.success();
    }

    @GetMapping("/ping")
    public BaseResponse<Map<String, Object>> ping() {
        return BaseResponse.success(userService.ping());
    }
}
package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.service.UserService;
import com.xingguang.delivery.service.UserSettingService;
import com.xingguang.delivery.support.CurrentUser;
import com.xingguang.delivery.vo.AddressVO;
import com.xingguang.delivery.vo.FileVO;
import com.xingguang.delivery.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 用户中心（SRS 5.2.9）。 */
@Tag(name = "06 用户中心")
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;
    private final UserSettingService settingService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, UserSettingService settingService, CurrentUser currentUser) {
        this.userService = userService;
        this.settingService = settingService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "个人资料", description = "含钱包余额、会员等级与成长值进度；手机号脱敏")
    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.ok(userService.profile(currentUser.userId()));
    }

    @Operation(summary = "上传头像（C 端用户）", description = "multipart 字段名 file；biz 固定 avatar；返回 objectKey 与可访问 url。需登录。")
    @PostMapping("/upload")
    public Result<FileVO> upload(@RequestPart("file") MultipartFile file,
                                @RequestParam(defaultValue = "avatar") String biz) {
        return Result.ok(userService.upload(file, biz));
    }

    @Operation(summary = "绑定头像", description = "把已上传的 objectKey 绑定到当前登录用户；返回可访问 url")
    @PostMapping("/avatar")
    public Result<Map<String, Object>> avatar(@RequestParam String objectKey) {
        return Result.ok(userService.bindAvatar(currentUser.requireUserId(), objectKey));
    }

    @Operation(summary = "收货地址列表")
    @GetMapping("/addresses")
    public Result<List<AddressVO>> addresses() {
        return Result.ok(userService.addresses(currentUser.userId()));
    }

    @Operation(summary = "新增收货地址")
    @PostMapping("/addresses")
    public Result<AddressVO> addAddress(@RequestBody Map<String, Object> body) {
        return Result.ok(userService.addAddress(
                currentUser.userId(),
                str(body.get("contact")),
                str(body.get("phone")),
                str(body.get("detail")),
                str(body.get("tag")),
                Boolean.TRUE.equals(body.get("isDefault"))));
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/addresses/{addressId}/default")
    public Result<Void> setDefault(@PathVariable Long addressId) {
        userService.setDefault(currentUser.userId(), addressId);
        return Result.ok();
    }

    @Operation(summary = "修改收货地址")
    @PutMapping("/addresses/{addressId}")
    public Result<AddressVO> updateAddress(@PathVariable Long addressId, @RequestBody Map<String, Object> body) {
        return Result.ok(userService.updateAddress(
                currentUser.userId(), addressId,
                str(body.get("contact")), str(body.get("phone")),
                str(body.get("detail")), str(body.get("tag")),
                Boolean.TRUE.equals(body.get("isDefault"))));
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/addresses/{addressId}")
    public Result<Void> deleteAddress(@PathVariable Long addressId) {
        userService.removeAddress(currentUser.userId(), addressId);
        return Result.ok();
    }

    // ==================================================================
    // 偏好设置（SRS 9.11）：GET 返回默认值∪已保存值；PUT 增量合并；DELETE 重置
    // ==================================================================

    @Operation(summary = "读取偏好设置", description = "返回完整设置对象（默认值合并用户已保存值）")
    @GetMapping("/settings")
    public Result<Map<String, Object>> getSettings() {
        return Result.ok(settingService.get(currentUser.requireUserId()));
    }

    @Operation(summary = "更新偏好设置", description = "增量补丁：仅提交要修改的键，服务端白名单校验后合并写回，返回更新后的完整设置")
    @PutMapping("/settings")
    public Result<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> body) {
        return Result.ok(settingService.update(currentUser.requireUserId(), body));
    }

    @Operation(summary = "重置偏好设置", description = "全部恢复默认值（删除用户设置记录）")
    @DeleteMapping("/settings")
    public Result<Void> resetSettings() {
        settingService.reset(currentUser.requireUserId());
        return Result.ok();
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}

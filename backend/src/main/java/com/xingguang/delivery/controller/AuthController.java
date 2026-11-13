package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.dto.AuthDTO;
import com.xingguang.delivery.service.AuthService;
import com.xingguang.delivery.service.SmsCodeService;
import com.xingguang.delivery.support.CurrentUser;
import com.xingguang.delivery.vo.AuthVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/** C 端账号：注册 / 登录 / 会话校验 / 登出。 */
@Tag(name = "01 账号认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "注册", description = "用户名 + 邮箱 + 手机号（需短信验证）+ 密码；成功后直接返回令牌，免二次登录")
    @PostMapping("/register")
    public Result<AuthVO> register(@RequestBody AuthDTO.Register body) {
        return Result.ok(authService.register(
                body.getUsername(), body.getEmail(), body.getPhone(), body.getCode(), body.getPassword()));
    }

    @Operation(summary = "登录", description = "account 支持用户名或邮箱")
    @PostMapping("/login")
    public Result<AuthVO> login(@RequestBody AuthDTO.Login body) {
        return Result.ok(authService.login(body.getAccount(), body.getPassword()));
    }

    @Operation(summary = "发送短信验证码", description = "同一号码 60 秒内只能发一次；mock 模式下验证码固定为 8888 并在响应中回传")
    @PostMapping("/sms/code")
    public Result<SmsCodeService.SendResult> sendSmsCode(@RequestBody AuthDTO.SmsSend body) {
        return Result.ok(authService.sendSmsCode(body.getPhone()));
    }

    @Operation(summary = "手机验证码登录", description = "手机号未注册时自动建号，首次登录即完成注册")
    @PostMapping("/sms/login")
    public Result<AuthVO> loginBySms(@RequestBody AuthDTO.SmsLogin body) {
        return Result.ok(authService.loginBySms(body.getPhone(), body.getCode()));
    }

    @Operation(summary = "当前会话", description = "校验 Bearer 令牌并返回用户资料；令牌无效返回 11001")
    @GetMapping("/me")
    public Result<AuthVO> me() {
        return Result.ok(authService.current(currentUser.requireUserId()));
    }

    @Operation(summary = "登出", description = "无状态令牌：服务端不维护会话，客户端丢弃令牌即可")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}

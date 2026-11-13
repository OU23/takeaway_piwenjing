package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.PageResult;
import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.service.RiderService;
import com.xingguang.delivery.support.CurrentRider;
import com.xingguang.delivery.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 08 骑手端：接单大厅、抢单、取货、送达、任务、收入结算、消息。
 * 骑手身份通过请求头 {@code X-Rider-Id} 传递（原型默认 1）。
 */
@Tag(name = "08 骑手端")
@RestController
@RequestMapping("/api/v1/rider")
public class RiderController {

    private final RiderService riderService;
    private final CurrentRider currentRider;

    public RiderController(RiderService riderService, CurrentRider currentRider) {
        this.riderService = riderService;
        this.currentRider = currentRider;
    }

    @Operation(summary = "骑手登录", description = "手机号 + 演示验证码（默认 8888）；成功后返回 token（=rider.id），后续放入 X-Rider-Token 头")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> body) {
        return Result.ok(riderService.login(str(body.get("phone")), str(body.get("code"))));
    }

    @Operation(summary = "骑手档案")
    @GetMapping("/profile")
    public Result<RiderProfileVO> profile() {
        return Result.ok(riderService.profile(currentRider.riderId()));
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @Operation(summary = "上线 / 下线切换", description = "online=true 接单中，false 下班")
    @PostMapping("/profile/online")
    public Result<RiderProfileVO> online(@RequestParam(defaultValue = "true") boolean online) {
        return Result.ok(riderService.setOnline(currentRider.riderId(), online));
    }

    @Operation(summary = "接单大厅", description = "未派单且处于备餐中/待取货的配送单")
    @GetMapping("/hall")
    public Result<List<RiderTaskVO>> hall() {
        return Result.ok(riderService.hall(currentRider.riderId()));
    }

    @Operation(summary = "我的任务列表", description = "按分组：进行中 / 已完成")
    @GetMapping("/tasks")
    public Result<PageResult<RiderTaskVO>> tasks(@RequestParam(required = false) String group,
                                                 @RequestParam(required = false) Long pageNum,
                                                 @RequestParam(required = false) Long pageSize) {
        return Result.ok(riderService.myTasks(currentRider.riderId(), group, pageNum, pageSize));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/tasks/{orderNo}")
    public Result<OrderVO> taskDetail(@PathVariable String orderNo) {
        return Result.ok(riderService.taskDetail(currentRider.riderId(), orderNo));
    }

    @Operation(summary = "抢单 / 接单")
    @PostMapping("/tasks/{orderNo}/grab")
    public Result<RiderTaskVO> grab(@PathVariable String orderNo) {
        return Result.ok(riderService.grab(currentRider.riderId(), orderNo));
    }

    @Operation(summary = "取货", description = "WAIT_PICKUP → DELIVERING")
    @PostMapping("/tasks/{orderNo}/pickup")
    public Result<OrderVO> pickup(@PathVariable String orderNo) {
        return Result.ok(riderService.pickUp(currentRider.riderId(), orderNo));
    }

    @Operation(summary = "送达", description = "DELIVERING → DELIVERED，并结算骑手收入")
    @PostMapping("/tasks/{orderNo}/deliver")
    public Result<OrderVO> deliver(@PathVariable String orderNo) {
        return Result.ok(riderService.deliver(currentRider.riderId(), orderNo));
    }

    @Operation(summary = "收入与结算")
    @GetMapping("/income")
    public Result<RiderIncomeVO> income() {
        return Result.ok(riderService.income(currentRider.riderId()));
    }

    @Operation(summary = "消息列表")
    @GetMapping("/messages")
    public Result<List<RiderMessageVO>> messages() {
        return Result.ok(riderService.messages(currentRider.riderId()));
    }

    @Operation(summary = "标记消息已读")
    @PostMapping("/messages/{id}/read")
    public Result<Void> readMessage(@PathVariable Long id) {
        riderService.readMessage(currentRider.riderId(), id);
        return Result.ok();
    }
}

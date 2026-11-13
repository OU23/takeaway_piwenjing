package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.PageResult;
import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.dto.AdminLoginDTO;
import com.xingguang.delivery.entity.*;
import com.xingguang.delivery.service.AdminService;
import com.xingguang.delivery.support.CurrentAdmin;
import com.xingguang.delivery.support.TokenCodec;
import com.xingguang.delivery.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台运营后台（高价值优先：订单 / 商家 / 数据看板 + RBAC）。
 *
 * <p>读接口对所有运营角色开放；写接口（入驻审核 / 菜品上下架 / 异常 / 取消）需
 * SUPER_ADMIN / OPERATOR 角色，CS 仅只读（见 {@link CurrentAdmin#requireManage()}）。
 */
@Tag(name = "07 运营后台")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final CurrentAdmin currentAdmin;

    @org.springframework.beans.factory.annotation.Value("${xingguang.auth.secret:xg-dev-secret-change-me-in-production}")
    private String secret;

    @org.springframework.beans.factory.annotation.Value("${xingguang.auth.token-ttl-hours:168}")
    private long tokenTtlHours;

    public AdminController(AdminService adminService, CurrentAdmin currentAdmin) {
        this.adminService = adminService;
        this.currentAdmin = currentAdmin;
    }

    @Operation(summary = "运营账号登录", description = "成功后返回签名令牌（受众 ADMIN），后续请求放入 X-Admin-Token 头")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody AdminLoginDTO dto) {
        AdminUser u = adminService.login(dto.getUsername(), dto.getPassword());
        long exp = System.currentTimeMillis() / 1000L + tokenTtlHours * 3600L;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", TokenCodec.issue(u.getId(), TokenCodec.AUD_ADMIN, exp, secret));
        data.put("expiresAt", exp);
        data.put("name", u.getName());
        data.put("role", u.getRole());
        data.put("roleLabel", CurrentAdmin.roleLabel(u.getRole()));
        return Result.ok(data);
    }

    @Operation(summary = "工作台数据看板")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(adminService.overview());
    }

    @Operation(summary = "近 N 日趋势（营业额 / 订单量 / 平均配送时长）")
    @GetMapping("/dashboard/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(adminService.dashboardTrend(days));
    }

    @Operation(summary = "订单列表（只读）")
    @GetMapping("/orders")
    public Result<PageResult<OrderVO>> orders(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long pageNum,
                                              @RequestParam(required = false) Long pageSize) {
        return Result.ok(adminService.orders(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "订单详情（后台，含明细/时间轴/金额）")
    @GetMapping("/orders/{orderNo}")
    public Result<OrderVO> orderDetail(@PathVariable String orderNo) {
        return Result.ok(adminService.orderDetail(orderNo));
    }

    @Operation(summary = "标记异常订单（需管理角色）")
    @PostMapping("/orders/{orderNo}/exception")
    public Result<OrderVO> markException(@PathVariable String orderNo,
                                        @RequestParam(required = false) String note) {
        return Result.ok(adminService.markException(orderNo, note));
    }

    @Operation(summary = "后台取消订单（需管理角色）")
    @PostMapping("/orders/{orderNo}/cancel")
    public Result<OrderVO> adminCancel(@PathVariable String orderNo,
                                       @RequestParam(required = false) String reason) {
        return Result.ok(adminService.adminCancel(orderNo, reason));
    }

    @Operation(summary = "订单状态模拟推进")
    @PostMapping("/orders/{orderNo}/advance")
    public Result<OrderVO> advance(@PathVariable String orderNo) {
        return Result.ok(adminService.advance(orderNo));
    }

    @Operation(summary = "门店与商家列表（只读）")
    @GetMapping("/shops")
    public Result<java.util.List<Map<String, Object>>> shops() {
        return Result.ok(adminService.shops());
    }

    @Operation(summary = "门店商品（只读）")
    @GetMapping("/dishes")
    public Result<java.util.List<Dish>> dishes(@RequestParam(required = false) Long shopId) {
        return Result.ok(adminService.dishes(shopId));
    }

    @Operation(summary = "菜品上/下架切换（需管理角色）")
    @PostMapping("/dishes/{id}/toggle-status")
    public Result<Dish> toggleDishStatus(@PathVariable Long id) {
        return Result.ok(adminService.toggleDishStatus(id));
    }

    @Operation(summary = "评分监控（只读）")
    @GetMapping("/rating-monitor")
    public Result<java.util.List<Map<String, Object>>> ratingMonitor() {
        return Result.ok(adminService.ratingMonitor());
    }

    @Operation(summary = "商家入驻申请列表（只读）")
    @GetMapping("/merchant-applications")
    public Result<java.util.List<MerchantApplication>> applications(
            @RequestParam(required = false) String status) {
        return Result.ok(adminService.applications(status));
    }

    @Operation(summary = "入驻审核：通过（需管理角色）")
    @PostMapping("/merchant-applications/{id}/approve")
    public Result<MerchantApplication> approve(@PathVariable Long id) {
        return Result.ok(adminService.approveApplication(id));
    }

    @Operation(summary = "入驻审核：驳回（需管理角色）")
    @PostMapping("/merchant-applications/{id}/reject")
    public Result<MerchantApplication> reject(@PathVariable Long id,
                                             @RequestParam(required = false) String reason) {
        return Result.ok(adminService.rejectApplication(id, reason));
    }

    @Operation(summary = "骑手调度概览（只读）：待分配单 + 骑手池")
    @GetMapping("/dispatch/overview")
    public Result<Map<String, Object>> dispatchOverview() {
        return Result.ok(adminService.dispatchOverview());
    }

    @Operation(summary = "用户列表（只读）")
    @GetMapping("/users")
    public Result<PageResult<AppUser>> users(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long pageNum,
                                             @RequestParam(required = false) Long pageSize) {
        return Result.ok(adminService.users(keyword, pageNum, pageSize));
    }

    @Operation(summary = "投诉列表（只读）")
    @GetMapping("/complaints")
    public Result<PageResult<Map<String, Object>>> complaints(@RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String type,
                                                             @RequestParam(required = false) Long pageNum,
                                                             @RequestParam(required = false) Long pageSize) {
        return Result.ok(adminService.complaints(status, type, pageNum, pageSize));
    }

    @Operation(summary = "支付流水（只读）")
    @GetMapping("/payments")
    public Result<java.util.List<Payment>> payments(@RequestParam(required = false) Long pageNum,
                                                   @RequestParam(required = false) Long pageSize) {
        return Result.ok(adminService.payments(pageNum, pageSize));
    }
}

package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.PageResult;
import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.dto.OrderSubmitRequest;
import com.xingguang.delivery.service.OrderService;
import com.xingguang.delivery.support.CurrentUser;
import com.xingguang.delivery.vo.OrderPreviewVO;
import com.xingguang.delivery.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 结算与订单（SRS 5.2.5 / 5.2.6）。 */
@Tag(name = "04 订单", description = "试算 → 下单 → 列表 / 详情 → 取消 / 催单 / 确认")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUser currentUser;

    public OrderController(OrderService orderService, CurrentUser currentUser) {
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "结算试算",
            description = "切换配送 / 自取只重算费用与预计时间，不创建订单。金额由服务端唯一计算。")
    @PostMapping("/preview")
    public Result<OrderPreviewVO> preview(@Valid @RequestBody OrderSubmitRequest req) {
        return Result.ok(orderService.preview(currentUser.userId(), req));
    }

    @Operation(summary = "提交订单",
            description = "写接口需携带 Idempotency-Key，重复提交返回同一订单（幂等）")
    @PostMapping
    public Result<OrderVO> submit(@Valid @RequestBody OrderSubmitRequest req,
                                  @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
        return Result.ok(orderService.submit(currentUser.userId(), req, idemKey));
    }

    @Operation(summary = "订单列表", description = "group: 全部 / 待付款 / 待接单 / 进行中 / 待评价 / 退款售后")
    @GetMapping
    public Result<PageResult<OrderVO>> list(@RequestParam(required = false) String group,
                                            @RequestParam(required = false) Long pageNum,
                                            @RequestParam(required = false) Long pageSize) {
        return Result.ok(orderService.list(currentUser.userId(), group, pageNum, pageSize));
    }

    @Operation(summary = "各分组订单数（Tab 角标）")
    @GetMapping("/group-count")
    public Result<Map<String, Long>> groupCount() {
        return Result.ok(orderService.groupCount(currentUser.userId()));
    }

    @Operation(summary = "订单详情", description = "含商品明细、金额构成与完整状态时间轴")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.detail(currentUser.userId(), orderNo));
    }

    @Operation(summary = "取消订单", description = "仅待支付 / 待接单 / 备餐中可取消")
    @PostMapping("/{orderNo}/cancel")
    public Result<OrderVO> cancel(@PathVariable String orderNo,
                                  @RequestParam(required = false) String reason) {
        return Result.ok(orderService.cancel(currentUser.userId(), orderNo, reason));
    }

    @Operation(summary = "催单", description = "限频 60 秒")
    @PostMapping("/{orderNo}/urge")
    public Result<OrderVO> urge(@PathVariable String orderNo) {
        return Result.ok(orderService.urge(currentUser.userId(), orderNo));
    }

    @Operation(summary = "确认收货", description = "配送单需已送达")
    @PostMapping("/{orderNo}/confirm")
    public Result<OrderVO> confirm(@PathVariable String orderNo) {
        return Result.ok(orderService.confirm(currentUser.userId(), orderNo));
    }

    @Operation(summary = "我已取餐", description = "到店自取单核销（商家扫码 / 输码）")
    @PostMapping("/{orderNo}/pickup")
    public Result<OrderVO> pickup(@PathVariable String orderNo) {
        return Result.ok(orderService.pickUp(currentUser.userId(), orderNo));
    }
}

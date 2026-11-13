package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.dto.AdminLoginDTO;
import com.xingguang.delivery.entity.Dish;
import com.xingguang.delivery.service.MerchantService;
import com.xingguang.delivery.support.CurrentMerchant;
import com.xingguang.delivery.vo.FileVO;
import com.xingguang.delivery.vo.OrderVO;
import com.xingguang.delivery.vo.ShopVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 商家端（v1）：独立账号登录、我的门店、菜品、接单改状态、数据看板。 */
@Tag(name = "08 商家端")
@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantController {

    private final MerchantService merchantService;
    private final CurrentMerchant currentMerchant;
    private final com.xingguang.delivery.service.ReviewService reviewService;

    public MerchantController(MerchantService merchantService, CurrentMerchant currentMerchant,
                              com.xingguang.delivery.service.ReviewService reviewService) {
        this.merchantService = merchantService;
        this.currentMerchant = currentMerchant;
        this.reviewService = reviewService;
    }

    @Operation(summary = "商家账号登录", description = "成功后返回 token（=merchant_user.id），后续放入 X-Merchant-Token 头")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody AdminLoginDTO dto) {
        return Result.ok(merchantService.login(dto.getUsername(), dto.getPassword()));
    }

    @Operation(summary = "我的门店")
    @GetMapping("/shop")
    public Result<ShopVO> myShop() {
        return Result.ok(merchantService.myShop());
    }

    @Operation(summary = "更新门店信息 / 营业开关（status: OPEN/CLOSED/REST）")
    @PutMapping("/shop")
    public Result<ShopVO> updateShop(@RequestBody Map<String, Object> patch) {
        return Result.ok(merchantService.updateShop(patch));
    }

    @Operation(summary = "我的菜品")
    @GetMapping("/dishes")
    public Result<List<Dish>> myDishes() {
        return Result.ok(merchantService.myDishes());
    }

    @Operation(summary = "菜品上/下架切换")
    @PostMapping("/dishes/{id}/toggle-status")
    public Result<Dish> toggleDishStatus(@PathVariable Long id) {
        return Result.ok(merchantService.toggleDishStatus(id));
    }

    @Operation(summary = "菜品改价（price 单价分，originPrice 划线价分，可空）")
    @PutMapping("/dishes/{id}")
    public Result<Dish> updateDish(@PathVariable Long id,
                                  @RequestParam Integer price,
                                  @RequestParam(required = false) Integer originPrice) {
        return Result.ok(merchantService.updateDish(id, price, originPrice));
    }

    @Operation(summary = "订单列表（group: 待付款/待接单/进行中/待评价/退款售后；status 精确状态）")
    @GetMapping("/orders")
    public Result<Object> orders(@RequestParam(required = false) String group,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) Long pageNum,
                                @RequestParam(required = false) Long pageSize) {
        return Result.ok(merchantService.orders(group, status, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{orderNo}")
    public Result<OrderVO> orderDetail(@PathVariable String orderNo) {
        return Result.ok(merchantService.orderDetail(orderNo));
    }

    @Operation(summary = "商家接单（待接单 → 备餐中）")
    @PostMapping("/orders/{orderNo}/accept")
    public Result<OrderVO> acceptOrder(@PathVariable String orderNo) {
        return Result.ok(merchantService.acceptOrder(orderNo));
    }

    @Operation(summary = "商家出餐（备餐中 → 待取货/待取餐）")
    @PostMapping("/orders/{orderNo}/prepare")
    public Result<OrderVO> prepareOrder(@PathVariable String orderNo) {
        return Result.ok(merchantService.prepareOrder(orderNo));
    }

    @Operation(summary = "商家完成（交付骑手/顾客）")
    @PostMapping("/orders/{orderNo}/complete")
    @Deprecated
    public Result<OrderVO> completeOrder(@PathVariable String orderNo) {
        return Result.ok(merchantService.completeOrder(orderNo));
    }

    @Operation(summary = "数据看板（今日订单/营业额）")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(merchantService.dashboard());
    }

    @Operation(summary = "上传图片（商家端）", description = "multipart 字段名 file；biz: shop 门店Logo / dish 菜品图；返回 objectKey 与可访问 url。需登录。")
    @PostMapping("/upload")
    public Result<FileVO> upload(@RequestPart("file") MultipartFile file,
                                 @RequestParam(defaultValue = "shop") String biz) {
        currentMerchant.id();
        if (!List.of("shop", "dish", "header").contains(biz)) {
            throw new BizException(20006, "不支持的上传业务类型：" + biz + "（仅 shop / dish / header）");
        }
        return Result.ok(merchantService.upload(file, biz));
    }

    @Operation(summary = "绑定门店 Logo", description = "仅可绑定当前商家自己的门店")
    @PostMapping("/shop/logo")
    public Result<Map<String, Object>> bindShopLogo(@RequestParam String objectKey) {
        return Result.ok(merchantService.bindShopLogo(objectKey));
    }

    @Operation(summary = "绑定菜品图片", description = "仅可绑定当前商家门店下的菜品（越权返回 10003）")
    @PostMapping("/dishes/{id}/image")
    public Result<Map<String, Object>> bindDishImage(@PathVariable Long id,
                                                    @RequestParam String objectKey) {
        return Result.ok(merchantService.bindDishImage(id, objectKey));
    }

    @Operation(summary = "绑定门店头图", description = "仅可绑定当前商家自己的门店")
    @PostMapping("/shop/header")
    public Result<Map<String, Object>> bindShopHeaderImg(@RequestParam String objectKey) {
        return Result.ok(merchantService.bindShopHeaderImg(objectKey));
    }

    @Operation(summary = "本店评价列表", description = "含平均分与本店全部评价（按时间倒序）")
    @GetMapping("/reviews")
    public Result<Map<String, Object>> reviews() {
        return Result.ok(reviewService.listByShop(currentMerchant.shopId()));
    }

    @Operation(summary = "解析对象存储地址（objectKey → 可访问 url）", description = "供前端回显已上传图片")
    @GetMapping("/oss/url")
    public Result<Map<String, Object>> ossUrl(@RequestParam String key) {
        return Result.ok(merchantService.resolveUrl(key));
    }
}

package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.FulfillType;
import com.xingguang.delivery.common.OrderStatus;
import com.xingguang.delivery.common.PayChannel;
import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.service.ShopQueryService;
import com.xingguang.delivery.vo.CategoryVO;
import com.xingguang.delivery.vo.HomeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 首页与字典。首页内容全部由后台数据驱动（SRS 4.9 / 5.2.2）。 */
@Tag(name = "01 首页", description = "首页聚合、品类、字典")
@RestController
@RequestMapping("/api/v1")
public class HomeController {

    private final ShopQueryService shopQueryService;

    public HomeController(ShopQueryService shopQueryService) {
        this.shopQueryService = shopQueryService;
    }

    @Operation(summary = "首页聚合数据", description = "轮播 + 金刚区品类 + 秒杀 + 附近好店")
    @GetMapping("/home")
    public Result<HomeVO> home() {
        return Result.ok(shopQueryService.home());
    }

    @Operation(summary = "平台品类（金刚区）")
    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(shopQueryService.home().getCategories());
    }

    @Operation(summary = "字典（订单状态 / 履约方式 / 支付渠道）")
    @GetMapping("/meta/dict")
    public Result<Map<String, Object>> dict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderStatus", OrderStatus.dict());
        Map<String, String> fulfill = new LinkedHashMap<>();
        for (FulfillType t : FulfillType.values()) {
            fulfill.put(t.name(), t.getLabel());
        }
        map.put("fulfillType", fulfill);
        Map<String, String> pay = new LinkedHashMap<>();
        for (PayChannel c : PayChannel.values()) {
            pay.put(c.name(), c.getLabel());
        }
        map.put("payChannel", pay);
        map.put("orderGroup", List.of("全部", "待付款", "待接单", "进行中", "待评价", "退款售后"));
        return Result.ok(map);
    }
}

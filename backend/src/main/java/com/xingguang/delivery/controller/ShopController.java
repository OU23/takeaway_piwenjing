package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.service.ShopQueryService;
import com.xingguang.delivery.vo.DishVO;
import com.xingguang.delivery.vo.ShopDetailVO;
import com.xingguang.delivery.vo.ShopVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 门店与商品（SRS 5.2.3 / 5.2.4）。 */
@Tag(name = "02 门店与商品")
@RestController
@RequestMapping("/api/v1")
public class ShopController {

    private final ShopQueryService shopQueryService;

    public ShopController(ShopQueryService shopQueryService) {
        this.shopQueryService = shopQueryService;
    }

    @Operation(summary = "门店列表", description = "sort: recommend / distance / sales / rating / minprice；传 lng/lat 时按距离排序真实生效")
    @GetMapping("/shops")
    public Result<List<ShopVO>> shops(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false, defaultValue = "recommend") String sort,
                                      @RequestParam(required = false) Double lng,
                                      @RequestParam(required = false) Double lat) {
        return Result.ok(shopQueryService.list(keyword, sort, lng, lat));
    }

    @Operation(summary = "附近门店", description = "按经纬度返回半径内的门店（默认 5km），按距离升序；distanceKm 已计算")
    @GetMapping("/shops/nearby")
    public Result<List<ShopVO>> nearby(@RequestParam double lng,
                                       @RequestParam double lat,
                                       @RequestParam(required = false) Double radiusKm) {
        return Result.ok(shopQueryService.nearby(lng, lat, radiusKm));
    }

    @Operation(summary = "门店详情", description = "含店内分类与商品（左分类右商品联动）")
    @GetMapping("/shops/{shopId}")
    public Result<ShopDetailVO> detail(@PathVariable Long shopId) {
        return Result.ok(shopQueryService.detail(shopId));
    }

    @Operation(summary = "商品详情与规格组", description = "用于规格 / 加料弹窗")
    @GetMapping("/dishes/{dishId}")
    public Result<DishVO> dish(@PathVariable Long dishId) {
        return Result.ok(shopQueryService.dish(dishId));
    }
}

package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.dto.AddCartRequest;
import com.xingguang.delivery.dto.CartUpdateRequest;
import com.xingguang.delivery.service.CartService;
import com.xingguang.delivery.support.CurrentUser;
import com.xingguang.delivery.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 购物车（SRS 5.2.5）。 */
@Tag(name = "03 购物车")
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    public CartController(CartService cartService, CurrentUser currentUser) {
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "查询某门店购物车")
    @GetMapping
    public Result<CartVO> cart(@RequestParam Long shopId) {
        return Result.ok(cartService.getCart(currentUser.userId(), shopId));
    }

    @Operation(summary = "购物车总件数（Tab 角标）")
    @GetMapping("/count")
    public Result<Integer> count() {
        return Result.ok(cartService.count(currentUser.userId()));
    }

    @Operation(summary = "加入购物车", description = "同商品 + 同规格自动合并")
    @PostMapping("/items")
    public Result<CartVO> add(@Valid @RequestBody AddCartRequest req) {
        return Result.ok(cartService.addItem(currentUser.userId(), req));
    }

    @Operation(summary = "修改数量", description = "quantity=0 表示删除该条目")
    @PutMapping("/items/{itemId}")
    public Result<Void> update(@PathVariable Long itemId, @Valid @RequestBody CartUpdateRequest req) {
        cartService.updateQuantity(currentUser.userId(), itemId, req.getQuantity());
        return Result.ok();
    }

    @Operation(summary = "删除条目")
    @DeleteMapping("/items/{itemId}")
    public Result<Void> remove(@PathVariable Long itemId) {
        cartService.remove(currentUser.userId(), itemId);
        return Result.ok();
    }

    @Operation(summary = "清空某门店购物车")
    @DeleteMapping
    public Result<Void> clear(@RequestParam Long shopId) {
        cartService.clear(currentUser.userId(), shopId);
        return Result.ok();
    }
}

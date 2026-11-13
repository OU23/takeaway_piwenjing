package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.service.ReviewService;
import com.xingguang.delivery.support.CurrentUser;
import com.xingguang.delivery.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 订单评价（C 端）：提交 / 我的评价 / 查询订单评价 / 门店评价。 */
@Tag(name = "09 评价")
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUser currentUser;

    public ReviewController(ReviewService reviewService, CurrentUser currentUser) {
        this.reviewService = reviewService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "提交评价", description = "订单完成后可评一次；rating 1-5，images 为 objectKey 数组（最多 9 张）")
    @PostMapping
    public Result<ReviewVO> create(@RequestBody Map<String, Object> body) {
        long uid = currentUser.requireUserId();
        String orderNo = str(body.get("orderNo"));
        Integer rating = body.get("rating") == null ? null
                : Integer.valueOf(String.valueOf(body.get("rating")));
        String content = str(body.get("content"));
        List<String> images = toList(body.get("images"));
        return Result.ok(reviewService.create(uid, orderNo, rating, content, images));
    }

    @Operation(summary = "我的评价")
    @GetMapping("/mine")
    public Result<List<ReviewVO>> mine() {
        return Result.ok(reviewService.listMine(currentUser.requireUserId()));
    }

    @Operation(summary = "查询订单的评价", description = "未评价返回空，前端据此显示「去评价 / 已评价」")
    @GetMapping("/order/{orderNo}")
    public Result<ReviewVO> byOrder(@PathVariable String orderNo) {
        return Result.ok(reviewService.findByOrder(currentUser.requireUserId(), orderNo));
    }

    @Operation(summary = "门店评价列表（含平均分）")
    @GetMapping("/shop/{shopId}")
    public Result<Map<String, Object>> byShop(@PathVariable Long shopId) {
        return Result.ok(reviewService.listByShop(shopId));
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private List<String> toList(Object o) {
        if (o instanceof List<?> l) {
            return l.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
        }
        return List.of();
    }
}

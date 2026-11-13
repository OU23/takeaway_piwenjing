package com.xingguang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.common.OrderStatus;
import com.xingguang.delivery.entity.AppUser;
import com.xingguang.delivery.entity.Orders;
import com.xingguang.delivery.entity.Review;
import com.xingguang.delivery.mapper.AppUserMapper;
import com.xingguang.delivery.mapper.OrdersMapper;
import com.xingguang.delivery.mapper.ReviewMapper;
import com.xingguang.delivery.vo.FileVO;
import com.xingguang.delivery.vo.ReviewVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单评价（最小可用版）：订单完成后评分 + 文字 + 多图。
 *
 * <p>一个订单只能评价一次（服务层校验 + 库表 uk_order 双保险）。
 * <p>图片列存 objectKey（逗号分隔，最多 9 张），展示时经 ObjectStorageService 解析为可访问 URL。
 */
@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrdersMapper ordersMapper;
    private final AppUserMapper appUserMapper;
    private final ObjectStorageService storage;

    public ReviewService(ReviewMapper reviewMapper, OrdersMapper ordersMapper,
                         AppUserMapper appUserMapper, ObjectStorageService storage) {
        this.reviewMapper = reviewMapper;
        this.ordersMapper = ordersMapper;
        this.appUserMapper = appUserMapper;
        this.storage = storage;
    }

    /** 提交评价：仅订单归属人、且订单已完成时可评一次。 */
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO create(long userId, String orderNo, Integer rating, String content, List<String> images) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BizException(10001, "缺少订单号 orderNo");
        }
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw BizException.order("订单不存在");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BizException(10003, "无权评价他人订单");
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw BizException.order("订单完成后才可以评价");
        }
        long exist = reviewMapper.selectCount(Wrappers.<Review>lambdaQuery()
                .eq(Review::getOrderId, order.getId()));
        if (exist > 0) {
            throw BizException.of("该订单已评价");
        }

        int r = rating == null ? 5 : rating;
        if (r < 1 || r > 5) {
            throw new BizException(10001, "评分需在 1-5 星之间");
        }
        String c = content == null ? "" : content.trim();
        if (c.length() > 500) {
            c = c.substring(0, 500);
        }
        List<String> imgs = images == null ? List.of()
                : images.stream().filter(s -> s != null && !s.isBlank()).limit(9).collect(Collectors.toList());

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setOrderNo(order.getOrderNo());
        review.setUserId(userId);
        review.setShopId(order.getShopId());
        review.setShopName(order.getShopName());
        review.setRating(r);
        review.setContent(c);
        review.setImages(imgs.isEmpty() ? null : String.join(",", imgs));
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);
        return toVO(review);
    }

    /** 某订单的评价；不存在返回 null（前端据此判断是否已评价）。 */
    public ReviewVO findByOrder(long userId, String orderNo) {
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getOrderNo, orderNo));
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            return null;
        }
        Review rv = reviewMapper.selectOne(Wrappers.<Review>lambdaQuery()
                .eq(Review::getOrderId, order.getId()));
        return rv == null ? null : toVO(rv);
    }

    /** 我的评价列表。 */
    public List<ReviewVO> listMine(long userId) {
        return reviewMapper.selectList(Wrappers.<Review>lambdaQuery()
                        .eq(Review::getUserId, userId)
                        .orderByDesc(Review::getCreatedAt))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 门店评价列表 + 平均分。 */
    public Map<String, Object> listByShop(Long shopId) {
        List<Review> list = reviewMapper.selectList(Wrappers.<Review>lambdaQuery()
                .eq(Review::getShopId, shopId)
                .orderByDesc(Review::getCreatedAt));
        double avg = list.isEmpty() ? 5.0
                : Math.round(list.stream().mapToInt(Review::getRating).average().orElse(5.0) * 10) / 10.0;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("avgRating", avg);
        m.put("total", list.size());
        m.put("list", list.stream().map(this::toVO).collect(Collectors.toList()));
        return m;
    }

    private ReviewVO toVO(Review r) {
        ReviewVO vo = new ReviewVO();
        vo.setId(r.getId());
        vo.setOrderNo(r.getOrderNo());
        vo.setShopId(r.getShopId());
        vo.setShopName(r.getShopName());
        vo.setRating(r.getRating());
        vo.setContent(r.getContent());
        if (r.getImages() != null && !r.getImages().isBlank()) {
            vo.setImages(Arrays.stream(r.getImages().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(this::resolveMedia).collect(Collectors.toList()));
        }
        vo.setReply(r.getReply());
        vo.setReplyAt(r.getReplyAt());
        vo.setCreatedAt(r.getCreatedAt());
        AppUser u = r.getUserId() == null ? null : appUserMapper.selectById(r.getUserId());
        if (u != null) {
            vo.setNickname(u.getNickname() == null ? "星光用户" : u.getNickname());
            vo.setUserAvatar(resolveMedia(u.getAvatar()));
        }
        return vo;
    }

    private String resolveMedia(String key) {
        if (key == null || key.isBlank() || !key.contains("/")) {
            return key;
        }
        FileVO vo = new FileVO();
        vo.setObjectKey(key);
        storage.applyUrl(vo);
        return vo.getUrl();
    }
}

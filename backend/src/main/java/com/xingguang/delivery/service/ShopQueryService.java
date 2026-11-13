package com.xingguang.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.entity.*;
import com.xingguang.delivery.mapper.*;
import com.xingguang.delivery.support.VoConverter;
import com.xingguang.delivery.vo.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 店铺 / 商品查询服务（首页、附近、店铺详情）。
 * 首页与金刚区全部由后台数据驱动，前端不写死（SRS 13 验收：后台配置项须真实驱动 App 效果）。
 */
@Service
public class ShopQueryService {

    private final ShopMapper shopMapper;
    private final DishMapper dishMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final DishSpecMapper dishSpecMapper;
    private final PlatformCategoryMapper platformCategoryMapper;
    private final VoConverter converter;

    public ShopQueryService(ShopMapper shopMapper, DishMapper dishMapper, DishCategoryMapper dishCategoryMapper,
                            DishSpecMapper dishSpecMapper, PlatformCategoryMapper platformCategoryMapper,
                            VoConverter converter) {
        this.shopMapper = shopMapper;
        this.dishMapper = dishMapper;
        this.dishCategoryMapper = dishCategoryMapper;
        this.dishSpecMapper = dishSpecMapper;
        this.platformCategoryMapper = platformCategoryMapper;
        this.converter = converter;
    }

    /** 首页聚合 */
    public HomeVO home() {
        HomeVO home = new HomeVO();

        home.setCategories(platformCategoryMapper.selectList(
                        Wrappers.<PlatformCategory>lambdaQuery()
                                .eq(PlatformCategory::getStatus, 1)
                                .orderByAsc(PlatformCategory::getSort))
                .stream()
                .map(c -> new CategoryVO(c.getId(), c.getName(), c.getIcon(), c.getSort()))
                .collect(Collectors.toList()));

        // 轮播：渐变占位（SRS 5.3.1 说明：商品图 / 店铺头图使用渐变占位）
        List<HomeVO.Banner> banners = new ArrayList<>();
        banners.add(banner("新客立减 5 元", "首单立减 · 到店自取再减 3 元", "sunrise", "CATEGORY", "8"));
        banners.add(banner("天天神券", "每日限领 · 满 30 减 8", "ember", "CATEGORY", "9"));
        banners.add(banner("轻食季", "会员专享 低卡轻食 8 折", "mint", "SHOP", "102"));
        home.setBanners(banners);

        // 秒杀：取门店中带「热销」标签的商品做横滑位（演示数据）
        home.setSeckills(seckill());

        home.setShops(list(null, "recommend"));
        return home;
    }

    /** 附近 / 搜索结果 */
    public List<ShopVO> list(String keyword, String sort) {
        return list(keyword, sort, null, null);
    }

    /** 带坐标的列表：传入 lng/lat 时计算各店 distanceKm，使 sort=distance 真实按距离排序。 */
    public List<ShopVO> list(String keyword, String sort, Double lng, Double lat) {
        LambdaQueryWrapper<Shop> q = Wrappers.<Shop>lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            q.like(Shop::getName, keyword.trim());
        }
        List<Shop> shops = shopMapper.selectList(q);

        if (lng != null && lat != null) {
            for (Shop s : shops) {
                if (s.getLat() != null && s.getLng() != null) {
                    s.setDistanceKm(java.math.BigDecimal.valueOf(
                            haversineKm(lat, lng, s.getLat().doubleValue(), s.getLng().doubleValue())));
                }
            }
        }

        Comparator<Shop> comparator = switch (sort == null ? "recommend" : sort) {
            case "distance" -> Comparator.comparing(s -> s.getDistanceKm() == null
                    ? java.math.BigDecimal.valueOf(99) : s.getDistanceKm());
            case "sales" -> Comparator.comparing(Shop::getMonthSales, Comparator.reverseOrder());
            case "rating" -> Comparator.comparing(Shop::getRating, Comparator.reverseOrder());
            case "minprice" -> Comparator.comparing(Shop::getMinPrice);
            default -> Comparator.comparing(Shop::getDistanceKm);
        };
        return shops.stream().sorted(comparator).map(converter::toShopVO).collect(Collectors.toList());
    }

    /** 附近门店：按经纬度返回半径内的门店，按距离升序（distanceKm 会写入 VO）。 */
    public List<ShopVO> nearby(double lng, double lat, Double radiusKm) {
        double radius = (radiusKm != null && radiusKm > 0) ? radiusKm : 5.0;
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery()
                .isNotNull(Shop::getLat).isNotNull(Shop::getLng));
        for (Shop s : shops) {
            double d = haversineKm(lat, lng, s.getLat().doubleValue(), s.getLng().doubleValue());
            s.setDistanceKm(java.math.BigDecimal.valueOf(d));
        }
        return shops.stream()
                .filter(s -> s.getDistanceKm().doubleValue() <= radius)
                .sorted(Comparator.comparing(Shop::getDistanceKm))
                .map(converter::toShopVO)
                .collect(Collectors.toList());
    }

    /** 两点间距离（km），Haversine 公式。 */
    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** 店铺详情（含店内分类与商品） */
    public ShopDetailVO detail(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw BizException.shop("门店不存在");
        }
        ShopDetailVO vo = new ShopDetailVO();
        vo.setShop(converter.toShopVO(shop));

        List<DishCategory> categories = dishCategoryMapper.selectList(
                Wrappers.<DishCategory>lambdaQuery()
                        .eq(DishCategory::getShopId, shopId)
                        .orderByAsc(DishCategory::getSort));
        vo.setCategories(categories.stream()
                .map(c -> new CategoryVO(c.getId(), c.getName(), null, c.getSort()))
                .collect(Collectors.toList()));

        List<Dish> dishes = dishMapper.selectList(Wrappers.<Dish>lambdaQuery()
                .eq(Dish::getShopId, shopId)
                .orderByAsc(Dish::getCategoryId).orderByAsc(Dish::getSort));

        Map<Long, List<DishSpec>> specMap = specMap(dishes);
        vo.setDishes(dishes.stream()
                .map(d -> converter.toDishVO(d, specMap.get(d.getId())))
                .collect(Collectors.toList()));
        return vo;
    }

    /** 单个商品（含规格组），用于规格弹窗 */
    public DishVO dish(Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw BizException.product("商品不存在");
        }
        List<DishSpec> specs = dishSpecMapper.selectList(Wrappers.<DishSpec>lambdaQuery()
                .eq(DishSpec::getDishId, dishId)
                .orderByAsc(DishSpec::getId));
        return converter.toDishVO(dish, specs);
    }

    /** 按商品 id 批量取规格 */
    public Map<Long, List<DishSpec>> specMap(List<Dish> dishes) {
        if (dishes.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = dishes.stream().map(Dish::getId).toList();
        List<DishSpec> specs = dishSpecMapper.selectList(Wrappers.<DishSpec>lambdaQuery()
                .in(DishSpec::getDishId, ids)
                .orderByAsc(DishSpec::getId));
        return specs.stream().collect(Collectors.groupingBy(DishSpec::getDishId));
    }

    private HomeVO.Banner banner(String title, String subtitle, String gradient,
                                 String targetType, String targetValue) {
        HomeVO.Banner b = new HomeVO.Banner();
        b.setTitle(title);
        b.setSubtitle(subtitle);
        b.setGradient(gradient);
        b.setTargetType(targetType);
        b.setTargetValue(targetValue);
        return b;
    }

    private List<HomeVO.Seckill> seckill() {
        List<Dish> hot = dishMapper.selectList(Wrappers.<Dish>lambdaQuery()
                .like(Dish::getTags, "热销")
                .last("limit 6"));
        List<HomeVO.Seckill> list = new ArrayList<>();
        int[] remain = {5400, 3600, 7200, 1800, 9000, 4200};
        for (int i = 0; i < hot.size(); i++) {
            Dish d = hot.get(i);
            HomeVO.Seckill s = new HomeVO.Seckill();
            s.setDishId(d.getId());
            s.setShopId(d.getShopId());
            s.setName(d.getName());
            // 与 DishVO 一致：objectKey 解析成 MinIO 直链，grad-* 占位原样返回
            s.setImage(converter.resolveMedia(d.getImage()));
            s.setPrice(d.getPrice());
            s.setOriginPrice(d.getOriginPrice() == null ? d.getPrice() : d.getOriginPrice());
            s.setRemainSeconds(remain[i % remain.length]);
            list.add(s);
        }
        return list;
    }
}

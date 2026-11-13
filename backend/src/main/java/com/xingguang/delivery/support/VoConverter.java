package com.xingguang.delivery.support;

import com.xingguang.delivery.entity.Dish;
import com.xingguang.delivery.entity.DishSpec;
import com.xingguang.delivery.entity.Shop;
import com.xingguang.delivery.service.ObjectStorageService;
import com.xingguang.delivery.vo.DishVO;
import com.xingguang.delivery.vo.FileVO;
import com.xingguang.delivery.vo.ShopVO;
import com.xingguang.delivery.vo.SpecGroupVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 实体 → VO 转换。 */
@Component
public class VoConverter {

    private final ObjectStorageService storage;

    public VoConverter(ObjectStorageService storage) {
        this.storage = storage;
    }

    private static final Pattern PROMO_CN = Pattern.compile("满(\\d+(?:\\.\\d+)?)减(\\d+(?:\\.\\d+)?)");

    public ShopVO toShopVO(Shop shop) {
        ShopVO vo = new ShopVO();
        vo.setId(shop.getId());
        vo.setMerchantId(shop.getMerchantId());
        vo.setName(shop.getName());
        vo.setLogo(resolveMedia(shop.getLogo()));
        vo.setHeaderImg(resolveMedia(shop.getHeaderImg()));
        vo.setRating(shop.getRating());
        vo.setMonthSales(shop.getMonthSales());
        vo.setAddress(shop.getAddress());
        vo.setNotice(shop.getNotice());
        vo.setOpenTime(shop.getOpenTime());
        vo.setCloseTime(shop.getCloseTime());
        vo.setStatus(shop.getStatus());
        vo.setStatusLabel(statusLabel(shop.getStatus()));
        vo.setMinPrice(shop.getMinPrice());
        vo.setDeliveryFee(shop.getDeliveryFee());
        vo.setEtaMinutes(shop.getEtaMinutes());
        vo.setPickupEnabled(shop.getPickupEnabled() != null && shop.getPickupEnabled() == 1);
        vo.setPickupDiscount(shop.getPickupDiscount());
        vo.setDistanceKm(shop.getDistanceKm());
        vo.setLat(shop.getLat());
        vo.setLng(shop.getLng());

        if (shop.getTags() != null && !shop.getTags().isBlank()) {
            vo.setTags(Arrays.stream(shop.getTags().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.toList()));
        }
        vo.setPromotions(parsePromotions(shop.getTags()));
        return vo;
    }

    private List<String> parsePromotions(String tags) {
        List<String> list = new ArrayList<>();
        if (tags == null || tags.isBlank()) {
            return list;
        }
        Matcher m = PROMO_CN.matcher(tags);
        while (m.find()) {
            list.add("满 ¥" + trimZero(m.group(1)) + " 减 ¥" + trimZero(m.group(2)));
        }
        return list;
    }

    private String trimZero(String num) {
        if (num.contains(".")) {
            num = num.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return num;
    }

    private String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "OPEN" -> "营业中";
            case "CLOSED" -> "已打烊";
            case "REST" -> "休息中";
            default -> status;
        };
    }

    /**
     * 商品 → VO，并装配规格组。
     *
     * @param specs 该商品的规格 / 加料（可为空）
     */
    public DishVO toDishVO(Dish dish, List<DishSpec> specs) {
        DishVO vo = new DishVO();
        vo.setId(dish.getId());
        vo.setShopId(dish.getShopId());
        vo.setCategoryId(dish.getCategoryId());
        vo.setName(dish.getName());
        vo.setDescription(dish.getDescription());
        vo.setImage(resolveMedia(dish.getImage()));
        vo.setPrice(dish.getPrice());
        vo.setOriginPrice(dish.getOriginPrice());
        vo.setMonthSales(dish.getMonthSales());
        vo.setStock(dish.getStock());
        vo.setSoldOut(dish.getSoldOut() != null && dish.getSoldOut() == 1);
        if (dish.getTags() != null && !dish.getTags().isBlank()) {
            vo.setTags(Arrays.stream(dish.getTags().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.toList()));
        }

        Map<String, SpecGroupVO> groups = new LinkedHashMap<>();
        if (specs != null) {
            for (DishSpec spec : specs) {
                SpecGroupVO group = groups.computeIfAbsent(spec.getGroupName(), g -> {
                    SpecGroupVO v = new SpecGroupVO();
                    v.setGroupName(g);
                    v.setType(spec.getType());
                    v.setOptions(new ArrayList<>());
                    return v;
                });
                group.getOptions().add(new SpecGroupVO.Option(
                        spec.getId(), spec.getName(), spec.getPriceDelta(),
                        spec.getIsDefault() != null && spec.getIsDefault() == 1));
            }
        }
        vo.setSpecGroups(new ArrayList<>(groups.values()));
        vo.setHasSpec(!groups.isEmpty());
        return vo;
    }

    /**
     * 把媒体字段解析成可访问地址：真实 objectKey（含 '/'）解析为 MinIO 直链；
     * 渐变占位 key（如 grad-1）或空值原样返回，由前端 PhImage 当作占位处理。
     */
    public String resolveMedia(String key) {
        if (key == null || key.isBlank() || !key.contains("/")) {
            return key;
        }
        FileVO vo = new FileVO();
        vo.setObjectKey(key);
        storage.applyUrl(vo);
        return vo.getUrl();
    }
}

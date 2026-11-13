package com.xingguang.delivery.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 首页聚合数据（由后台配置驱动，前端不写死）。 */
@Data
public class HomeVO {

    /** 当前城市（本期单城市运营，切换仅预留入口） */
    private String city = "天津";

    /** 顶部轮播（渐变占位） */
    private List<Banner> banners = new ArrayList<>();

    /** 金刚区品类 */
    private List<CategoryVO> categories = new ArrayList<>();

    /** 限时秒杀（横滑） */
    private List<Seckill> seckills = new ArrayList<>();

    /** 附近好店 */
    private List<ShopVO> shops = new ArrayList<>();

    /** 首页 Tab 标语 */
    private List<String> serviceGuarantees = List.of("品质联盟", "准时达", "食安封签");

    @Data
    public static class Banner {
        private String title;
        private String subtitle;
        private String gradient;
        /** 跳转类型：SHOP / CATEGORY / ACTIVITY */
        private String targetType;
        private String targetValue;
    }

    @Data
    public static class Seckill {
        private Long dishId;
        private Long shopId;
        private String name;
        private String image;
        private Integer price;
        private Integer originPrice;
        /** 秒杀剩余秒数（演示用固定值） */
        private Integer remainSeconds;
    }
}

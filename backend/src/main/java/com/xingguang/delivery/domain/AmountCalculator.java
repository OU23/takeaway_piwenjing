package com.xingguang.delivery.domain;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.common.FulfillType;
import com.xingguang.delivery.entity.Shop;
import com.xingguang.delivery.vo.AmountVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 金额计算器（SRS 9.7 / 9.8 / 8.6）。
 *
 * <p>职责：
 * <ol>
 *   <li>商品小计 / 打包费 / 配送费 / 优惠 / 实付，全部以「分」整数计算，禁止浮点；</li>
 *   <li>起送价校验按「商品小计」判断（不含配送费与打包费）；</li>
 *   <li>满减门槛按折后商品小计判断，取命中档位中门槛最高的一档；</li>
 *   <li>自取单：免配送费，并按门店配置叠加「自取立减」；</li>
 *   <li>保底：实付 ≥ 1 分（不允许 0 元或负数成交）。</li>
 * </ol>
 * 该计算器是唯一金额来源，前端计算结果仅作展示。
 */
public final class AmountCalculator {

    /** 匹配门店标签中的满减规则，如「满30减8」 */
    private static final Pattern FULL_REDUCE = Pattern.compile("满(\\d+(?:\\.\\d+)?)减(\\d+(?:\\.\\d+)?)");

    private AmountCalculator() {
    }

    public static AmountVO calculate(Shop shop, FulfillType fulfillType, List<LineItem> lines) {
        if (lines == null || lines.isEmpty()) {
            throw BizException.order("请先选择商品");
        }

        AmountVO vo = new AmountVO();

        // 1) 商品小计 + 件数
        int itemAmount = 0;
        int totalQty = 0;
        for (LineItem line : lines) {
            if (line.getQuantity() == null || line.getQuantity() <= 0) {
                throw BizException.order("商品数量不合法：" + line.getDishName());
            }
            itemAmount += line.amount();
            totalQty += line.getQuantity();
        }
        vo.setItemAmount(itemAmount);
        vo.setTotalQuantity(totalQty);

        // 2) 打包费 = 门店打包费 × 件数
        int packFee = nvl(shop.getPackFee()) * totalQty;
        vo.setPackFee(packFee);

        // 3) 配送费：自取单免费（原价划线展示）
        int originDeliveryFee = nvl(shop.getDeliveryFee());
        vo.setDeliveryFeeOrigin(originDeliveryFee);
        vo.setDeliveryFee(fulfillType == FulfillType.DELIVERY ? originDeliveryFee : 0);

        // 4) 满减（门槛按商品小计判断）
        FullReduce hit = matchFullReduce(shop.getTags(), itemAmount);
        if (hit != null) {
            vo.setFullReduceAmount(hit.reduce);
            vo.setFullReduceRule("满" + yuan(hit.threshold) + "减" + yuan(hit.reduce));
        } else {
            FullReduce next = nextFullReduce(shop.getTags(), itemAmount);
            if (next != null) {
                vo.setNextFullReduceHint("再买 ¥" + yuan(next.threshold - itemAmount)
                        + " 可减 ¥" + yuan(next.reduce));
            }
        }

        // 5) 自取立减（履约方式优惠，与营销满减不互斥，SRS 8.6）
        if (fulfillType == FulfillType.PICKUP && nvl(shop.getPickupDiscount()) > 0) {
            vo.setPickupDiscountAmount(shop.getPickupDiscount());
        }

        // 6) 优惠合计与实付
        int discount = vo.getFullReduceAmount() + vo.getPickupDiscountAmount();
        int pay = itemAmount + packFee + vo.getDeliveryFee() - discount;
        if (pay < 1) {
            pay = 1;
            discount = itemAmount + packFee + vo.getDeliveryFee() - 1;
        }
        vo.setDiscountAmount(discount);
        vo.setPayAmount(pay);

        // 7) 起送价
        int minPrice = nvl(shop.getMinPrice());
        vo.setMinPrice(minPrice);
        boolean reach = itemAmount >= minPrice;
        vo.setReachMin(reach);
        vo.setMinPriceGap(reach ? 0 : minPrice - itemAmount);

        return vo;
    }

    /** 命中门槛最高的一档满减；未命中返回 null */
    private static FullReduce matchFullReduce(String tags, int itemAmount) {
        List<FullReduce> rules = parseFullReduce(tags);
        FullReduce hit = null;
        for (FullReduce r : rules) {
            if (itemAmount >= r.threshold) {
                if (hit == null || r.threshold > hit.threshold) {
                    hit = r;
                }
            }
        }
        return hit;
    }

    /** 下一档（门槛高于当前小计的最低档） */
    private static FullReduce nextFullReduce(String tags, int itemAmount) {
        List<FullReduce> rules = parseFullReduce(tags);
        FullReduce next = null;
        for (FullReduce r : rules) {
            if (r.threshold > itemAmount) {
                if (next == null || r.threshold < next.threshold) {
                    next = r;
                }
            }
        }
        return next;
    }

    private static List<FullReduce> parseFullReduce(String tags) {
        List<FullReduce> list = new ArrayList<>();
        if (tags == null || tags.isEmpty()) {
            return list;
        }
        Matcher m = FULL_REDUCE.matcher(tags);
        while (m.find()) {
            int threshold = yuan2fen(m.group(1));
            int reduce = yuan2fen(m.group(2));
            if (threshold > 0 && reduce > 0) {
                list.add(new FullReduce(threshold, reduce));
            }
        }
        list.sort(Comparator.comparingInt(r -> r.threshold));
        return list;
    }

    private static int yuan2fen(String yuan) {
        return new BigDecimal(yuan).multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /** 分 → 元字符串（用于提示文案，保留两位小数、去掉多余 0） */
    public static String yuan(int fen) {
        return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private record FullReduce(int threshold, int reduce) {
    }
}

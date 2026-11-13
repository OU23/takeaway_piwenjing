package com.xingguang.delivery.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 骑手收入与结算视图。金额单位均为「分」。 */
@Data
public class RiderIncomeVO {

    /** 今日收入 */
    private Integer todayIncome = 0;
    /** 累计收入 */
    private Integer totalIncome = 0;
    /** 已结算收入 */
    private Integer settledIncome = 0;
    /** 待结算收入 */
    private Integer unsettledIncome = 0;
    /** 累计完成单量 */
    private Integer totalCount = 0;

    private List<Item> list = new ArrayList<>();

    @Data
    public static class Item {
        private String orderNo;
        private String shopName;
        /** 配送费佣金（分） */
        private Integer baseFee;
        /** 平台补贴（分） */
        private Integer subsidy;
        /** 罚款（分） */
        private Integer penalty;
        /** 实得收入（分） */
        private Integer incomeAmount;
        /** 1 已结算 / 0 待结算 */
        private Integer settled;
        private LocalDateTime createdAt;
    }
}

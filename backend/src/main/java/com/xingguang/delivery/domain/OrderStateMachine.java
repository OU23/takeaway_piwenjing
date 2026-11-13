package com.xingguang.delivery.domain;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.common.FulfillType;
import com.xingguang.delivery.common.OrderStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.xingguang.delivery.common.OrderStatus.*;

/**
 * 订单状态机（SRS 8.1 / 8.9）。
 * 两类订单共用同一套状态机，自取单把「待取货 → 配送中 → 已送达」替换为「待取餐 → 已取餐」，
 * 不引入任何新状态。
 */
public final class OrderStateMachine {

    private static final Map<String, Set<OrderStatus>> TRANSITIONS = new LinkedHashMap<>();

    static {
        // ---------------- 平台配送单 ----------------
        transition(FulfillType.DELIVERY, UNPAID, WAIT_ACCEPT, CANCELED);
        transition(FulfillType.DELIVERY, WAIT_ACCEPT, PREPARING, REFUNDED, CANCELED);
        transition(FulfillType.DELIVERY, PREPARING, WAIT_PICKUP, EXCEPTION, REFUNDED, CANCELED);
        transition(FulfillType.DELIVERY, WAIT_PICKUP, DELIVERING, EXCEPTION, REFUNDED);
        transition(FulfillType.DELIVERY, DELIVERING, DELIVERED, EXCEPTION, REFUNDED);
        transition(FulfillType.DELIVERY, DELIVERED, COMPLETED, EXCEPTION, PART_REFUNDED);
        transition(FulfillType.DELIVERY, EXCEPTION, DELIVERED, CANCELED, REFUNDED);
        transition(FulfillType.DELIVERY, COMPLETED, PART_REFUNDED, REFUNDED);

        // ---------------- 到店自取单 ----------------
        transition(FulfillType.PICKUP, UNPAID, WAIT_ACCEPT, CANCELED);
        transition(FulfillType.PICKUP, WAIT_ACCEPT, PREPARING, REFUNDED, CANCELED);
        transition(FulfillType.PICKUP, PREPARING, WAIT_MEAL, REFUNDED, CANCELED);
        transition(FulfillType.PICKUP, WAIT_MEAL, PICKED, REFUNDED, EXCEPTION);
        transition(FulfillType.PICKUP, PICKED, COMPLETED, PART_REFUNDED);
        transition(FulfillType.PICKUP, EXCEPTION, REFUNDED, CANCELED);
        transition(FulfillType.PICKUP, COMPLETED, PART_REFUNDED, REFUNDED);
    }

    private OrderStateMachine() {
    }

    private static void transition(FulfillType type, OrderStatus from, OrderStatus... to) {
        Set<OrderStatus> set = TRANSITIONS.computeIfAbsent(key(type, from), k -> new LinkedHashSet<>());
        for (OrderStatus t : to) {
            set.add(t);
        }
    }

    private static String key(FulfillType type, OrderStatus status) {
        return type.name() + "#" + status.name();
    }

    /** 是否允许流转 */
    public static boolean canTransfer(FulfillType type, String fromCode, String toCode) {
        OrderStatus from = OrderStatus.of(fromCode);
        OrderStatus to = OrderStatus.of(toCode);
        if (from == null || to == null) {
            return false;
        }
        Set<OrderStatus> set = TRANSITIONS.get(key(type, from));
        return set != null && set.contains(to);
    }

    /**
     * 校验并返回目标状态，不允许则抛业务异常（防并发/越权改状态）。
     */
    public static OrderStatus require(FulfillType type, String fromCode, OrderStatus to) {
        if (!canTransfer(type, fromCode, to.name())) {
            throw BizException.order("订单状态不允许从「" + OrderStatus.labelOf(fromCode)
                    + "」流转到「" + to.getLabel() + "」");
        }
        return to;
    }

    /** 终态 */
    public static boolean isFinal(String code) {
        return COMPLETED.name().equals(code) || CANCELED.name().equals(code)
                || REFUNDED.name().equals(code);
    }

    /**
     * 当前状态下的「模拟推进」目标（演示用：本原型不含商家端 / 骑手端，
     * 由后台一键推进状态，逻辑与真实端上操作一致）。
     */
    public static OrderStatus nextBySimulation(FulfillType type, String code) {
        OrderStatus current = OrderStatus.of(code);
        if (current == null) {
            return null;
        }
        if (type == FulfillType.DELIVERY) {
            return switch (current) {
                case WAIT_ACCEPT -> PREPARING;
                case PREPARING -> WAIT_PICKUP;
                case WAIT_PICKUP -> DELIVERING;
                case DELIVERING -> DELIVERED;
                case DELIVERED -> COMPLETED;
                default -> null;
            };
        }
        return switch (current) {
            case WAIT_ACCEPT -> PREPARING;
            case PREPARING -> WAIT_MEAL;
            case WAIT_MEAL -> PICKED;
            case PICKED -> COMPLETED;
            default -> null;
        };
    }

    /** 该状态在业务上由谁操作（写入状态流水的 operator） */
    public static String operatorOf(OrderStatus to) {
        return switch (to) {
            case PREPARING, WAIT_MEAL -> "MERCHANT";
            case WAIT_PICKUP, DELIVERING, DELIVERED -> "RIDER";
            case PICKED, COMPLETED -> "USER";
            case CANCELED, REFUNDED, PART_REFUNDED -> "ADMIN";
            case EXCEPTION -> "SYSTEM";
            default -> "SYSTEM";
        };
    }

    /** 前端可用的操作按钮 */
    public static List<String> actionsOf(FulfillType type, String code, boolean isOwner) {
        List<String> actions = new ArrayList<>();
        if (!isOwner) {
            return actions;
        }
        OrderStatus s = OrderStatus.of(code);
        if (s == null) {
            return actions;
        }
        switch (s) {
            case UNPAID -> actions.add("PAY");
            case WAIT_ACCEPT -> {
                actions.add("URGE");
                actions.add("CANCEL");
            }
            case PREPARING -> {
                actions.add("URGE");
                actions.add("CANCEL");
                if (type == FulfillType.DELIVERY) {
                    actions.add("MODIFY_ADDRESS");
                }
            }
            case WAIT_PICKUP, DELIVERING, WAIT_MEAL -> actions.add("URGE");
            case DELIVERED -> {
                actions.add("CONFIRM");
                actions.add("URGE");
            }
            case PICKED -> actions.add("CONFIRM");
            case COMPLETED -> actions.add("REBUY");
            case CANCELED, REFUNDED, PART_REFUNDED -> actions.add("REBUY");
            default -> {
                // 异常等状态不提供用户侧操作
            }
        }
        return actions;
    }
}

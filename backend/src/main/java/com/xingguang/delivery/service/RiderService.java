package com.xingguang.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xingguang.delivery.common.*;
import com.xingguang.delivery.entity.*;
import com.xingguang.delivery.mapper.*;
import com.xingguang.delivery.support.TokenCodec;
import com.xingguang.delivery.vo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 骑手端服务：接单大厅、抢单、取货、送达、任务查询、收入结算、消息。
 *
 * <p>与订单域共用 {@link OrderStateMachine} 与 {@link OrderService#transferStatus}，
 * 骑手取货/送达严格走状态机校验，避免跳状态。
 */
@Service
public class RiderService {

    /** 平台补贴（分），真实环境按运力策略配置（SRS 4.5） */
    private static final int PLATFORM_SUBSIDY = 200;

    /** 接单大厅可抢的订单状态：即将取货（备餐中）/ 待取货 */
    private static final List<String> GRABBABLE = List.of("PREPARING", "WAIT_PICKUP");

    private static final Map<String, List<String>> TASK_GROUP = new LinkedHashMap<>();

    static {
        TASK_GROUP.put("进行中", List.of("PREPARING", "WAIT_PICKUP", "DELIVERING", "DELIVERED", "EXCEPTION"));
        TASK_GROUP.put("已完成", List.of("COMPLETED"));
    }

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper logMapper;
    private final ShopMapper shopMapper;
    private final RiderMapper riderMapper;
    private final RiderIncomeMapper incomeMapper;
    private final RiderMessageMapper messageMapper;
    private final OrderService orderService;
    private final com.xingguang.delivery.support.VoConverter converter;

    public RiderService(OrdersMapper ordersMapper, OrderItemMapper orderItemMapper,
                        OrderStatusLogMapper logMapper, ShopMapper shopMapper,
                        RiderMapper riderMapper, RiderIncomeMapper incomeMapper,
                        RiderMessageMapper messageMapper, OrderService orderService,
                        com.xingguang.delivery.support.VoConverter converter) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.logMapper = logMapper;
        this.shopMapper = shopMapper;
        this.riderMapper = riderMapper;
        this.incomeMapper = incomeMapper;
        this.messageMapper = messageMapper;
        this.orderService = orderService;
        this.converter = converter;
    }

    // ==================================================================
    // 登录
    // ==================================================================

    @org.springframework.beans.factory.annotation.Value("${xingguang.mock-sms-code:8888}")
    private String mockSmsCode;

    @Value("${xingguang.auth.secret:xg-dev-secret-change-me-in-production}")
    private String secret;

    @Value("${xingguang.auth.token-ttl-hours:168}")
    private long tokenTtlHours;

    /**
     * 骑手登录（原型）：手机号 + 演示验证码（默认 8888）。
     *
     * <p>成功后签发受众为 RIDER 的签名令牌（与用户/后台令牌受众隔离），
     * 前端放入 {@code X-Rider-Token} 头。
     */
    public Map<String, Object> login(String phone, String code) {
        String p = phone == null ? "" : phone.trim();
        String c = code == null ? "" : code.trim();
        if (!p.matches("^1\\d{10}$")) {
            throw new BizException(10001, "请输入有效的 11 位手机号");
        }
        if (!mockSmsCode.equals(c)) {
            throw new BizException(11009, "验证码不正确（演示验证码 " + mockSmsCode + "）");
        }
        Rider rider = riderMapper.selectOne(Wrappers.<Rider>lambdaQuery()
                .eq(Rider::getPhone, p));
        if (rider == null) {
            throw new BizException(10002, "该手机号未注册骑手");
        }
        Map<String, Object> riderInfo = new LinkedHashMap<>();
        riderInfo.put("id", rider.getId());
        riderInfo.put("name", rider.getName());
        riderInfo.put("phoneMasked", Mask.phone(rider.getPhone()));
        riderInfo.put("avatar", rider.getAvatar());
        riderInfo.put("status", rider.getStatus());
        riderInfo.put("rating", rider.getRating());
        riderInfo.put("vehicle", rider.getVehicle());

        Map<String, Object> result = new LinkedHashMap<>();
        long exp = System.currentTimeMillis() / 1000L + tokenTtlHours * 3600L;
        result.put("token", TokenCodec.issue(rider.getId(), TokenCodec.AUD_RIDER, exp, secret));
        result.put("expiresAt", exp);
        result.put("rider", riderInfo);
        return result;
    }

    // ==================================================================
    // 档案 / 上线
    // ==================================================================

    public RiderProfileVO profile(long riderId) {
        Rider r = requireRider(riderId);
        RiderProfileVO vo = new RiderProfileVO();
        vo.setId(r.getId());
        vo.setName(r.getName());
        vo.setPhoneMasked(Mask.phone(r.getPhone()));
        vo.setAvatar(r.getAvatar());
        vo.setStatus(r.getStatus());
        vo.setOnline("ONLINE".equals(r.getStatus()));
        vo.setRating(r.getRating());
        vo.setVehicle(r.getVehicle());
        vo.setCompletedCount(r.getCompletedCount());
        vo.setBalance(r.getBalance());

        LocalDate today = LocalDate.now();
        List<RiderIncome> todayInc = incomeMapper.selectList(Wrappers.<RiderIncome>lambdaQuery()
                .eq(RiderIncome::getRiderId, riderId)
                .ge(RiderIncome::getCreatedAt, today.atStartOfDay()));
        vo.setTodayIncome(todayInc.stream().mapToInt(RiderIncome::getIncomeAmount).sum());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public RiderProfileVO setOnline(long riderId, boolean online) {
        Rider r = requireRider(riderId);
        r.setStatus(online ? "ONLINE" : "OFFLINE");
        riderMapper.updateById(r);
        return profile(riderId);
    }

    // ==================================================================
    // 接单大厅
    // ==================================================================

    /** 接单大厅：未派单的配送单，状态为「即将取货 / 待取货」。 */
    public List<RiderTaskVO> hall(long riderId) {
        List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getFulfillType, FulfillType.DELIVERY.name())
                .isNull(Orders::getRiderId)
                .in(Orders::getStatus, GRABBABLE)
                .orderByDesc(Orders::getCreatedAt));
        return orders.stream().map(o -> toTask(o, false)).collect(Collectors.toList());
    }

    // ==================================================================
    // 我的任务
    // ==================================================================

    public PageResult<RiderTaskVO> myTasks(long riderId, String group, Long pageNum, Long pageSize) {
        long pn = PageResult.normalizePageNum(pageNum);
        long ps = PageResult.normalizePageSize(pageSize);

        LambdaQueryWrapper<Orders> q = Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getRiderId, riderId)
                .eq(Orders::getFulfillType, FulfillType.DELIVERY.name())
                .orderByDesc(Orders::getCreatedAt);
        if (group != null && !group.isBlank() && !"全部".equals(group)) {
            List<String> statuses = TASK_GROUP.get(group);
            if (statuses == null) {
                throw new BizException(10001, "未知的任务分组：" + group);
            }
            q.in(Orders::getStatus, statuses);
        }

        IPage<Orders> page = ordersMapper.selectPage(new Page<>(pn, ps), q);
        List<RiderTaskVO> list = page.getRecords().stream()
                .map(o -> toTask(o, true))
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    /** 任务详情（复用订单域 VO，保证金额 / 时间轴与用户端一致）。 */
    public OrderVO taskDetail(long riderId, String orderNo) {
        Orders order = requireRiderOrder(riderId, orderNo);
        if (!FulfillType.DELIVERY.name().equals(order.getFulfillType())) {
            throw BizException.order("到店自取订单无需骑手配送");
        }
        return orderService.toVO(order, true);
    }

    // ==================================================================
    // 抢单 / 取货 / 送达
    // ==================================================================

    /**
     * 抢单：并发安全地把订单派给自己。
     * 用 {@code UPDATE ... WHERE rider_id IS NULL AND status IN (...) } 的影响行数判定，避免超卖。
     */
    @Transactional(rollbackFor = Exception.class)
    public RiderTaskVO grab(long riderId, String orderNo) {
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw BizException.order("订单不存在：" + orderNo);
        }
        if (!FulfillType.DELIVERY.name().equals(order.getFulfillType())) {
            throw BizException.order("到店自取订单无需骑手配送");
        }
        if (order.getRiderId() != null) {
            throw BizException.order("手慢了，订单已被其他骑手接走");
        }
        if (!GRABBABLE.contains(order.getStatus())) {
            throw BizException.order("订单状态不可接单（需为备餐中或待取货）");
        }

        int rows = ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                .set(Orders::getRiderId, riderId)
                .eq(Orders::getOrderNo, orderNo)
                .isNull(Orders::getRiderId)
                .in(Orders::getStatus, GRABBABLE));
        if (rows == 0) {
            throw BizException.order("手慢了，订单已被其他骑手接走");
        }

        Orders updated = requireRiderOrder(riderId, orderNo);
        orderService.addLog(updated, updated.getStatus(), updated.getStatus(), "RIDER", "骑手已接单");
        pushMessage(riderId, "ORDER", "接单成功",
                "您已承接 " + updated.getShopName() + " 的订单，请及时到店取货", orderNo);
        return toTask(updated, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderVO pickUp(long riderId, String orderNo) {
        Orders order = requireRiderOrder(riderId, orderNo);
        if (!FulfillType.DELIVERY.name().equals(order.getFulfillType())) {
            throw BizException.order("到店自取订单无需骑手配送");
        }
        if (!OrderStatus.WAIT_PICKUP.name().equals(order.getStatus())) {
            throw BizException.order("订单尚未出餐完成，暂不可取货");
        }
        orderService.transferStatus(order, OrderStatus.DELIVERING, "RIDER", "骑手已取货，开始配送");
        pushMessage(riderId, "ORDER", "已取货", "您已取货，正在配送至顾客", orderNo);
        return orderService.toVO(order, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderVO deliver(long riderId, String orderNo) {
        Orders order = requireRiderOrder(riderId, orderNo);
        if (!FulfillType.DELIVERY.name().equals(order.getFulfillType())) {
            throw BizException.order("到店自取订单无需骑手配送");
        }
        if (!OrderStatus.DELIVERING.name().equals(order.getStatus())) {
            throw BizException.order("订单尚未取货，暂不可送达");
        }
        orderService.transferStatus(order, OrderStatus.DELIVERED, "RIDER", "骑手已送达");
        settleIncome(riderId, order);
        pushMessage(riderId, "ORDER", "已送达", "订单已送达顾客，辛苦了！", orderNo);
        return orderService.toVO(order, true);
    }

    /** 送达后结算骑手收入（防重复结算）。 */
    private void settleIncome(long riderId, Orders order) {
        long exist = incomeMapper.selectCount(Wrappers.<RiderIncome>lambdaQuery()
                .eq(RiderIncome::getRiderId, riderId)
                .eq(RiderIncome::getOrderId, order.getId()));
        if (exist > 0) {
            return;
        }
        int base = order.getDeliveryFee() == null ? 0 : order.getDeliveryFee();
        int income = base + PLATFORM_SUBSIDY;
        RiderIncome ri = new RiderIncome();
        ri.setRiderId(riderId);
        ri.setOrderId(order.getId());
        ri.setOrderNo(order.getOrderNo());
        ri.setShopName(order.getShopName());
        ri.setBaseFee(base);
        ri.setSubsidy(PLATFORM_SUBSIDY);
        ri.setPenalty(0);
        ri.setIncomeAmount(income);
        ri.setSettled(0);
        ri.setCreatedAt(LocalDateTime.now());
        incomeMapper.insert(ri);

        Rider r = requireRider(riderId);
        r.setCompletedCount((r.getCompletedCount() == null ? 0 : r.getCompletedCount()) + 1);
        r.setBalance((r.getBalance() == null ? 0 : r.getBalance()) + income);
        riderMapper.updateById(r);
    }

    // ==================================================================
    // 收入与结算
    // ==================================================================

    public RiderIncomeVO income(long riderId) {
        List<RiderIncome> all = incomeMapper.selectList(Wrappers.<RiderIncome>lambdaQuery()
                .eq(RiderIncome::getRiderId, riderId)
                .orderByDesc(RiderIncome::getCreatedAt));
        RiderIncomeVO vo = new RiderIncomeVO();
        LocalDate today = LocalDate.now();
        for (RiderIncome ri : all) {
            vo.setTotalIncome(vo.getTotalIncome() + ri.getIncomeAmount());
            vo.setTotalCount(vo.getTotalCount() + 1);
            if (ri.getSettled() != null && ri.getSettled() == 1) {
                vo.setSettledIncome(vo.getSettledIncome() + ri.getIncomeAmount());
            } else {
                vo.setUnsettledIncome(vo.getUnsettledIncome() + ri.getIncomeAmount());
            }
            if (ri.getCreatedAt() != null && ri.getCreatedAt().isAfter(today.atStartOfDay())) {
                vo.setTodayIncome(vo.getTodayIncome() + ri.getIncomeAmount());
            }
            RiderIncomeVO.Item item = new RiderIncomeVO.Item();
            item.setOrderNo(ri.getOrderNo());
            item.setShopName(ri.getShopName());
            item.setBaseFee(ri.getBaseFee());
            item.setSubsidy(ri.getSubsidy());
            item.setPenalty(ri.getPenalty());
            item.setIncomeAmount(ri.getIncomeAmount());
            item.setSettled(ri.getSettled());
            item.setCreatedAt(ri.getCreatedAt());
            vo.getList().add(item);
        }
        return vo;
    }

    // ==================================================================
    // 消息
    // ==================================================================

    public List<RiderMessageVO> messages(long riderId) {
        List<RiderMessage> all = messageMapper.selectList(Wrappers.<RiderMessage>lambdaQuery()
                .eq(RiderMessage::getRiderId, riderId)
                .orderByDesc(RiderMessage::getCreatedAt));
        return all.stream().map(m -> {
            RiderMessageVO vo = new RiderMessageVO();
            vo.setId(m.getId());
            vo.setType(m.getType());
            vo.setTitle(m.getTitle());
            vo.setContent(m.getContent());
            vo.setRefOrderNo(m.getRefOrderNo());
            vo.setIsRead(m.getIsRead());
            vo.setCreatedAt(m.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void readMessage(long riderId, long id) {
        RiderMessage m = messageMapper.selectById(id);
        if (m == null || !m.getRiderId().equals(riderId)) {
            throw new BizException(10003, "消息不存在或无权操作");
        }
        m.setIsRead(1);
        messageMapper.updateById(m);
    }

    // ==================================================================
    // 内部工具
    // ==================================================================

    private Rider requireRider(long riderId) {
        Rider r = riderMapper.selectById(riderId);
        if (r == null) {
            throw BizException.order("骑手不存在：" + riderId);
        }
        return r;
    }

    private Orders requireRiderOrder(long riderId, String orderNo) {
        Orders order = orderService.requireOrder(orderNo);
        if (order.getRiderId() == null || !order.getRiderId().equals(riderId)) {
            throw new BizException(10003, "无权操作该订单");
        }
        return order;
    }

    private void pushMessage(long riderId, String type, String title, String content, String orderNo) {
        RiderMessage m = new RiderMessage();
        m.setRiderId(riderId);
        m.setType(type);
        m.setTitle(title);
        m.setContent(content);
        m.setRefOrderNo(orderNo);
        m.setIsRead(0);
        m.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(m);
    }

    private RiderTaskVO toTask(Orders o, boolean withActions) {
        RiderTaskVO vo = new RiderTaskVO();
        vo.setOrderNo(o.getOrderNo());
        vo.setShopId(o.getShopId());
        vo.setShopName(o.getShopName());
        vo.setShopHeaderImg(converter.resolveMedia(o.getShopHeaderImg()));
        vo.setFulfillType(o.getFulfillType());
        vo.setFulfillTypeLabel(FulfillType.of(o.getFulfillType()).getLabel());
        vo.setStatus(o.getStatus());
        vo.setStatusLabel(OrderStatus.labelOf(o.getStatus()));
        OrderStatus st = OrderStatus.of(o.getStatus());
        vo.setStatusGroup(st == null ? null : st.getGroup());
        vo.setReceiverName(o.getReceiverName());
        vo.setReceiverPhone(Mask.phone(o.getReceiverPhone()));
        vo.setReceiverAddress(o.getReceiverAddress());
        vo.setPayAmount(o.getPayAmount());

        vo.setItemCount(Math.toIntExact(orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, o.getId()))));

        Shop shop = shopMapper.selectById(o.getShopId());
        if (shop != null) {
            vo.setDistanceKm(shop.getDistanceKm());
            vo.setShopLat(shop.getLat());
            vo.setShopLng(shop.getLng());
        }
        vo.setEtaMinutes(o.getPromiseMinutes());
        LocalDateTime base = o.getPaidAt() != null ? o.getPaidAt() : o.getCreatedAt();
        if (base != null && o.getPromiseMinutes() != null) {
            String text = base.plusMinutes(o.getPromiseMinutes())
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            vo.setPromiseText("预计 " + text + " 送达");
        } else {
            vo.setPromiseText("商家备餐中");
        }
        vo.setCreatedAt(o.getCreatedAt());

        if (withActions) {
            vo.setCanPickup(OrderStatus.WAIT_PICKUP.name().equals(o.getStatus()));
            vo.setCanDeliver(OrderStatus.DELIVERING.name().equals(o.getStatus()));
        }
        return vo;
    }
}

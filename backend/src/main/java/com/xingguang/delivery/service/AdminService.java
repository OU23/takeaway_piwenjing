package com.xingguang.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xingguang.delivery.common.OrderStatus;
import com.xingguang.delivery.common.PageResult;
import com.xingguang.delivery.entity.*;
import com.xingguang.delivery.mapper.*;
import com.xingguang.delivery.support.CurrentAdmin;
import com.xingguang.delivery.support.VoConverter;
import com.xingguang.delivery.vo.OrderVO;
import com.xingguang.delivery.vo.ShopVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 平台运营后台（高价值优先：订单管理 + 商家管理 + 数据看板；骑手调度 / 用户管理只读）。
 *
 * <p>写操作（入驻审核 / 菜品上下架 / 异常订单标记 / 取消）受 RBAC 约束，
 * 仅 SUPER_ADMIN / OPERATOR 可操作，CS 仅只读。
 */
@Service
public class AdminService {

    private final ShopMapper shopMapper;
    private final DishMapper dishMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantApplicationMapper applicationMapper;
    private final ComplaintMapper complaintMapper;
    private final RiderMapper riderMapper;
    private final AppUserMapper appUserMapper;
    private final AdminUserMapper adminUserMapper;
    private final VoConverter converter;
    private final OrderService orderService;
    private final CurrentAdmin currentAdmin;

    public AdminService(ShopMapper shopMapper, DishMapper dishMapper, OrdersMapper ordersMapper,
                        OrderItemMapper orderItemMapper, PaymentMapper paymentMapper,
                        MerchantMapper merchantMapper, MerchantApplicationMapper applicationMapper,
                        ComplaintMapper complaintMapper, RiderMapper riderMapper,
                        AppUserMapper appUserMapper, AdminUserMapper adminUserMapper,
                        VoConverter converter, OrderService orderService, CurrentAdmin currentAdmin) {
        this.shopMapper = shopMapper;
        this.dishMapper = dishMapper;
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentMapper = paymentMapper;
        this.merchantMapper = merchantMapper;
        this.applicationMapper = applicationMapper;
        this.complaintMapper = complaintMapper;
        this.riderMapper = riderMapper;
        this.appUserMapper = appUserMapper;
        this.adminUserMapper = adminUserMapper;
        this.converter = converter;
        this.orderService = orderService;
        this.currentAdmin = currentAdmin;
    }

    /** 工作台数据卡 + 状态分布 */
    public Map<String, Object> overview() {
        List<Orders> all = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery());
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long todayOrders = all.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(todayStart)).count();
        long todayGmv = all.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(todayStart))
                .filter(o -> !OrderStatus.UNPAID.name().equals(o.getStatus())
                        && !OrderStatus.CANCELED.name().equals(o.getStatus()))
                .mapToLong(o -> o.getPayAmount() == null ? 0 : o.getPayAmount()).sum();
        long totalGmv = all.stream()
                .filter(o -> !OrderStatus.UNPAID.name().equals(o.getStatus())
                        && !OrderStatus.CANCELED.name().equals(o.getStatus()))
                .mapToLong(o -> o.getPayAmount() == null ? 0 : o.getPayAmount()).sum();
        long processing = all.stream().filter(o -> {
            OrderStatus s = OrderStatus.of(o.getStatus());
            return s != null && "进行中".equals(s.getGroup());
        }).count();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("todayOrderCount", todayOrders);
        map.put("todayGmv", todayGmv);
        map.put("avgOrderAmount", todayOrders == 0 ? 0 : todayGmv / todayOrders);
        map.put("processingOrderCount", processing);
        map.put("totalOrderCount", all.size());
        map.put("totalGmv", totalGmv);
        map.put("shopCount", shopMapper.selectCount(Wrappers.<Shop>lambdaQuery()));
        map.put("merchantCount", merchantMapper.selectCount(Wrappers.<Merchant>lambdaQuery()));
        map.put("riderCount", riderMapper.selectCount(Wrappers.<Rider>lambdaQuery()));
        map.put("userCount", appUserMapper.selectCount(Wrappers.<AppUser>lambdaQuery()));
        map.put("dishCount", dishMapper.selectCount(Wrappers.<Dish>lambdaQuery()));
        map.put("pickupOrderCount", all.stream()
                .filter(o -> "PICKUP".equals(o.getFulfillType())).count());

        Map<String, Long> statusDist = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            long count = all.stream().filter(o -> s.name().equals(o.getStatus())).count();
            if (count > 0) {
                statusDist.put(s.getLabel(), count);
            }
        }
        map.put("statusDistribution", statusDist);
        return map;
    }

    /** 近 N 日营业额 / 订单量趋势 + 平均配送时长 */
    public Map<String, Object> dashboardTrend(int days) {
        int n = Math.min(Math.max(days, 1), 30);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(n - 1);

        List<Orders> all = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .ge(Orders::getCreatedAt, start.atStartOfDay())
                .le(Orders::getCreatedAt, end.plusDays(1).atStartOfDay()));

        Map<LocalDate, Long> gmvByDay = new LinkedHashMap<>();
        Map<LocalDate, Long> cntByDay = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            gmvByDay.put(d, 0L);
            cntByDay.put(d, 0L);
        }
        for (Orders o : all) {
            if (o.getCreatedAt() == null) {
                continue;
            }
            LocalDate d = o.getCreatedAt().toLocalDate();
            if (OrderStatus.UNPAID.name().equals(o.getStatus()) || OrderStatus.CANCELED.name().equals(o.getStatus())) {
                continue;
            }
            gmvByDay.merge(d, (long) (o.getPayAmount() == null ? 0 : o.getPayAmount()), Long::sum);
            cntByDay.merge(d, 1L, Long::sum);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<Map<String, Object>> gmvTrend = new ArrayList<>();
        List<Map<String, Object>> orderTrend = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("date", d.format(fmt));
            g.put("value", gmvByDay.get(d));
            gmvTrend.add(g);
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("date", d.format(fmt));
            c.put("value", cntByDay.get(d));
            orderTrend.add(c);
        }

        // 平均配送时长：已送达/完成 的配送单（finished - paid）
        double avgDelivery = all.stream()
                .filter(o -> "DELIVERY".equals(o.getFulfillType()))
                .filter(o -> o.getPaidAt() != null && o.getFinishedAt() != null)
                .filter(o -> OrderStatus.DELIVERED.name().equals(o.getStatus())
                        || OrderStatus.COMPLETED.name().equals(o.getStatus()))
                .mapToLong(o -> java.time.Duration.between(o.getPaidAt(), o.getFinishedAt()).toMinutes())
                .average().orElse(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gmvTrend", gmvTrend);
        result.put("orderTrend", orderTrend);
        result.put("avgDeliveryMinutes", Math.round(avgDelivery));
        return result;
    }

    /** 订单列表（只读，多条件筛选） */
    public PageResult<OrderVO> orders(String keyword, String status, Long pageNum, Long pageSize) {
        long pn = PageResult.normalizePageNum(pageNum);
        long ps = PageResult.normalizePageSize(pageSize);

        LambdaQueryWrapper<Orders> q = Wrappers.<Orders>lambdaQuery()
                .orderByDesc(Orders::getCreatedAt);
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            q.eq(Orders::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            q.and(w -> w.like(Orders::getOrderNo, kw).or().like(Orders::getShopName, kw));
        }
        IPage<Orders> page = ordersMapper.selectPage(new Page<>(pn, ps), q);
        List<OrderVO> list = page.getRecords().stream()
                .map(o -> orderService.toVO(o, false))
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    /** 订单详情（后台，不受用户归属限制），含明细 / 状态时间轴 / 金额 */
    public OrderVO orderDetail(String orderNo) {
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery().eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw com.xingguang.delivery.common.BizException.order("订单不存在：" + orderNo);
        }
        return orderService.toVO(order, true);
    }

    /** 标记异常订单（仅管理角色） */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public OrderVO markException(String orderNo, String note) {
        currentAdmin.requireManage();
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery().eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw com.xingguang.delivery.common.BizException.order("订单不存在：" + orderNo);
        }
        OrderStatus cur = OrderStatus.of(order.getStatus());
        if (cur != null && ("COMPLETED".equals(order.getStatus()) || "CANCELED".equals(order.getStatus())
                || "REFUNDED".equals(order.getStatus()) || "EXCEPTION".equals(order.getStatus()))) {
            throw com.xingguang.delivery.common.BizException.order("该订单状态不可标记为异常");
        }
        order.setStatus(OrderStatus.EXCEPTION.name());
        ordersMapper.updateById(order);
        orderService.addLog(order, cur == null ? null : cur.name(), OrderStatus.EXCEPTION.name(),
                "ADMIN", "平台标记异常：" + (note == null ? "" : note));
        return orderService.toVO(order, false);
    }

    /** 后台强制取消订单（仅管理角色） */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public OrderVO adminCancel(String orderNo, String reason) {
        currentAdmin.requireManage();
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery().eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw com.xingguang.delivery.common.BizException.order("订单不存在：" + orderNo);
        }
        OrderStatus cur = OrderStatus.of(order.getStatus());
        if (cur != null && ("COMPLETED".equals(order.getStatus()) || "CANCELED".equals(order.getStatus())
                || "REFUNDED".equals(order.getStatus()))) {
            throw com.xingguang.delivery.common.BizException.order("该订单已终态，不可取消");
        }
        // 后台取消同样要退款：否则用户付款后被平台单方面取消，钱会凭空消失
        boolean refunded = orderService.refundIfPaid(order);

        order.setStatus(OrderStatus.CANCELED.name());
        order.setCancelReason(reason);
        ordersMapper.updateById(order);
        orderService.addLog(order, cur == null ? null : cur.name(), OrderStatus.CANCELED.name(),
                "ADMIN", "平台取消订单：" + (reason == null ? "" : reason) + (refunded ? "（已原路退款）" : ""));
        return orderService.toVO(order, false);
    }

    /** 门店与商品（只读） */
    public List<Map<String, Object>> shops() {
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery().orderByAsc(Shop::getId));
        Map<Long, Merchant> merchants = merchantMapper.selectList(Wrappers.<Merchant>lambdaQuery())
                .stream().collect(Collectors.toMap(Merchant::getId, m -> m));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Shop s : shops) {
            ShopVO vo = converter.toShopVO(s);
            long dishCount = dishMapper.selectCount(Wrappers.<Dish>lambdaQuery()
                    .eq(Dish::getShopId, s.getId()));
            long orderCount = ordersMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getShopId, s.getId()));
            Merchant m = merchants.get(s.getMerchantId());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("shop", vo);
            row.put("merchantName", m == null ? "-" : m.getName());
            row.put("commissionRate", m == null ? null : m.getCommissionRate());
            row.put("dishCount", dishCount);
            row.put("orderCount", orderCount);
            list.add(row);
        }
        return list;
    }

    /** 门店商品明细（只读） */
    public List<Dish> dishes(Long shopId) {
        return dishMapper.selectList(Wrappers.<Dish>lambdaQuery()
                .eq(shopId != null, Dish::getShopId, shopId)
                .orderByAsc(Dish::getCategoryId).orderByAsc(Dish::getSort));
    }

    /** 支付流水（只读） */
    public List<Payment> payments(Long pageNum, Long pageSize) {
        long pn = PageResult.normalizePageNum(pageNum);
        long ps = PageResult.normalizePageSize(pageSize);
        return paymentMapper.selectPage(new Page<>(pn, ps),
                Wrappers.<Payment>lambdaQuery().orderByDesc(Payment::getCreatedAt)).getRecords();
    }

    /** 菜品上 / 下架切换（仅管理角色） */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Dish toggleDishStatus(Long dishId) {
        currentAdmin.requireManage();
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw com.xingguang.delivery.common.BizException.product("菜品不存在：" + dishId);
        }
        String next = "ON_SALE".equals(dish.getStatus()) ? "OFF_SHELF" : "ON_SALE";
        dish.setStatus(next);
        dishMapper.updateById(dish);
        return dish;
    }

    /** 评分监控：门店评分 + 低分预警（只读） */
    public List<Map<String, Object>> ratingMonitor() {
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery().orderByAsc(Shop::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Shop s : shops) {
            long orderCount = ordersMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getShopId, s.getId()));
            long complaintCount = complaintMapper.selectCount(Wrappers.<Complaint>lambdaQuery()
                    .eq(Complaint::getOrderNo, "").or().like(Complaint::getContent, s.getName()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("shopId", s.getId());
            row.put("shopName", s.getName());
            row.put("rating", s.getRating());
            row.put("monthSales", s.getMonthSales());
            row.put("orderCount", orderCount);
            row.put("complaintCount", complaintCount);
            row.put("lowRating", s.getRating() != null && s.getRating().doubleValue() < 4.5);
            list.add(row);
        }
        return list;
    }

    /** 商家入驻申请列表（只读） */
    public List<MerchantApplication> applications(String status) {
        LambdaQueryWrapper<MerchantApplication> q = Wrappers.<MerchantApplication>lambdaQuery()
                .orderByDesc(MerchantApplication::getCreatedAt);
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            q.eq(MerchantApplication::getStatus, status);
        }
        return applicationMapper.selectList(q);
    }

    /** 入驻审核：通过（仅管理角色） */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public MerchantApplication approveApplication(Long id) {
        currentAdmin.requireManage();
        MerchantApplication app = applicationMapper.selectById(id);
        if (app == null) {
            throw com.xingguang.delivery.common.BizException.shop("申请不存在：" + id);
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw com.xingguang.delivery.common.BizException.shop("该申请已处理");
        }
        app.setStatus("APPROVED");
        app.setReviewerId(currentAdmin.id());
        app.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(app);
        return app;
    }

    /** 入驻审核：驳回（仅管理角色） */
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public MerchantApplication rejectApplication(Long id, String reason) {
        currentAdmin.requireManage();
        MerchantApplication app = applicationMapper.selectById(id);
        if (app == null) {
            throw com.xingguang.delivery.common.BizException.shop("申请不存在：" + id);
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw com.xingguang.delivery.common.BizException.shop("该申请已处理");
        }
        app.setStatus("REJECTED");
        app.setRejectReason(reason);
        app.setReviewerId(currentAdmin.id());
        app.setReviewedAt(LocalDateTime.now());
        applicationMapper.updateById(app);
        return app;
    }

    /** 骑手调度概览（只读）：待分配配送单 + 骑手池 */
    public Map<String, Object> dispatchOverview() {
        List<Orders> pending = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getFulfillType, "DELIVERY")
                .isNull(Orders::getRiderId)
                .in(Orders::getStatus, OrderStatus.PREPARING.name(), OrderStatus.WAIT_PICKUP.name())
                .orderByAsc(Orders::getCreatedAt));
        List<Rider> riders = riderMapper.selectList(Wrappers.<Rider>lambdaQuery()
                .orderByDesc(Rider::getStatus).orderByDesc(Rider::getCompletedCount));

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> riderRows = new ArrayList<>();
        for (Rider r : riders) {
            long todayDeliveries = ordersMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getRiderId, r.getId())
                    .in(Orders::getStatus, OrderStatus.DELIVERING.name(), OrderStatus.DELIVERED.name(),
                            OrderStatus.COMPLETED.name())
                    .ge(Orders::getCreatedAt, today.atStartOfDay()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rider", r);
            row.put("todayDeliveries", todayDeliveries);
            riderRows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pendingOrders", pending.stream().map(o -> orderService.toVO(o, false)).collect(Collectors.toList()));
        result.put("riders", riderRows);
        result.put("pendingCount", pending.size());
        return result;
    }

    /** 用户列表（只读） */
    public PageResult<AppUser> users(String keyword, Long pageNum, Long pageSize) {
        long pn = PageResult.normalizePageNum(pageNum);
        long ps = PageResult.normalizePageSize(pageSize);
        LambdaQueryWrapper<AppUser> q = Wrappers.<AppUser>lambdaQuery().orderByDesc(AppUser::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            q.and(w -> w.like(AppUser::getPhone, kw).or().like(AppUser::getNickname, kw));
        }
        IPage<AppUser> page = appUserMapper.selectPage(new Page<>(pn, ps), q);
        return PageResult.of(page.getTotal(), pn, ps, page.getRecords());
    }

    /** 投诉列表（只读） */
    public PageResult<Map<String, Object>> complaints(String status, String type, Long pageNum, Long pageSize) {
        long pn = PageResult.normalizePageNum(pageNum);
        long ps = PageResult.normalizePageSize(pageSize);
        LambdaQueryWrapper<Complaint> q = Wrappers.<Complaint>lambdaQuery().orderByDesc(Complaint::getCreatedAt);
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            q.eq(Complaint::getStatus, status);
        }
        if (type != null && !type.isBlank() && !"ALL".equals(type)) {
            q.eq(Complaint::getType, type);
        }
        IPage<Complaint> page = complaintMapper.selectPage(new Page<>(pn, ps), q);
        Map<Long, String> handlerNames = adminUserMapper.selectList(Wrappers.<AdminUser>lambdaQuery())
                .stream().collect(Collectors.toMap(AdminUser::getId, AdminUser::getName, (a, b) -> a));
        List<Map<String, Object>> list = page.getRecords().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("complaint", c);
            m.put("handlerName", c.getHandlerId() == null ? null : handlerNames.get(c.getHandlerId()));
            return m;
        }).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    /** 运营账号登录（原型：MD5 比对，默认密码 123456）。真实实现应签发 JWT。 */
    public AdminUser login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw com.xingguang.delivery.common.BizException.of("账号或密码不能为空");
        }
        AdminUser u = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, username.trim()));
        if (u == null || (u.getDeleted() != null && u.getDeleted() == 1)) {
            throw com.xingguang.delivery.common.BizException.of("账号不存在");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw com.xingguang.delivery.common.BizException.of("账号已停用");
        }
        if (!md5Hex(password).equalsIgnoreCase(u.getPassword())) {
            throw com.xingguang.delivery.common.BizException.of("密码错误");
        }
        u.setLastLogin(LocalDateTime.now());
        adminUserMapper.updateById(u);
        return u;
    }

    private static String md5Hex(String src) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(src.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 状态模拟推进（演示用，逻辑与商家端 / 骑手端操作一致；需管理角色，S4 修复） */
    public OrderVO advance(String orderNo) {
        currentAdmin.requireManage();
        return orderService.advance(orderNo);
    }
}

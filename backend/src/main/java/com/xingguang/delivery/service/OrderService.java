package com.xingguang.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xingguang.delivery.common.*;
import com.xingguang.delivery.domain.AmountCalculator;
import com.xingguang.delivery.domain.LineItem;
import com.xingguang.delivery.domain.OrderStateMachine;
import com.xingguang.delivery.dto.OrderSubmitRequest;
import com.xingguang.delivery.entity.*;
import com.xingguang.delivery.mapper.*;
import com.xingguang.delivery.pay.PaymentChannel;
import com.xingguang.delivery.pay.PaymentChannelRegistry;
import com.xingguang.delivery.service.ObjectStorageService;
import com.xingguang.delivery.support.VoConverter;
import com.xingguang.delivery.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务：试算 → 下单 → 状态机流转。
 *
 * <p>关键约定：
 * <ul>
 *   <li>金额一律由服务端计算一次（{@link AmountCalculator}），提交订单时按最新价重算防篡改；</li>
 *   <li>下单接口支持 Idempotency-Key 幂等，重复提交返回同一订单；</li>
 *   <li>所有状态流转必须通过 {@link OrderStateMachine} 校验，防止跳状态 / 并发错乱；</li>
 *   <li>订单与购物车条目的归属校验（水平越权防护）。</li>
 * </ul>
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 自取单不派单、不生成配送单，状态机替换见 SRS 8.1 / 8.9 */
    private static final Map<String, List<String>> GROUP_STATUS = new LinkedHashMap<>();

    static {
        GROUP_STATUS.put("待付款", List.of("UNPAID"));
        GROUP_STATUS.put("待接单", List.of("WAIT_ACCEPT"));
        GROUP_STATUS.put("进行中", List.of("PREPARING", "WAIT_PICKUP", "DELIVERING",
                "DELIVERED", "WAIT_MEAL", "PICKED", "EXCEPTION"));
        GROUP_STATUS.put("待评价", List.of("COMPLETED"));
        GROUP_STATUS.put("退款售后", List.of("CANCELED", "REFUNDED", "PART_REFUNDED"));
    }

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper logMapper;
    private final ShopMapper shopMapper;
    private final UserAddressMapper addressMapper;
    private final IdempotencyRecordMapper idemMapper;
    private final PaymentMapper paymentMapper;
    private final PaymentChannelRegistry channelRegistry;
    private final CartService cartService;
    private final VoConverter converter;
    private final ObjectStorageService storage;
    private final AmapGeocodeService geocode;

    public OrderService(OrdersMapper ordersMapper, OrderItemMapper orderItemMapper,
                        OrderStatusLogMapper logMapper, ShopMapper shopMapper,
                        UserAddressMapper addressMapper, IdempotencyRecordMapper idemMapper,
                        PaymentMapper paymentMapper, PaymentChannelRegistry channelRegistry,
                        CartService cartService, VoConverter converter, ObjectStorageService storage,
                        AmapGeocodeService geocode) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.logMapper = logMapper;
        this.shopMapper = shopMapper;
        this.addressMapper = addressMapper;
        this.idemMapper = idemMapper;
        this.paymentMapper = paymentMapper;
        this.channelRegistry = channelRegistry;
        this.cartService = cartService;
        this.converter = converter;
        this.storage = storage;
        this.geocode = geocode;
    }

    /** 同 VoConverter.resolveMedia：真实 objectKey 解析为 MinIO 直链，渐变占位/空值原样返回。 */
    private String resolveMedia(String key) {
        if (key == null || key.isBlank() || !key.contains("/")) {
            return key;
        }
        FileVO vo = new FileVO();
        vo.setObjectKey(key);
        storage.applyUrl(vo);
        return vo.getUrl();
    }

    // ==================================================================
    // 试算
    // ==================================================================

    /** 结算页试算：切换配送 / 自取只会重算费用与预计时间，不创建订单。 */
    public OrderPreviewVO preview(long userId, OrderSubmitRequest req) {
        FulfillType fulfillType = FulfillType.of(req.getFulfillType());
        Shop shop = requireShop(req.getShopId());
        checkFulfillSupported(shop, fulfillType);

        List<CartItem> cartItems = cartService.listItems(userId, req.getShopId(), req.getItemIds());
        if (cartItems.isEmpty()) {
            throw BizException.order("购物车为空，请先选择商品");
        }
        List<LineItem> lines = cartService.toLineItems(cartItems);
        AmountVO amount = AmountCalculator.calculate(shop, fulfillType, lines);

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setShop(converter.toShopVO(shop));
        vo.setFulfillType(fulfillType.name());
        vo.setFulfillTypeLabel(fulfillType.getLabel());
        vo.setDeliverySupported(true);
        vo.setPickupSupported(shop.getPickupEnabled() != null && shop.getPickupEnabled() == 1);

        List<OrderPreviewVO.Line> items = new ArrayList<>();
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem c = cartItems.get(i);
            LineItem l = lines.get(i);
            OrderPreviewVO.Line line = new OrderPreviewVO.Line();
            line.setCartItemId(c.getId());
            line.setDishId(l.getDishId());
            line.setDishName(l.getDishName());
            line.setDishImage(l.getDishImage());
            line.setSpecText(l.getSpecText());
            line.setUnitPrice(l.getUnitPrice());
            line.setQuantity(l.getQuantity());
            line.setAmount(l.amount());
            items.add(line);
        }
        vo.setItems(items);
        vo.setAmount(amount);

        if (fulfillType == FulfillType.DELIVERY) {
            UserAddress address = resolveAddress(userId, req.getAddressId());
            AddressVO av = new AddressVO();
            av.setId(address.getId());
            av.setContact(address.getContact());
            av.setPhoneMasked(Mask.phone(address.getPhone()));
            av.setDetail(address.getDetail());
            av.setTag(address.getTag());
            av.setIsDefault(address.getIsDefault() != null && address.getIsDefault() == 1);
            vo.setAddress(av);
            vo.setPromiseMinutes(shop.getEtaMinutes());
            vo.setPromiseText("预计 " + shop.getEtaMinutes() + " 分钟送达");
        } else {
            vo.setPromiseMinutes(prepMinutes(shop));
            vo.setPromiseText("预计 " + prepMinutes(shop) + " 分钟备餐完成");
            vo.setPickupTip("备餐完成后凭取餐码到店取餐，自取单免配送费");
        }
        return vo;
    }

    // ==================================================================
    // 下单
    // ==================================================================

    @Transactional(rollbackFor = Exception.class)
    public OrderVO submit(long userId, OrderSubmitRequest req, String idempotencyKey) {
        // 1) 幂等：同一 key 重复提交返回同一订单（SRS 9.6）
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord exist = idemMapper.selectOne(Wrappers.<IdempotencyRecord>lambdaQuery()
                    .eq(IdempotencyRecord::getIdemKey, idempotencyKey)
                    .eq(IdempotencyRecord::getScene, "ORDER_CREATE"));
            if (exist != null) {
                log.info("[幂等命中] 重复下单 key={} orderNo={}", idempotencyKey, exist.getResultRef());
                return detail(userId, exist.getResultRef());
            }
        }

        FulfillType fulfillType = FulfillType.of(req.getFulfillType());
        Shop shop = requireShop(req.getShopId());
        checkFulfillSupported(shop, fulfillType);

        List<CartItem> cartItems = cartService.listItems(userId, req.getShopId(), req.getItemIds());
        if (cartItems.isEmpty()) {
            throw BizException.order("购物车为空，请先选择商品");
        }

        // 2) 服务端按最新价重算（防前端篡改单价 / 金额）
        List<LineItem> lines = cartService.toLineItems(cartItems);
        AmountVO amount = AmountCalculator.calculate(shop, fulfillType, lines);

        // 3) 起送价校验（按商品小计）
        if (Boolean.FALSE.equals(amount.getReachMin())) {
            throw BizException.order("差 ¥" + AmountCalculator.yuan(amount.getMinPriceGap())
                    + " 起送，请再选点商品");
        }

        // 4) 收货信息 / 门店自取信息
        String receiverName;
        String receiverPhone;
        String receiverAddress;
        if (fulfillType == FulfillType.DELIVERY) {
            UserAddress address = resolveAddress(userId, req.getAddressId());
            receiverName = address.getContact();
            receiverPhone = Mask.phone(address.getPhone());
            receiverAddress = address.getDetail();
        } else {
            receiverName = "到店自取";
            receiverPhone = null;
            receiverAddress = shop.getName() + "（" + shop.getAddress() + "）";
        }

        // 5) 落订单
        LocalDateTime now = LocalDateTime.now();
        Orders order = new Orders();
        order.setOrderNo(genOrderNo());
        order.setUserId(userId);
        order.setShopId(shop.getId());
        order.setShopName(shop.getName());
        order.setShopHeaderImg(shop.getHeaderImg());
        order.setFulfillType(fulfillType.name());
        order.setStatus(OrderStatus.UNPAID.name());
        order.setItemAmount(amount.getItemAmount());
        order.setPackFee(amount.getPackFee());
        order.setDeliveryFee(amount.getDeliveryFee());
        order.setDiscountAmount(amount.getDiscountAmount());
        order.setPayAmount(amount.getPayAmount());
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setRemark(req.getRemark());
        order.setPromiseMinutes(fulfillType == FulfillType.DELIVERY
                ? shop.getEtaMinutes() : prepMinutes(shop));
        order.setCreatedAt(now);
        ordersMapper.insert(order);

        for (LineItem line : lines) {
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setOrderNo(order.getOrderNo());
            oi.setDishId(line.getDishId());
            oi.setDishName(line.getDishName());
            oi.setDishImage(line.getDishImage());
            oi.setSpecText(line.getSpecText());
            oi.setUnitPrice(line.getUnitPrice());
            oi.setQuantity(line.getQuantity());
            oi.setAmount(line.amount());
            orderItemMapper.insert(oi);
        }

        addLog(order, null, OrderStatus.UNPAID.name(), "USER",
                fulfillType == FulfillType.PICKUP ? "提交订单（到店自取）" : "提交订单（平台配送）");

        // 6) 清掉已下单的购物车条目
        List<Long> usedIds = cartItems.stream().map(CartItem::getId).toList();
        cartService.listItems(userId, shop.getId(), usedIds)
                .forEach(c -> cartService.remove(userId, c.getId()));

        // 7) 记录幂等
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setIdemKey(idempotencyKey);
            record.setScene("ORDER_CREATE");
            record.setResultRef(order.getOrderNo());
            idemMapper.insert(record);
        }

        log.info("[下单成功] orderNo={} 履约={} 实付={}分", order.getOrderNo(), fulfillType, order.getPayAmount());
        return detail(userId, order.getOrderNo());
    }

    // ==================================================================
    // 查询
    // ==================================================================

    public PageResult<OrderVO> list(long userId, String group, Long pageNum, Long pageSize) {
        long pn = PageResult.normalizePageNum(pageNum);
        long ps = PageResult.normalizePageSize(pageSize);

        LambdaQueryWrapper<Orders> q = Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getUserId, userId)
                .orderByDesc(Orders::getCreatedAt);
        if (group != null && !group.isBlank() && !"全部".equals(group)) {
            List<String> statuses = GROUP_STATUS.get(group);
            if (statuses == null) {
                throw new BizException(10001, "未知的订单分组：" + group);
            }
            q.in(Orders::getStatus, statuses);
        }

        IPage<Orders> page = ordersMapper.selectPage(new Page<>(pn, ps), q);
        List<OrderVO> list = page.getRecords().stream()
                .map(o -> toVO(o, false))
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    public OrderVO detail(long userId, String orderNo) {
        Orders order = requireOrder(orderNo);
        if (!Objects.equals(order.getUserId(), userId)) {
            // 水平越权防护
            throw new BizException(10003, "无权查看他人订单");
        }
        return toVO(order, true);
    }

    /** 订单分组统计（订单页 Tab 角标） */
    public Map<String, Long> groupCount(long userId) {
        List<Orders> all = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getUserId, userId));
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : GROUP_STATUS.entrySet()) {
            result.put(e.getKey(), all.stream()
                    .filter(o -> e.getValue().contains(o.getStatus())).count());
        }
        return result;
    }

    // ==================================================================
    // 状态流转（用户侧）
    // ==================================================================

    /** 取消订单：仅待支付 / 待接单 / 备餐中可取消。已付款的先退款再改状态。 */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancel(long userId, String orderNo, String reason) {
        Orders order = requireOwned(userId, orderNo);
        FulfillType type = FulfillType.of(order.getFulfillType());
        OrderStateMachine.require(type, order.getStatus(), OrderStatus.CANCELED);

        String from = order.getStatus();
        // 先退款再落状态：退款抛异常时事务回滚，订单仍是原状态，用户可以重试，
        // 不会出现「订单已取消但钱没退」的中间态。
        boolean refunded = refundIfPaid(order);

        order.setStatus(OrderStatus.CANCELED.name());
        order.setCancelReason(reason == null || reason.isBlank() ? "用户主动取消" : reason);
        ordersMapper.updateById(order);
        addLog(order, from, OrderStatus.CANCELED.name(), "USER",
                order.getCancelReason() + (refunded ? "（已原路退款）" : ""));
        return toVO(order, true);
    }

    /**
     * 已付款订单取消时原路退回。供用户取消与后台强制取消共用，避免两条路径退款口径不一致。
     *
     * @return 是否发生了退款
     */
    boolean refundIfPaid(Orders order) {
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, order.getId())
                .eq(Payment::getStatus, "SUCCESS")
                .orderByDesc(Payment::getPaidAt)
                .last("limit 1"));
        if (payment == null) {
            return false;
        }
        PaymentChannel channel = channelRegistry.get(PayChannel.of(payment.getChannel()));
        channel.onRefund(payment, order);

        payment.setStatus("REFUNDED");
        payment.setRefundedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);
        log.info("[订单取消退款] orderNo={} payNo={} channel={} 退回 {} 分",
                order.getOrderNo(), payment.getPayNo(), payment.getChannel(), payment.getAmount());
        return true;
    }

    /** 催单：限频 60 秒（阈值后台可配，SRS 4.4） */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO urge(long userId, String orderNo) {
        Orders order = requireOwned(userId, orderNo);
        OrderStatusLog last = logMapper.selectOne(Wrappers.<OrderStatusLog>lambdaQuery()
                .eq(OrderStatusLog::getOrderId, order.getId())
                .eq(OrderStatusLog::getToStatus, order.getStatus())
                .eq(OrderStatusLog::getOperator, "USER")
                .likeRight(OrderStatusLog::getRemark, "催单")
                .orderByDesc(OrderStatusLog::getCreatedAt)
                .last("limit 1"));
        if (last != null && last.getCreatedAt() != null
                && last.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
            throw BizException.order("催单太频繁啦，请 1 分钟后再试");
        }
        addLog(order, order.getStatus(), order.getStatus(), "USER", "催单：请商家尽快出餐");
        return toVO(order, true);
    }

    /** 确认收货 / 我已取餐 */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO confirm(long userId, String orderNo) {
        Orders order = requireOwned(userId, orderNo);
        FulfillType type = FulfillType.of(order.getFulfillType());
        String from = order.getStatus();

        OrderStatus to = OrderStatus.COMPLETED;
        if (type == FulfillType.DELIVERY && !OrderStatus.DELIVERED.name().equals(from)) {
            throw BizException.order("订单尚未送达，暂时无法确认收货");
        }
        if (type == FulfillType.PICKUP && !OrderStatus.PICKED.name().equals(from)) {
            throw BizException.order("请先完成取餐后再确认");
        }
        OrderStateMachine.require(type, from, to);

        order.setStatus(to.name());
        order.setFinishedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        addLog(order, from, to.name(), "USER",
                type == FulfillType.DELIVERY ? "用户确认收货" : "我已取餐");
        return toVO(order, true);
    }

    /** 自取单：用户点「我已取餐」（等商家核销）→ 演示环境直接推进 */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO pickUp(long userId, String orderNo) {
        Orders order = requireOwned(userId, orderNo);
        if (!FulfillType.PICKUP.name().equals(order.getFulfillType())) {
            throw BizException.order("仅到店自取订单支持该操作");
        }
        FulfillType type = FulfillType.PICKUP;
        String from = order.getStatus();
        OrderStateMachine.require(type, from, OrderStatus.PICKED);
        order.setStatus(OrderStatus.PICKED.name());
        ordersMapper.updateById(order);
        addLog(order, from, OrderStatus.PICKED.name(), "MERCHANT", "取餐码核销成功");
        return toVO(order, true);
    }

    // ==================================================================
    // 支付回调（由 PayService 调用）
    // ==================================================================

    /** 支付成功后推进订单：待支付 → 待接单 */
    @Transactional(rollbackFor = Exception.class)
    public void onPaid(Orders order, Payment payment) {
        OrderStateMachine.require(FulfillType.of(order.getFulfillType()), order.getStatus(),
                OrderStatus.WAIT_ACCEPT);
        String from = order.getStatus();
        order.setStatus(OrderStatus.WAIT_ACCEPT.name());
        order.setPaidAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        addLog(order, from, OrderStatus.WAIT_ACCEPT.name(), "USER",
                "支付成功（" + payment.getChannel() + "）¥" + AmountCalculator.yuan(payment.getAmount()));
    }

    // ==================================================================
    // 模拟推进（本原型不含商家端 / 骑手端，由后台一键推进，逻辑与端上操作一致）
    // ==================================================================

    @Transactional(rollbackFor = Exception.class)
    public OrderVO advance(String orderNo) {
        Orders order = requireOrder(orderNo);
        FulfillType type = FulfillType.of(order.getFulfillType());
        OrderStatus to = OrderStateMachine.nextBySimulation(type, order.getStatus());
        if (to == null) {
            throw BizException.order("当前状态「" + OrderStatus.labelOf(order.getStatus()) + "」无可推进的下一步");
        }
        String from = order.getStatus();
        order.setStatus(to.name());
        if (to == OrderStatus.WAIT_MEAL) {
            // 备餐完成 → 下发取餐码（SRS 5.3.8 / 8.6）
            order.setPickupCode(genPickupCode());
        }
        if (to == OrderStatus.COMPLETED) {
            order.setFinishedAt(LocalDateTime.now());
        }
        ordersMapper.updateById(order);

        String remark = switch (to) {
            case PREPARING -> "商家接单";
            case WAIT_PICKUP -> "商家出餐完成，等待骑手取货";
            case DELIVERING -> "骑手已取货，开始配送";
            case DELIVERED -> "骑手已送达";
            case WAIT_MEAL -> "备餐完成，取餐码已下发：" + order.getPickupCode();
            case PICKED -> "取餐码核销成功";
            case COMPLETED -> "订单完成";
            default -> "状态推进";
        };
        addLog(order, from, to.name(), OrderStateMachine.operatorOf(to), remark);
        log.info("[状态推进] orderNo={} {} -> {}", orderNo, from, to.name());
        return toVO(order, true);
    }

    // ==================================================================
    // 内部工具
    // ==================================================================

    public Orders requireOrder(String orderNo) {
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw BizException.order("订单不存在：" + orderNo);
        }
        return order;
    }

    /**
     * 统一的「状态机校验 + 落库 + 写流水」推进，供订单域与骑手端共用。
     *
     * @param order   待推进订单（方法内直接 updateById）
     * @param to      目标状态（必须经过 {@link OrderStateMachine} 校验）
     * @param operator 操作方 USER/MERCHANT/RIDER/ADMIN/SYSTEM
     * @param remark   流水备注
     */
    @Transactional(rollbackFor = Exception.class)
    public void transferStatus(Orders order, OrderStatus to, String operator, String remark) {
        FulfillType type = FulfillType.of(order.getFulfillType());
        OrderStateMachine.require(type, order.getStatus(), to);
        String from = order.getStatus();
        order.setStatus(to.name());
        ordersMapper.updateById(order);
        addLog(order, from, to.name(), operator, remark);
    }

    private Orders requireOwned(long userId, String orderNo) {
        Orders order = requireOrder(orderNo);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BizException(10003, "无权操作他人订单");
        }
        return order;
    }

    private Shop requireShop(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw BizException.shop("门店不存在");
        }
        if (!"OPEN".equals(shop.getStatus())) {
            throw BizException.shop("门店已打烊，暂时无法下单");
        }
        return shop;
    }

    private void checkFulfillSupported(Shop shop, FulfillType type) {
        if (type == FulfillType.PICKUP
                && (shop.getPickupEnabled() == null || shop.getPickupEnabled() != 1)) {
            throw BizException.shop("该门店不支持到店自取");
        }
    }

    private UserAddress resolveAddress(long userId, Long addressId) {
        if (addressId == null) {
            UserAddress def = addressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery()
                    .eq(UserAddress::getUserId, userId)
                    .eq(UserAddress::getIsDefault, 1)
                    .last("limit 1"));
            if (def != null) {
                return def;
            }
            throw BizException.order("请先选择收货地址");
        }
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null) {
            throw BizException.order("收货地址不存在");
        }
        if (!Objects.equals(address.getUserId(), userId)) {
            throw new BizException(10003, "无权使用他人地址");
        }
        return address;
    }

    private int prepMinutes(Shop shop) {
        int eta = shop.getEtaMinutes() == null ? 25 : shop.getEtaMinutes();
        return Math.max(10, eta - 12);
    }

    private String genOrderNo() {
        for (int i = 0; i < 5; i++) {
            String no = "SG" + LocalDateTime.now().format(NO_FMT)
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (ordersMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getOrderNo, no)) == 0) {
                return no;
            }
        }
        throw BizException.order("订单号生成失败，请重试");
    }

    /** 取餐码规则：前缀 SG- + 4 位数字（前缀 / 位数 / 有效期后台可配，SRS 4.2） */
    private String genPickupCode() {
        return "SG-" + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    public void addLog(Orders order, String from, String to, String operator, String remark) {
        OrderStatusLog logRow = new OrderStatusLog();
        logRow.setOrderId(order.getId());
        logRow.setOrderNo(order.getOrderNo());
        logRow.setFromStatus(from);
        logRow.setToStatus(to);
        logRow.setOperator(operator);
        logRow.setRemark(remark);
        logMapper.insert(logRow);
    }

    /** 订单 → 订单视图 */
    public OrderVO toVO(Orders order, boolean withDetail) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setShopId(order.getShopId());
        vo.setShopName(order.getShopName());
        vo.setShopHeaderImg(resolveMedia(order.getShopHeaderImg()));
        vo.setFulfillType(order.getFulfillType());
        vo.setFulfillTypeLabel(FulfillType.of(order.getFulfillType()).getLabel());
        vo.setStatus(order.getStatus());
        vo.setStatusLabel(OrderStatus.labelOf(order.getStatus()));
        OrderStatus st = OrderStatus.of(order.getStatus());
        vo.setStatusGroup(st == null ? null : st.getGroup());
        vo.setItemAmount(order.getItemAmount());
        vo.setPackFee(order.getPackFee());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setPickupCode(order.getPickupCode());
        vo.setRemark(order.getRemark());
        vo.setPromiseMinutes(order.getPromiseMinutes());
        vo.setCancelReason(order.getCancelReason());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setPaidAt(order.getPaidAt());
        vo.setFinishedAt(order.getFinishedAt());

        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()));
        vo.setItems(items.stream().map(i -> {
            OrderVO.Item item = new OrderVO.Item();
            item.setDishId(i.getDishId());
            item.setDishName(i.getDishName());
            item.setDishImage(resolveMedia(i.getDishImage()));
            item.setSpecText(i.getSpecText());
            item.setUnitPrice(i.getUnitPrice());
            item.setQuantity(i.getQuantity());
            item.setAmount(i.getAmount());
            vo.setTotalQuantity((vo.getTotalQuantity() == null ? 0 : vo.getTotalQuantity()) + i.getQuantity());
            return item;
        }).collect(Collectors.toList()));

        LocalDateTime base = order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt();
        if (base != null && order.getPromiseMinutes() != null) {
            String text = base.plusMinutes(order.getPromiseMinutes()).format(
                    DateTimeFormatter.ofPattern("HH:mm"));
            vo.setPromiseText(FulfillType.PICKUP.name().equals(order.getFulfillType())
                    ? "预计 " + text + " 备餐完成" : "预计 " + text + " 送达");
        }

        // 用户侧可执行操作
        vo.setActions(OrderStateMachine.actionsOf(FulfillType.of(order.getFulfillType()),
                order.getStatus(), true));
        vo.setCanUrge(vo.getActions().contains("URGE"));

        if (withDetail) {
            // 详情页带上门店经纬度（配送地图打点）；列表不查，避免 N+1
            Shop shop = shopMapper.selectById(order.getShopId());
            if (shop != null) {
                vo.setShopLat(shop.getLat());
                vo.setShopLng(shop.getLng());
            }
            // 收货地址经纬度：首次查看详情时按需地理编码并回写订单（失败不阻塞）
            if (order.getReceiverLat() == null && order.getReceiverLng() == null
                    && order.getReceiverAddress() != null && !order.getReceiverAddress().isBlank()) {
                double[] coord = geocode.geocode(order.getReceiverAddress());
                if (coord != null) {
                    order.setReceiverLng(BigDecimal.valueOf(coord[0]));
                    order.setReceiverLat(BigDecimal.valueOf(coord[1]));
                    ordersMapper.updateById(order);
                }
            }
            vo.setReceiverLat(order.getReceiverLat());
            vo.setReceiverLng(order.getReceiverLng());
            // 详情页返回金额明细（服务端口径），便于前端渲染「金额构成」
            AmountVO amount = new AmountVO();
            amount.setItemAmount(order.getItemAmount());
            amount.setPackFee(order.getPackFee());
            amount.setDeliveryFee(order.getDeliveryFee());
            amount.setDeliveryFeeOrigin(order.getDeliveryFee());
            amount.setDiscountAmount(order.getDiscountAmount());
            amount.setFullReduceAmount(order.getDiscountAmount());
            amount.setPickupDiscountAmount(0);
            amount.setPayAmount(order.getPayAmount());
            amount.setTotalQuantity(vo.getTotalQuantity());
            vo.setAmount(amount);
            List<OrderStatusLog> logs = logMapper.selectList(Wrappers.<OrderStatusLog>lambdaQuery()
                    .eq(OrderStatusLog::getOrderId, order.getId())
                    .orderByAsc(OrderStatusLog::getCreatedAt).orderByAsc(OrderStatusLog::getId));
            vo.setTimeline(logs.stream().map(l -> {
                OrderVO.Log item = new OrderVO.Log();
                item.setFromStatus(l.getFromStatus());
                item.setFromLabel(l.getFromStatus() == null ? null : OrderStatus.labelOf(l.getFromStatus()));
                item.setToStatus(l.getToStatus());
                item.setToLabel(OrderStatus.labelOf(l.getToStatus()));
                item.setOperator(l.getOperator());
                item.setOperatorLabel(operatorLabel(l.getOperator()));
                item.setRemark(l.getRemark());
                item.setCreatedAt(l.getCreatedAt());
                return item;
            }).collect(Collectors.toList()));
        }
        return vo;
    }

    private String operatorLabel(String operator) {
        if (operator == null) {
            return "";
        }
        return switch (operator) {
            case "USER" -> "用户";
            case "MERCHANT" -> "商家";
            case "RIDER" -> "骑手";
            case "ADMIN" -> "平台";
            case "SYSTEM" -> "系统";
            default -> operator;
        };
    }
}

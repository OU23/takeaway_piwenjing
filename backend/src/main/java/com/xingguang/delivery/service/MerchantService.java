package com.xingguang.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.common.OrderStatus;
import com.xingguang.delivery.common.PageResult;
import com.xingguang.delivery.entity.*;
import com.xingguang.delivery.mapper.*;
import com.xingguang.delivery.support.CurrentMerchant;
import com.xingguang.delivery.support.TokenCodec;
import com.xingguang.delivery.support.VoConverter;
import com.xingguang.delivery.vo.FileVO;
import com.xingguang.delivery.vo.OrderVO;
import com.xingguang.delivery.vo.ShopVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商家端服务（v1）：商家独立账号登录、我的门店、菜品上/下架与改价、接单改状态、数据看板。
 *
 * <p>所有写操作均按 {@link CurrentMerchant#shopId()} 做水平隔离，商家只能操作自己门店的数据。
 * 订单状态流转复用 {@link OrderService#transferStatus} + {@code OrderStateMachine}，杜绝跳状态。
 */
@Service
public class MerchantService {

    private static final Map<String, List<String>> GROUP_STATUS = new LinkedHashMap<>();

    static {
        GROUP_STATUS.put("待付款", List.of("UNPAID"));
        GROUP_STATUS.put("待接单", List.of("WAIT_ACCEPT"));
        GROUP_STATUS.put("进行中", List.of("PREPARING", "WAIT_PICKUP", "DELIVERING",
                "DELIVERED", "WAIT_MEAL", "PICKED", "EXCEPTION"));
        GROUP_STATUS.put("待评价", List.of("COMPLETED"));
        GROUP_STATUS.put("退款售后", List.of("CANCELED", "REFUNDED", "PART_REFUNDED"));
    }

    private final ShopMapper shopMapper;
    private final DishMapper dishMapper;
    private final OrdersMapper ordersMapper;
    private final MerchantUserMapper merchantUserMapper;
    private final VoConverter converter;
    private final OrderService orderService;
    private final CurrentMerchant currentMerchant;
    private final ObjectStorageService storage;
    private final MediaService media;

    @Value("${xingguang.auth.secret:xg-dev-secret-change-me-in-production}")
    private String secret;

    @Value("${xingguang.auth.token-ttl-hours:168}")
    private long tokenTtlHours;

    public MerchantService(ShopMapper shopMapper, DishMapper dishMapper, OrdersMapper ordersMapper,
                          MerchantUserMapper merchantUserMapper, VoConverter converter,
                          OrderService orderService, CurrentMerchant currentMerchant,
                          ObjectStorageService storage, MediaService media) {
        this.shopMapper = shopMapper;
        this.dishMapper = dishMapper;
        this.ordersMapper = ordersMapper;
        this.merchantUserMapper = merchantUserMapper;
        this.converter = converter;
        this.orderService = orderService;
        this.currentMerchant = currentMerchant;
        this.storage = storage;
        this.media = media;
    }

    // ==================================================================
    // 登录
    // ==================================================================

    public Map<String, Object> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw BizException.of("账号或密码不能为空");
        }
        MerchantUser u = merchantUserMapper.selectOne(Wrappers.<MerchantUser>lambdaQuery()
                .eq(MerchantUser::getUsername, username.trim()));
        if (u == null || (u.getDeleted() != null && u.getDeleted() == 1)) {
            throw BizException.of("账号不存在");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw BizException.of("账号已停用");
        }
        if (!md5Hex(password).equalsIgnoreCase(u.getPassword())) {
            throw BizException.of("密码错误");
        }
        u.setLastLogin(LocalDateTime.now());
        merchantUserMapper.updateById(u);

        Map<String, Object> data = new LinkedHashMap<>();
        long exp = System.currentTimeMillis() / 1000L + tokenTtlHours * 3600L;
        data.put("token", TokenCodec.issue(u.getId(), TokenCodec.AUD_MERCHANT, exp, secret));
        data.put("expiresAt", exp);
        data.put("name", u.getName());
        data.put("shopId", u.getShopId());
        data.put("role", "MERCHANT");
        data.put("roleLabel", "商家");
        return data;
    }

    // ==================================================================
    // 我的门店
    // ==================================================================

    public ShopVO myShop() {
        Shop shop = requireMyShop();
        return converter.toShopVO(shop);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShopVO updateShop(Map<String, Object> patch) {
        Shop shop = requireMyShop();
        if (patch.containsKey("name")) {
            shop.setName(String.valueOf(patch.get("name")));
        }
        if (patch.containsKey("notice")) {
            shop.setNotice(String.valueOf(patch.get("notice")));
        }
        if (patch.containsKey("openTime")) {
            shop.setOpenTime(String.valueOf(patch.get("openTime")));
        }
        if (patch.containsKey("closeTime")) {
            shop.setCloseTime(String.valueOf(patch.get("closeTime")));
        }
        if (patch.containsKey("minPrice")) {
            shop.setMinPrice(intOf(patch.get("minPrice")));
        }
        if (patch.containsKey("deliveryFee")) {
            shop.setDeliveryFee(intOf(patch.get("deliveryFee")));
        }
        if (patch.containsKey("packFee")) {
            shop.setPackFee(intOf(patch.get("packFee")));
        }
        if (patch.containsKey("pickupEnabled")) {
            shop.setPickupEnabled(intOf(patch.get("pickupEnabled")));
        }
        if (patch.containsKey("pickupDiscount")) {
            shop.setPickupDiscount(intOf(patch.get("pickupDiscount")));
        }
        if (patch.containsKey("status")) {
            String s = String.valueOf(patch.get("status"));
            if (!List.of("OPEN", "CLOSED", "REST").contains(s)) {
                throw BizException.shop("非法的营业状态：" + s);
            }
            shop.setStatus(s);
        }
        shopMapper.updateById(shop);
        return converter.toShopVO(shop);
    }

    // ==================================================================
    // 菜品
    // ==================================================================

    public List<Dish> myDishes() {
        return dishMapper.selectList(Wrappers.<Dish>lambdaQuery()
                .eq(Dish::getShopId, currentMerchant.shopId())
                .orderByAsc(Dish::getCategoryId).orderByAsc(Dish::getSort));
    }

    @Transactional(rollbackFor = Exception.class)
    public Dish toggleDishStatus(Long dishId) {
        Dish dish = requireMyDish(dishId);
        String next = "ON_SALE".equals(dish.getStatus()) ? "OFF_SHELF" : "ON_SALE";
        dish.setStatus(next);
        dishMapper.updateById(dish);
        return dish;
    }

    @Transactional(rollbackFor = Exception.class)
    public Dish updateDish(Long dishId, Integer price, Integer originPrice) {
        Dish dish = requireMyDish(dishId);
        if (price != null) {
            if (price < 0) {
                throw BizException.product("价格不能为负");
            }
            dish.setPrice(price);
        }
        if (originPrice != null) {
            dish.setOriginPrice(originPrice);
        }
        dishMapper.updateById(dish);
        return dish;
    }

    // ==================================================================
    // 订单
    // ==================================================================

    public PageResult<OrderVO> orders(String group, String status, Long pageNum, Long pageSize) {
        long pn = PageResult.normalizePageNum(pageNum);
        long ps = PageResult.normalizePageSize(pageSize);

        LambdaQueryWrapper<Orders> q = Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getShopId, currentMerchant.shopId())
                .orderByDesc(Orders::getCreatedAt);
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            q.eq(Orders::getStatus, status);
        } else if (group != null && !group.isBlank() && !"全部".equals(group) && !"ALL".equals(group)) {
            List<String> statuses = GROUP_STATUS.get(group);
            if (statuses == null) {
                throw new BizException(10001, "未知的订单分组：" + group);
            }
            q.in(Orders::getStatus, statuses);
        }

        IPage<Orders> page = ordersMapper.selectPage(new Page<>(pn, ps), q);
        List<OrderVO> list = page.getRecords().stream()
                .map(o -> orderService.toVO(o, false))
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    public OrderVO orderDetail(String orderNo) {
        Orders order = requireMyOrder(orderNo);
        return orderService.toVO(order, true);
    }

    /** 商家接单：待接单 → 备餐中 */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO acceptOrder(String orderNo) {
        Orders order = requireMyOrder(orderNo);
        orderService.transferStatus(order, OrderStatus.PREPARING, "MERCHANT", "商家接单");
        return orderService.toVO(order, false);
    }

    /** 商家出餐：备餐中 → 待取货(配送) / 待取餐(自取) */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO prepareOrder(String orderNo) {
        Orders order = requireMyOrder(orderNo);
        boolean delivery = "DELIVERY".equals(order.getFulfillType());
        OrderStatus to = delivery ? OrderStatus.WAIT_PICKUP : OrderStatus.WAIT_MEAL;
        OrderStatus from = OrderStatus.of(order.getStatus());
        if (from != OrderStatus.PREPARING) {
            throw BizException.order("仅「备餐中」的订单可以出餐");
        }
        orderService.transferStatus(order, to, "MERCHANT", delivery ? "商家出餐完成，等待骑手取货" : "商家出餐完成，取餐码已下发");
        if (to == OrderStatus.WAIT_MEAL && order.getPickupCode() == null) {
            order.setPickupCode(genPickupCode());
            ordersMapper.updateById(order);
        }
        return orderService.toVO(order, false);
    }

    /** 商家完成：配送单 待取货 → 配送中(交付骑手)；自取单 待取餐 → 已取餐(交付顾客) */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO completeOrder(String orderNo) {
        Orders order = requireMyOrder(orderNo);
        boolean delivery = "DELIVERY".equals(order.getFulfillType());
        OrderStatus from = OrderStatus.of(order.getStatus());
        OrderStatus to = delivery ? OrderStatus.WAIT_PICKUP : OrderStatus.WAIT_MEAL;
        if (from != to) {
            throw BizException.order("当前状态「" + OrderStatus.labelOf(order.getStatus())
                    + "」还不能标记完成");
        }
        OrderStatus next = delivery ? OrderStatus.DELIVERING : OrderStatus.PICKED;
        orderService.transferStatus(order, next, "MERCHANT",
                delivery ? "商家交付骑手，开始配送" : "商家交付顾客（已取餐）");
        return orderService.toVO(order, false);
    }

    // ==================================================================
    // 数据看板
    // ==================================================================

    public Map<String, Object> dashboard() {
        Long shopId = currentMerchant.shopId();
        List<Orders> all = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getShopId, shopId));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long todayOrders = all.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(todayStart)).count();
        long todayGmv = all.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(todayStart))
                .filter(o -> !OrderStatus.UNPAID.name().equals(o.getStatus())
                        && !OrderStatus.CANCELED.name().equals(o.getStatus()))
                .mapToLong(o -> o.getPayAmount() == null ? 0 : o.getPayAmount()).sum();
        long processing = all.stream().filter(o -> {
            OrderStatus s = OrderStatus.of(o.getStatus());
            return s != null && "进行中".equals(s.getGroup());
        }).count();

        Map<String, Long> statusDist = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            long count = all.stream().filter(o -> s.name().equals(o.getStatus())).count();
            if (count > 0) {
                statusDist.put(s.getLabel(), count);
            }
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("todayOrderCount", todayOrders);
        map.put("todayGmv", todayGmv);
        map.put("processingOrderCount", processing);
        map.put("totalOrderCount", all.size());
        map.put("shopName", shopMapper.selectById(shopId) == null ? null
                : shopMapper.selectById(shopId).getName());
        map.put("statusDistribution", statusDist);
        return map;
    }

    // ==================================================================
    // 对象存储（商家端真实图片上传）
    // ==================================================================

    /** 上传图片到 MinIO，返回 objectKey 与可访问 url（公开直链或临时签名 URL）。 */
    public FileVO upload(MultipartFile file, String biz) {
        return storage.upload(file, biz);
    }

    /** 绑定门店 Logo：仅可绑定当前商家自己的门店。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> bindShopLogo(String objectKey) {
        media.bindShopLogo(currentMerchant.shopId(), objectKey);
        return withObjectUrl(objectKey);
    }

    /** 绑定菜品图片：仅可绑定当前商家门店下的菜品（越权由 requireMyDish 拦截）。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> bindDishImage(Long dishId, String objectKey) {
        requireMyDish(dishId);
        media.bindDishImage(dishId, objectKey);
        return withObjectUrl(objectKey);
    }

    /** 绑定门店头图：仅可绑定当前商家自己的门店。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> bindShopHeaderImg(String objectKey) {
        media.bindShopHeaderImg(currentMerchant.shopId(), objectKey);
        return withObjectUrl(objectKey);
    }

    /** 把 objectKey 解析成可访问 url，供前端回显（不校验归属，仅拼地址）。 */
    public Map<String, Object> resolveUrl(String objectKey) {
        return withObjectUrl(objectKey);
    }

    private Map<String, Object> withObjectUrl(String objectKey) {
        FileVO vo = new FileVO();
        vo.setObjectKey(objectKey);
        storage.applyUrl(vo);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("objectKey", objectKey);
        m.put("url", vo.getUrl());
        return m;
    }

    // ==================================================================
    // 内部工具
    // ==================================================================

    private Shop requireMyShop() {
        Shop shop = shopMapper.selectById(currentMerchant.shopId());
        if (shop == null) {
            throw BizException.shop("门店不存在");
        }
        return shop;
    }

    private Dish requireMyDish(Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw BizException.product("菜品不存在：" + dishId);
        }
        if (!Objects.equals(dish.getShopId(), currentMerchant.shopId())) {
            throw new BizException(10003, "无权操作其他门店的菜品");
        }
        return dish;
    }

    private Orders requireMyOrder(String orderNo) {
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery().eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw BizException.order("订单不存在：" + orderNo);
        }
        if (!Objects.equals(order.getShopId(), currentMerchant.shopId())) {
            throw new BizException(10003, "无权操作其他门店的订单");
        }
        return order;
    }

    private static int intOf(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String genPickupCode() {
        return "SG-" + String.format("%04d", new java.security.SecureRandom().nextInt(10000));
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
}

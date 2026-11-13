package com.xingguang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.domain.LineItem;
import com.xingguang.delivery.dto.AddCartRequest;
import com.xingguang.delivery.entity.CartItem;
import com.xingguang.delivery.entity.Dish;
import com.xingguang.delivery.entity.DishSpec;
import com.xingguang.delivery.entity.Shop;
import com.xingguang.delivery.mapper.CartItemMapper;
import com.xingguang.delivery.mapper.DishMapper;
import com.xingguang.delivery.mapper.DishSpecMapper;
import com.xingguang.delivery.mapper.ShopMapper;
import com.xingguang.delivery.vo.CartVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** 购物车服务。单价在下单前为「快照」，提交订单时会以服务端最新价重新计算。 */
@Service
public class CartService {

    private final CartItemMapper cartMapper;
    private final DishMapper dishMapper;
    private final DishSpecMapper specMapper;
    private final ShopMapper shopMapper;

    public CartService(CartItemMapper cartMapper, DishMapper dishMapper,
                       DishSpecMapper specMapper, ShopMapper shopMapper) {
        this.cartMapper = cartMapper;
        this.dishMapper = dishMapper;
        this.specMapper = specMapper;
        this.shopMapper = shopMapper;
    }

    /** 查询某门店的购物车 */
    public CartVO getCart(long userId, Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw BizException.shop("门店不存在");
        }
        CartVO vo = new CartVO();
        vo.setShopId(shop.getId());
        vo.setShopName(shop.getName());
        vo.setShopHeaderImg(shop.getHeaderImg());
        vo.setMinPrice(shop.getMinPrice());

        List<CartItem> items = listItems(userId, shopId, null);
        int itemAmount = 0;
        int qty = 0;
        List<CartVO.Line> lines = new ArrayList<>();
        for (CartItem c : items) {
            CartVO.Line line = new CartVO.Line();
            line.setId(c.getId());
            line.setDishId(c.getDishId());
            line.setSpecText(c.getSpecText());
            line.setUnitPrice(c.getUnitPrice());
            line.setQuantity(c.getQuantity());
            line.setAmount(c.getUnitPrice() * c.getQuantity());
            Dish dish = dishMapper.selectById(c.getDishId());
            if (dish != null) {
                line.setDishName(dish.getName());
                line.setDishImage(dish.getImage());
            }
            lines.add(line);
            itemAmount += line.getAmount();
            qty += c.getQuantity();
        }
        vo.setLines(lines);
        vo.setItemAmount(itemAmount);
        vo.setTotalQuantity(qty);
        return vo;
    }

    /** 加入购物车（同商品 + 同规格自动合并） */
    @Transactional(rollbackFor = Exception.class)
    public CartVO addItem(long userId, AddCartRequest req) {
        Dish dish = dishMapper.selectById(req.getDishId());
        if (dish == null) {
            throw BizException.product("商品不存在");
        }
        if (dish.getSoldOut() != null && dish.getSoldOut() == 1) {
            throw BizException.product("「" + dish.getName() + "」已估清，请选择其他商品");
        }
        Shop shop = shopMapper.selectById(dish.getShopId());
        if (shop == null || !"OPEN".equals(shop.getStatus())) {
            throw BizException.shop("门店已打烊，暂时无法下单");
        }

        List<DishSpec> specs = resolveSpecs(dish.getId(), req.getSpecIds());
        int unitPrice = dish.getPrice() + specs.stream()
                .mapToInt(s -> s.getPriceDelta() == null ? 0 : s.getPriceDelta()).sum();
        String specText = specs.isEmpty() ? null : specs.stream()
                .map(DishSpec::getName).collect(Collectors.joining(" / "));
        String specIds = specs.isEmpty() ? null : specs.stream()
                .map(s -> String.valueOf(s.getId())).collect(Collectors.joining(","));

        int stock = dish.getStock() == null ? 999 : dish.getStock();

        // 合并同款同规格
        var sameQuery = Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDishId, dish.getId());
        if (specIds == null) {
            sameQuery.isNull(CartItem::getSpecIds);
        } else {
            sameQuery.eq(CartItem::getSpecIds, specIds);
        }
        List<CartItem> same = cartMapper.selectList(sameQuery);
        if (!same.isEmpty()) {
            CartItem exist = same.get(0);
            int newQty = exist.getQuantity() + req.getQuantity();
            if (newQty > stock) {
                throw BizException.product("库存不足，最多可购 " + stock + " 份");
            }
            exist.setQuantity(newQty);
            cartMapper.updateById(exist);
            return getCart(userId, dish.getShopId());
        }

        if (req.getQuantity() > stock) {
            throw BizException.product("库存不足，最多可购 " + stock + " 份");
        }

        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setShopId(dish.getShopId());
        item.setDishId(dish.getId());
        item.setSpecIds(specIds);
        item.setSpecText(specText);
        item.setUnitPrice(unitPrice);
        item.setQuantity(req.getQuantity());
        cartMapper.insert(item);
        return getCart(userId, dish.getShopId());
    }

    /** 修改数量；quantity=0 视为删除 */
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(long userId, Long itemId, int quantity) {
        CartItem item = requireOwned(userId, itemId);
        if (quantity <= 0) {
            cartMapper.deleteById(itemId);
            return;
        }
        Dish dish = dishMapper.selectById(item.getDishId());
        int stock = (dish == null || dish.getStock() == null) ? 999 : dish.getStock();
        if (quantity > stock) {
            throw BizException.product("库存不足，最多可购 " + stock + " 份");
        }
        item.setQuantity(quantity);
        cartMapper.updateById(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(long userId, Long itemId) {
        requireOwned(userId, itemId);
        cartMapper.deleteById(itemId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void clear(long userId, Long shopId) {
        cartMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getShopId, shopId));
    }

    /** 购物车总件数（底部 Tab 角标） */
    public int count(long userId) {
        List<CartItem> items = cartMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId));
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    /** 取结算行项目（供下单 / 试算） */
    public List<CartItem> listItems(long userId, Long shopId, List<Long> itemIds) {
        var q = Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getShopId, shopId)
                .orderByAsc(CartItem::getId);
        if (itemIds != null && !itemIds.isEmpty()) {
            q.in(CartItem::getId, itemIds);
        }
        return cartMapper.selectList(q);
    }

    /** 购物车条目 → 结算行项目，并按服务端最新价重算（防前端改价） */
    public List<LineItem> toLineItems(List<CartItem> cartItems) {
        List<LineItem> lines = new ArrayList<>();
        for (CartItem c : cartItems) {
            Dish dish = dishMapper.selectById(c.getDishId());
            if (dish == null) {
                throw BizException.product("购物车中存在已下架商品，请重新选择");
            }
            if (dish.getSoldOut() != null && dish.getSoldOut() == 1) {
                throw BizException.product("「" + dish.getName() + "」已估清，请先移除");
            }
            // 按最新规格加价重算单价，避免商品调价后仍按旧价下单
            List<DishSpec> specs = resolveSpecs(dish.getId(), parseSpecIds(c.getSpecIds()));
            int unitPrice = dish.getPrice() + specs.stream()
                    .mapToInt(s -> s.getPriceDelta() == null ? 0 : s.getPriceDelta()).sum();
            lines.add(new LineItem(dish.getId(), dish.getName(), dish.getImage(),
                    c.getSpecText(), unitPrice, c.getQuantity()));
        }
        return lines;
    }

    private List<Long> parseSpecIds(String specIds) {
        if (specIds == null || specIds.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String s : specIds.split(",")) {
            if (!s.isBlank()) {
                ids.add(Long.parseLong(s.trim()));
            }
        }
        return ids;
    }

    /** 校验规格归属，并按「组内顺序 + 加价」返回（保证规格文本稳定） */
    private List<DishSpec> resolveSpecs(Long dishId, List<Long> specIds) {
        if (specIds == null || specIds.isEmpty()) {
            return List.of();
        }
        List<DishSpec> specs = specMapper.selectList(Wrappers.<DishSpec>lambdaQuery()
                .eq(DishSpec::getDishId, dishId)
                .in(DishSpec::getId, specIds));
        if (specs.size() != specIds.size()) {
            throw BizException.product("存在无效的规格选项，请重新选择");
        }
        // 按 id 排序得到稳定的规格文本（与商品配置顺序一致）
        specs.sort(Comparator.comparing(DishSpec::getId));
        return specs;
    }

    private CartItem requireOwned(long userId, Long itemId) {
        CartItem item = cartMapper.selectById(itemId);
        if (item == null) {
            throw BizException.of("购物车条目不存在");
        }
        if (!Objects.equals(item.getUserId(), userId)) {
            // 水平越权防护（SRS 9.3）
            throw new BizException(10003, "无权操作他人的购物车");
        }
        return item;
    }
}

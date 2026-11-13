package com.xingguang.delivery.service;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.entity.AppUser;
import com.xingguang.delivery.entity.Dish;
import com.xingguang.delivery.entity.Shop;
import com.xingguang.delivery.mapper.AppUserMapper;
import com.xingguang.delivery.mapper.DishMapper;
import com.xingguang.delivery.mapper.ShopMapper;
import org.springframework.stereotype.Service;

/**
 * 把已上传的对象绑定到业务实体：商品图 / 门店 Logo / 用户头像。
 *
 * <p>库中只存 objectKey（可迁移、可换 CDN），展示地址由 {@link ObjectStorageService} 生成。
 */
@Service
public class MediaService {

    private final DishMapper dishMapper;
    private final ShopMapper shopMapper;
    private final AppUserMapper appUserMapper;

    public MediaService(DishMapper dishMapper, ShopMapper shopMapper, AppUserMapper appUserMapper) {
        this.dishMapper = dishMapper;
        this.shopMapper = shopMapper;
        this.appUserMapper = appUserMapper;
    }

    public Dish bindDishImage(Long dishId, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BizException(20006, "缺少对象键 objectKey");
        }
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new BizException(40004, "商品不存在");
        }
        dish.setImage(objectKey);
        dishMapper.updateById(dish);
        return dish;
    }

    public Shop bindShopLogo(Long shopId, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BizException(20006, "缺少对象键 objectKey");
        }
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BizException(50001, "门店不存在");
        }
        shop.setLogo(objectKey);
        shopMapper.updateById(shop);
        return shop;
    }

    public AppUser bindUserAvatar(Long userId, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BizException(20006, "缺少对象键 objectKey");
        }
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(10002, "用户不存在");
        }
        user.setAvatar(objectKey);
        appUserMapper.updateById(user);
        return user;
    }

    /** 绑定门店头图（headerImg）。 */
    public Shop bindShopHeaderImg(Long shopId, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BizException(20006, "缺少对象键 objectKey");
        }
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BizException(50001, "门店不存在");
        }
        shop.setHeaderImg(objectKey);
        shopMapper.updateById(shop);
        return shop;
    }
}

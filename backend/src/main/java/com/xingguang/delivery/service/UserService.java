package com.xingguang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.common.Mask;
import com.xingguang.delivery.entity.AppUser;
import com.xingguang.delivery.entity.UserAddress;
import com.xingguang.delivery.mapper.AppUserMapper;
import com.xingguang.delivery.mapper.UserAddressMapper;
import com.xingguang.delivery.service.ObjectStorageService;
import com.xingguang.delivery.service.MediaService;
import com.xingguang.delivery.vo.AddressVO;
import com.xingguang.delivery.vo.FileVO;
import com.xingguang.delivery.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 用户中心：个人资料、收货地址。手机号一律脱敏输出（SRS 9.11.6）。 */
@Service
public class UserService {

    /** 会员六档等级（SRS 5.2.11），门槛为成长值 */
    private static final String[][] LEVELS = {
            {"NORMAL", "普通", "0"},
            {"SILVER", "白银", "1000"},
            {"GOLD", "黄金", "2500"},
            {"PLATINUM", "铂金", "5000"},
            {"BLACKGOLD", "黑金", "10000"},
            {"BLACKDIAMOND", "黑钻", "30000"},
    };

    private final AppUserMapper userMapper;
    private final UserAddressMapper addressMapper;
    private final ObjectStorageService storage;
    private final MediaService media;

    public UserService(AppUserMapper userMapper, UserAddressMapper addressMapper,
                       ObjectStorageService storage, MediaService media) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.storage = storage;
        this.media = media;
    }

    public UserVO profile(long userId) {
        AppUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.of("用户不存在");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhoneMasked(Mask.phone(user.getPhone()));
        vo.setNickname(user.getNickname());
        vo.setAvatar(resolveMedia(user.getAvatar()));
        vo.setBalance(user.getBalance());
        vo.setMemberLevel(user.getMemberLevel());
        vo.setMemberLevelLabel(levelLabel(user.getMemberLevel()));
        vo.setGrowth(user.getGrowth());

        int growth = user.getGrowth() == null ? 0 : user.getGrowth();
        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i][0].equals(user.getMemberLevel()) && i < LEVELS.length - 1) {
                int next = Integer.parseInt(LEVELS[i + 1][2]);
                vo.setNextLevelLabel(LEVELS[i + 1][1]);
                vo.setGrowthToNext(Math.max(0, next - growth));
                break;
            }
        }
        if (vo.getNextLevelLabel() == null) {
            vo.setNextLevelLabel("已是最高等级");
            vo.setGrowthToNext(0);
        }
        return vo;
    }

    public List<AddressVO> addresses(long userId) {
        List<UserAddress> list = addressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getDeleted, 0)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByAsc(UserAddress::getId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public AddressVO addAddress(long userId, String contact, String phone, String detail,
                                String tag, boolean asDefault) {
        if (contact == null || contact.isBlank() || phone == null || phone.isBlank()
                || detail == null || detail.isBlank()) {
            throw new BizException(10001, "收货人、手机号与详细地址均不能为空");
        }
        if (asDefault) {
            clearDefault(userId);
        }
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setContact(contact);
        address.setPhone(phone);
        address.setDetail(detail);
        address.setTag(tag);
        address.setIsDefault(asDefault ? 1 : 0);
        addressMapper.insert(address);
        return toVO(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(long userId, Long addressId) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || !Objects.equals(address.getUserId(), userId)) {
            throw new BizException(10003, "地址不存在或无权操作");
        }
        clearDefault(userId);
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public AddressVO updateAddress(long userId, Long addressId, String contact, String phone,
                                   String detail, String tag, boolean asDefault) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || address.getDeleted() == 1
                || !Objects.equals(address.getUserId(), userId)) {
            throw new BizException(10003, "地址不存在或无权操作");
        }
        if (contact != null) address.setContact(contact);
        if (phone != null) address.setPhone(phone);
        if (detail != null) address.setDetail(detail);
        if (tag != null) address.setTag(tag);
        if (asDefault) {
            clearDefault(userId);
            address.setIsDefault(1);
        }
        addressMapper.updateById(address);
        return toVO(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeAddress(long userId, Long addressId) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || address.getDeleted() == 1
                || !Objects.equals(address.getUserId(), userId)) {
            throw new BizException(10003, "地址不存在或无权操作");
        }
        // 逻辑删除字段由 MyBatis-Plus 全局配置托管，updateById 会忽略 deleted，
        // 必须用 deleteById 触发软删除（自动 SET deleted=1）。
        addressMapper.deleteById(addressId);
    }

    private void clearDefault(long userId) {
        List<UserAddress> list = addressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1));
        for (UserAddress a : list) {
            a.setIsDefault(0);
            addressMapper.updateById(a);
        }
    }

    private AddressVO toVO(UserAddress a) {
        AddressVO vo = new AddressVO();
        vo.setId(a.getId());
        vo.setContact(a.getContact());
        vo.setPhoneMasked(Mask.phone(a.getPhone()));
        vo.setDetail(a.getDetail());
        vo.setTag(a.getTag());
        vo.setIsDefault(a.getIsDefault() != null && a.getIsDefault() == 1);
        return vo;
    }

    public static String levelLabel(String code) {
        if (code == null) {
            return "普通";
        }
        for (String[] l : LEVELS) {
            if (l[0].equals(code)) {
                return l[1];
            }
        }
        return code;
    }

    // ==================================================================
    // 对象存储（C 端用户头像上传）
    // ==================================================================

    /** 上传图片到 MinIO，返回 objectKey 与可访问 url。biz: avatar 头像 / review 评价晒图。 */
    public FileVO upload(MultipartFile file, String biz) {
        if (!List.of("avatar", "review").contains(biz)) {
            throw new BizException(20006, "不支持的上传业务类型：" + biz + "（仅 avatar / review）");
        }
        return storage.upload(file, biz);
    }

    /** 绑定头像：仅可绑定当前登录用户自己。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> bindAvatar(long userId, String objectKey) {
        media.bindUserAvatar(userId, objectKey);
        FileVO vo = new FileVO();
        vo.setObjectKey(objectKey);
        storage.applyUrl(vo);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("objectKey", objectKey);
        m.put("url", vo.getUrl());
        return m;
    }

    /** 同 VoConverter.resolveMedia：真实 objectKey 解析为可访问 url，渐变占位/空值原样返回。 */
    private String resolveMedia(String key) {
        if (key == null || key.isBlank() || !key.contains("/")) {
            return key;
        }
        FileVO vo = new FileVO();
        vo.setObjectKey(key);
        storage.applyUrl(vo);
        return vo.getUrl();
    }
}

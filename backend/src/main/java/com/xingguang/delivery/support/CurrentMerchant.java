package com.xingguang.delivery.support;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.entity.MerchantUser;
import com.xingguang.delivery.mapper.MerchantUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 当前登录商家解析（商家端）。
 *
 * <p>商家登录接口签发受众为 MERCHANT 的签名令牌，请求放入 {@code X-Merchant-Token} 头；
 * 服务端验签 + 查库校验状态。与其它端令牌受众隔离，未携带合法令牌直接 401。
 */
@Component
public class CurrentMerchant {

    private static final String HEADER = "X-Merchant-Token";

    private final HttpServletRequest request;
    private final MerchantUserMapper merchantUserMapper;

    @org.springframework.beans.factory.annotation.Value("${xingguang.auth.secret:xg-dev-secret-change-me-in-production}")
    private String secret;

    public CurrentMerchant(HttpServletRequest request, MerchantUserMapper merchantUserMapper) {
        this.request = request;
        this.merchantUserMapper = merchantUserMapper;
    }

    public MerchantUser user() {
        String v = request.getHeader(HEADER);
        if (v == null || v.isBlank()) {
            throw new BizException(401, "请先登录商家端");
        }
        Long id = TokenCodec.verify(v.trim(), secret, TokenCodec.AUD_MERCHANT);
        if (id < 0) {
            throw new BizException(401, "登录状态已失效，请重新登录");
        }
        MerchantUser u = merchantUserMapper.selectById(id);
        if (u == null || (u.getDeleted() != null && u.getDeleted() == 1)
                || (u.getStatus() != null && u.getStatus() == 0)) {
            throw new BizException(401, "账号不可用");
        }
        return u;
    }

    public Long id() {
        return user().getId();
    }

    /** 当前商家管理的门店 id（所有写操作按此做水平隔离）。 */
    public Long shopId() {
        return user().getShopId();
    }

    public Long merchantId() {
        return user().getMerchantId();
    }
}

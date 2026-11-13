package com.xingguang.delivery.support;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.entity.Rider;
import com.xingguang.delivery.mapper.RiderMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 当前登录骑手解析（骑手端）。
 *
 * <p>骑手登录接口签发受众为 RIDER 的签名令牌，请求放入 {@code X-Rider-Token} 头。
 * 与用户/后台令牌受众隔离，互不通用（S2 修复）。
 *
 * <p><b>安全约束</b>：{@code X-Rider-Id} 直传骑手 id 的通道已移除；
 * 仅当 {@code xingguang.auth.demo-identity-enabled=true}（本地开发/e2e）时，
 * 未携带令牌才回落到演示骑手 1。
 */
@Component
public class CurrentRider {

    /** 演示骑手 id（与 data.sql 中的 rider 一致） */
    public static final long DEMO_RIDER_ID = 1L;

    private static final String HEADER_TOKEN = "X-Rider-Token";

    private final HttpServletRequest request;
    private final RiderMapper riderMapper;

    @org.springframework.beans.factory.annotation.Value("${xingguang.auth.secret:xg-dev-secret-change-me-in-production}")
    private String secret;

    @org.springframework.beans.factory.annotation.Value("${xingguang.auth.demo-identity-enabled:true}")
    private boolean demoIdentityEnabled;

    public CurrentRider(HttpServletRequest request, RiderMapper riderMapper) {
        this.request = request;
        this.riderMapper = riderMapper;
    }

    public long riderId() {
        String v = request.getHeader(HEADER_TOKEN);
        if (v != null && !v.isBlank()) {
            long id = TokenCodec.verify(v.trim(), secret, TokenCodec.AUD_RIDER);
            if (id < 0) {
                throw new BizException(401, "骑手登录状态已失效，请重新登录");
            }
            Rider r = riderMapper.selectById(id);
            if (r == null || (r.getDeleted() != null && r.getDeleted() == 1)) {
                throw new BizException(401, "骑手账号不可用");
            }
            return id;
        }
        if (demoIdentityEnabled) {
            return DEMO_RIDER_ID;
        }
        throw new BizException(401, "请先登录骑手端");
    }
}

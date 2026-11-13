package com.xingguang.delivery.support;

import com.xingguang.delivery.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 当前登录用户解析。
 *
 * <p>解析顺序：
 * <ol>
 *   <li>{@code Authorization: Bearer <token>} —— 正式会话，受众必须为 USER（见 {@link TokenCodec}）</li>
 *   <li>都没有时回落到演示用户 1，保证未登录也能浏览商品（演示开关关闭时同样回落）</li>
 * </ol>
 *
 * <p><b>安全约束（S2 修复）</b>：
 * <ul>
 *   <li>{@code X-User-Id} 直传身份的通道已移除——改请求头冒充他人用户不再可行；
 *       演示 / e2e 场景请在登录接口换取令牌，或临时开启
 *       {@code xingguang.auth.demo-identity-enabled=true}（仅限本地开发）。</li>
 *   <li>令牌无效直接返回 11001，绝不静默回落到演示用户——
 *       否则令牌过期后用户会看到别人的购物车和订单。</li>
 * </ul>
 */
@Component
public class CurrentUser {

    /** 演示用户 id（与 data.sql 中的 app_user 一致） */
    public static final long DEMO_USER_ID = 1L;

    /** 未登录 / 令牌失效 */
    public static final int CODE_UNAUTHORIZED = 11001;

    private static final String HEADER_AUTH = "Authorization";
    private static final String BEARER = "Bearer ";

    private final HttpServletRequest request;

    @Value("${xingguang.auth.secret:xg-dev-secret-change-me-in-production}")
    private String secret;

    /** 演示身份开关（本地开发 / e2e 用）：允许未登录请求回落到演示用户 */
    @Value("${xingguang.auth.demo-identity-enabled:true}")
    private boolean demoIdentityEnabled;

    public CurrentUser(HttpServletRequest request) {
        this.request = request;
    }

    /** 宽松解析：供「登录与否都能看」的接口使用 */
    public long userId() {
        String auth = request.getHeader(HEADER_AUTH);
        if (auth != null && !auth.isBlank()) {
            long uid = TokenCodec.verify(stripBearer(auth), secret, TokenCodec.AUD_USER);
            if (uid < 0) {
                throw new BizException(CODE_UNAUTHORIZED, "登录状态已失效，请重新登录");
            }
            return uid;
        }
        if (!demoIdentityEnabled) {
            throw new BizException(CODE_UNAUTHORIZED, "请先登录");
        }
        return DEMO_USER_ID;
    }

    /** 严格解析：必须携带有效令牌，否则 11001 */
    public long requireUserId() {
        String auth = request.getHeader(HEADER_AUTH);
        long uid = auth == null || auth.isBlank()
                ? -1L
                : TokenCodec.verify(stripBearer(auth), secret, TokenCodec.AUD_USER);
        if (uid < 0) {
            throw new BizException(CODE_UNAUTHORIZED, "请先登录");
        }
        return uid;
    }

    /** 当前请求是否携带了令牌（不校验有效性） */
    public boolean hasToken() {
        String auth = request.getHeader(HEADER_AUTH);
        return auth != null && !auth.isBlank();
    }

    private static String stripBearer(String raw) {
        String v = raw.trim();
        if (v.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return v.substring(BEARER.length()).trim();
        }
        return v;
    }
}

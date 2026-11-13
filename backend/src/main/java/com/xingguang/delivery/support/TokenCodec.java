package com.xingguang.delivery.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 无状态访问令牌（HMAC-SHA256 签名的紧凑串，形态近似 JWT 但无依赖）。
 *
 * <p>结构：{@code base64url(id.aud.exp) + "." + base64url(HMAC)}。
 * 服务端不落库，因此无需会话表；代价是无法单个吊销，
 * 需要「立即踢下线」时应改为带版本号的用户态校验。
 *
 * <p><b>受众隔离</b>：载荷中带 aud（USER / RIDER / ADMIN / MERCHANT），
 * 各端只接受自己的受众——用户令牌调不了骑手/后台接口（S2 修复）。
 * 历史格式 {@code id.exp}（无受众）仅在校验 USER 时兼容，随存量令牌 7 天 TTL 自然淘汰。
 */
public final class TokenCodec {

    public static final String AUD_USER = "USER";
    public static final String AUD_RIDER = "RIDER";
    public static final String AUD_ADMIN = "ADMIN";
    public static final String AUD_MERCHANT = "MERCHANT";

    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private TokenCodec() {
    }

    /** 签发指定受众的令牌；expEpochSecond 为过期时刻（Unix 秒） */
    public static String issue(long id, String aud, long expEpochSecond, String secret) {
        String body = ENC.encodeToString((id + "." + aud + "." + expEpochSecond).getBytes(StandardCharsets.UTF_8));
        return body + "." + ENC.encodeToString(sign(body, secret));
    }

    /** 校验令牌（受众必须匹配），返回 id；签名不符、已过期或受众不符返回 -1。 */
    public static long verify(String token, String secret, String aud) {
        if (token == null || token.isEmpty()) {
            return -1L;
        }
        int dot = token.lastIndexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return -1L;
        }
        String body = token.substring(0, dot);
        byte[] actual;
        try {
            actual = DEC.decode(token.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            return -1L;
        }
        if (!MessageDigest.isEqual(sign(body, secret), actual)) {
            return -1L;
        }
        String payload;
        try {
            payload = new String(DEC.decode(body), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return -1L;
        }
        String[] parts = payload.split("\\.");
        try {
            // 历史格式 id.exp：仅 USER 受众兼容（存量 C 端令牌过渡期）
            if (parts.length == 2) {
                if (!AUD_USER.equals(aud)) {
                    return -1L;
                }
                long userId = Long.parseLong(parts[0]);
                long exp = Long.parseLong(parts[1]);
                return exp > System.currentTimeMillis() / 1000L ? userId : -1L;
            }
            if (parts.length != 3) {
                return -1L;
            }
            long id = Long.parseLong(parts[0]);
            long exp = Long.parseLong(parts[2]);
            if (exp <= System.currentTimeMillis() / 1000L) {
                return -1L;
            }
            return aud.equals(parts[1]) ? id : -1L;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private static byte[] sign(String body, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("签发令牌失败", e);
        }
    }
}

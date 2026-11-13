package com.xingguang.delivery.support;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码摘要（PBKDF2-HMAC-SHA256 + 随机盐）。
 *
 * <p>只依赖 JDK，不引入 spring-security。存储格式：
 * {@code pbkdf2$<迭代次数>$<saltBase64>$<hashBase64>}，
 * 迭代次数写在串里，后续调高参数时旧密码仍可校验。
 *
 * <p>注意：后台运营账号（admin_user）沿用历史 MD5，本类只用于 C 端用户。
 */
public final class PasswordCodec {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENC = Base64.getEncoder().withoutPadding();

    private PasswordCodec() {
    }

    /** 生成可入库的摘要串 */
    public static String hash(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] dk = derive(raw, salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$" + ENC.encodeToString(salt) + "$" + ENC.encodeToString(dk);
    }

    /** 恒定时间比对，避免时序侧信道 */
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(derive(raw, salt, iterations), expected);
    }

    private static byte[] derive(String raw, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码摘要失败", e);
        }
    }
}

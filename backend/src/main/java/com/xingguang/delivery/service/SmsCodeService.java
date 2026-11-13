package com.xingguang.delivery.service;

import com.xingguang.delivery.common.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码。
 *
 * <p>当前为单机内存实现：进程重启即失效，多实例部署也不共享。
 * 生产环境应替换为 Redis（带 TTL 的 key + 发送频次计数）并接入真实短信网关。
 *
 * <p>开发期打开 {@code xingguang.mock-sms-enabled} 时：
 * 不发真实短信，验证码固定为 {@code mock-sms-code}（默认 8888），
 * 并在接口响应里回传，方便前端直接联调。
 */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);

    /** 验证码有效期 */
    private static final int TTL_SECONDS = 300;
    /** 同一手机号重发间隔 */
    private static final int RESEND_SECONDS = 60;
    /** 单个验证码最大尝试次数 */
    private static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Value("${xingguang.mock-sms-enabled:true}")
    private boolean mockEnabled;

    @Value("${xingguang.mock-sms-code:8888}")
    private String mockCode;

    /** 发送结果；mock 模式下带上明文验证码供前端提示 */
    public record SendResult(int expireSeconds, int resendSeconds, String devCode) {
    }

    private static final class Entry {
        String code;
        long expireAt;
        long sentAt;
        int attempts;

        Entry(String code, long expireAt, long sentAt) {
            this.code = code;
            this.expireAt = expireAt;
            this.sentAt = sentAt;
        }
    }

    /**
     * 发送验证码。同一手机号 60 秒内重复请求直接拒绝，
     * 避免被当作短信轰炸的入口。
     */
    public SendResult send(String phone) {
        long now = System.currentTimeMillis();
        Entry old = store.get(phone);
        if (old != null && now - old.sentAt < RESEND_SECONDS * 1000L) {
            long wait = (RESEND_SECONDS * 1000L - (now - old.sentAt)) / 1000L + 1;
            throw new BizException(11006, "发送过于频繁，请 " + wait + " 秒后再试");
        }

        String code = mockEnabled ? mockCode : randomCode();
        store.put(phone, new Entry(code, now + TTL_SECONDS * 1000L, now));

        if (mockEnabled) {
            log.info("[短信-模拟] {} -> {}", phone, code);
            return new SendResult(TTL_SECONDS, RESEND_SECONDS, code);
        }
        // 真实网关接入点：此处调用短信服务商 API，失败需抛异常
        log.info("[短信] 已向 {} 发送验证码", maskPhone(phone));
        return new SendResult(TTL_SECONDS, RESEND_SECONDS, null);
    }

    /** 校验验证码；通过后立即作废，防止重放 */
    public void verify(String phone, String code) {
        Entry e = store.get(phone);
        if (e == null) {
            throw new BizException(11007, "验证码已失效，请重新获取");
        }
        if (System.currentTimeMillis() > e.expireAt) {
            store.remove(phone);
            throw new BizException(11007, "验证码已过期，请重新获取");
        }
        if (e.attempts >= MAX_ATTEMPTS) {
            store.remove(phone);
            throw new BizException(11008, "尝试次数过多，请重新获取验证码");
        }
        if (!e.code.equals(code)) {
            e.attempts += 1;
            int left = MAX_ATTEMPTS - e.attempts;
            throw new BizException(11009, left > 0 ? "验证码不正确，还可尝试 " + left + " 次" : "尝试次数过多，请重新获取验证码");
        }
        store.remove(phone);
    }

    private static String randomCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static String maskPhone(String phone) {
        return phone.length() < 7 ? phone : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}

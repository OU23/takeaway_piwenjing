package com.xingguang.delivery.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * 启动安全自检（SRS 9.3 / 审查报告 8.2「建立闸门」）：
 * 把「演示可用」与「生产可用」的边界显式化——
 * 模拟支付、模拟短信、演示身份、默认密钥等演示开关在启动时逐项告警，
 * 提醒上线前必须通过环境变量关闭 / 覆盖。
 */
@Configuration
public class StartupGuard {

    private static final Logger log = LoggerFactory.getLogger(StartupGuard.class);

    @Value("${xingguang.mock-pay-enabled:true}")
    private boolean mockPayEnabled;

    @Value("${xingguang.mock-sms-enabled:true}")
    private boolean mockSmsEnabled;

    @Value("${xingguang.auth.demo-identity-enabled:true}")
    private boolean demoIdentityEnabled;

    @Value("${xingguang.auth.secret:xg-dev-secret-change-me-in-production}")
    private String authSecret;

    @Value("${xingguang.pay.notify-secret:xg-dev-pay-notify-secret}")
    private String payNotifySecret;

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        if (mockPayEnabled) {
            log.warn("[安全自检] 模拟支付已开启（xingguang.mock-pay-enabled=true）：任何人可免支付完成订单，"
                    + "生产环境必须设为 false");
        }
        if (mockSmsEnabled) {
            log.warn("[安全自检] 模拟短信已开启：验证码为固定值，生产环境必须设为 false");
        }
        if (demoIdentityEnabled) {
            log.warn("[安全自检] 演示身份已开启（xingguang.auth.demo-identity-enabled=true）："
                    + "未携带令牌的请求将回落到演示账号，生产环境必须设为 false");
        }
        if ("xg-dev-secret-change-me-in-production".equals(authSecret)) {
            log.warn("[安全自检] 令牌签名密钥仍为默认开发值，请通过 XG_AUTH_SECRET 注入生产密钥");
        }
        if ("xg-dev-pay-notify-secret".equals(payNotifySecret)) {
            log.warn("[安全自检] 支付回调验签密钥仍为默认开发值，请通过 XG_PAY_NOTIFY_SECRET 注入");
        }
    }
}

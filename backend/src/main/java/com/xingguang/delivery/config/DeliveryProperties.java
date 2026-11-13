package com.xingguang.delivery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务开关（对应 SRS 1.5 的【保留】/【替换】/【取消】三分法与 9.3 安全要求）。
 * <p>生产环境必须关闭模拟支付与模拟短信（启动校验），避免"一键支付成功"误入正式环境。
 */
@Data
@Component
@ConfigurationProperties(prefix = "xingguang")
public class DeliveryProperties {

    /** 模拟支付通道总开关 */
    private boolean mockPayEnabled = true;

    /** 模拟短信开关 */
    private boolean mockSmsEnabled = true;

    /** 开发环境固定验证码 */
    private String mockSmsCode = "8888";

    private Alipay alipay = new Alipay();

    private Oss oss = new Oss();

    @Data
    public static class Alipay {
        /** 是否启用支付宝沙箱（个人可注册开放平台沙箱，SRS【保留】） */
        private boolean enabled = false;
        private String appId = "";
        private String gateway = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    }

    /** MinIO 对象存储配置；AK/SK 建议通过环境变量 XG_OSS_ACCESS_KEY / XG_OSS_SECRET_KEY 注入。 */
    @Data
    public static class Oss {
        private boolean enabled = true;
        /** S3 API 地址，注意不是 9001 控制台 */
        private String endpoint = "http://192.168.35.120:9000";
        /** 桶公开读时的直链前缀；留空则一律返回临时签名 URL */
        private String publicBaseUrl = "";
        private String bucket = "xingguang-oss";
        private String accessKey = "";
        private String secretKey = "";
        private int presignExpirySeconds = 3600;
        private int maxSizeMb = 5;

        /** 四项必填齐全才认为可用，避免半配置导致启动失败。 */
        public boolean ready() {
            return enabled
                    && endpoint != null && !endpoint.isBlank()
                    && bucket != null && !bucket.isBlank()
                    && accessKey != null && !accessKey.isBlank()
                    && secretKey != null && !secretKey.isBlank();
        }
    }
}

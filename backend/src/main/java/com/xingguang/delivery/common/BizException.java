package com.xingguang.delivery.common;

/**
 * 业务异常。错误码分段（SRS 9.6）：
 * 1xxxx 系统 / 2xxxx 订单 / 3xxxx 资金 / 4xxxx 商品 / 5xxxx 门店。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(10000, message);
    }

    public int getCode() {
        return code;
    }

    /** 通用业务失败 */
    public static BizException of(String message) {
        return new BizException(10000, message);
    }

    /** 订单域 */
    public static BizException order(String message) {
        return new BizException(20000, message);
    }

    /** 资金域 */
    public static BizException pay(String message) {
        return new BizException(30000, message);
    }

    /** 商品域 */
    public static BizException product(String message) {
        return new BizException(40000, message);
    }

    /** 门店域 */
    public static BizException shop(String message) {
        return new BizException(50000, message);
    }
}

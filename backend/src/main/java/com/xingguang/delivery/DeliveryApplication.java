package com.xingguang.delivery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数创星光外卖 · 原型后端启动类。
 *
 * 提示：启动时请显式指定端口（--server.port=8090），
 * 以规避环境变量 SERVER__PORT 的松散绑定劫持（SRS 中未涉及，属本机沙箱特性）。
 */
@SpringBootApplication
@MapperScan("com.xingguang.delivery.mapper")
public class DeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryApplication.class, args);
    }
}

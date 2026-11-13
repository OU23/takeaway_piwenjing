package com.xingguang.delivery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档（springdoc / Swagger UI）：http://localhost:8090/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI xingguangOpenApi() {
        return new OpenAPI().info(new Info()
                .title("数创星光外卖 · 原型后端 API")
                .version("v1")
                .description("单端下单链路原型：首页 → 店铺 → 购物车 → 结算试算 → 下单 → 支付 → 订单详情。"
                        + "金额一律以「分」为单位；写接口支持 Idempotency-Key 幂等。"));
    }
}

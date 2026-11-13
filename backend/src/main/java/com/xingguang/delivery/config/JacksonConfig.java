package com.xingguang.delivery.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花 ID 序列化为字符串。
 *
 * <p>MyBatis-Plus 的 {@code ASSIGN_ID} 生成 19 位长整型，例如 {@code 2114617793801461762}，
 * 远超 JavaScript {@code Number.MAX_SAFE_INTEGER}（9007199254740991，2^53）。
 * 前端用 {@code JSON.parse} 拿到的是 {@code 2114617793801461800} —— 已经被改写的另一个数字，
 * 于是任何「按 id 操作」的请求都会打到不存在的行。
 *
 * <p>实测症状：购物车点「−」后，请求确实发出了
 * {@code PUT /cart/items/2114617793801461800 {"quantity":1}}，服务端回
 * {@code 10000 购物车条目不存在}，前端乐观更新随后被校准回滚，
 * 用户看到的就是「点减号没反应」。删除、改数量同理。
 *
 * <p>因此把 {@code Long}/{@code long} 统一序列化成字符串。前端只把这些 id 当作不透明令牌
 * 原样回传，不做算术，所以改成字符串是安全的；金额仍是 {@code Integer}（分），保持数字类型。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("long-as-string");
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.modulesToInstall(module);
        };
    }
}

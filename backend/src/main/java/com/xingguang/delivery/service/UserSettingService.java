package com.xingguang.delivery.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.entity.UserSetting;
import com.xingguang.delivery.mapper.UserSettingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 用户偏好设置（SRS 9.11 设置模块）。
 *
 * 存储模型：一行一用户，settings 存「按分组嵌套」的 JSON 对象：
 * {"appearance":{"theme":"dark"},"notification":{...},"privacy":{...}}
 * GET 返回「默认值 ∪ 用户已保存值」（深合并后的完整对象）；
 * PUT 接收增量补丁（键为 "分组.键" 点分路径，如 appearance.theme），
 * 校验后合并写回；DELETE 重置为默认（删行）。
 *
 * 校验模型：键与取值范围在 {@link #ALLOWED} 白名单中登记，未登记的键、
 * 类型不符或超出范围的值一律抛业务异常（错误码 10005），不静默容忍。
 * 与前端 store/settings-schema.js 双侧校验，前端先拦、后端兜底。
 */
@Service
public class UserSettingService {

    private static final int CODE_INVALID = 10005;

    /** 免打扰时段 HH:mm */
    private static final Pattern HHMM = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    /**
     * 设置项白名单：键路径 -> 取值规则。
     * 枚举项列出全部合法值；"time" 表示 HH:mm 时刻；布尔项用 "true"/"false" 枚举。
     * 与前端 store/settings-schema.js 保持一致。
     */
    private static final Map<String, List<String>> ALLOWED = new LinkedHashMap<>() {{
        put("appearance.theme", List.of("light", "dark", "system"));
        put("appearance.fontSize", List.of("small", "standard", "large"));
        put("notification.order", List.of("true", "false"));
        put("notification.promo", List.of("true", "false"));
        put("notification.quietMode", List.of("true", "false"));
        put("notification.quietStart", List.of("time"));
        put("notification.quietEnd", List.of("time"));
        put("privacy.personalization", List.of("true", "false"));
    }};

    private final UserSettingMapper mapper;
    private final ObjectMapper objectMapper;

    public UserSettingService(UserSettingMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /** 读取当前生效设置：默认值 ∪ 用户已保存值（深合并后的完整嵌套对象）。 */
    public Map<String, Object> get(long userId) {
        Map<String, Object> result = defaults();
        UserSetting row = mapper.selectById(userId);
        if (row != null) {
            for (Map.Entry<String, Object> e : flatten(parse(row.getSettings())).entrySet()) {
                setNested(result, e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * 增量更新：补丁键为点分路径（appearance.theme），仅接受白名单内的键，
     * 校验通过后与已保存值逐键合并写回（不影响其他键）。
     */
    public Map<String, Object> update(long userId, Map<String, Object> patch) {
        Map<String, Object> flat = patch == null ? Map.of() : flatten(patch);
        if (flat.isEmpty()) {
            throw new BizException(CODE_INVALID, "没有要更新的设置项");
        }
        for (Map.Entry<String, Object> e : flat.entrySet()) {
            validate(e.getKey(), e.getValue());
        }

        Map<String, Object> saved = new LinkedHashMap<>();
        UserSetting row = mapper.selectById(userId);
        if (row != null) {
            saved.putAll(parse(row.getSettings()));
        }
        for (Map.Entry<String, Object> e : flat.entrySet()) {
            setNested(saved, e.getKey(), e.getValue());
        }

        UserSetting upsert = new UserSetting();
        upsert.setUserId(userId);
        upsert.setSettings(write(saved));
        if (row == null) {
            mapper.insert(upsert);
        } else {
            mapper.updateById(upsert);
        }
        return get(userId);
    }

    /** 重置为默认值：直接删行（下次 GET 自然回落到默认值）。 */
    public void reset(long userId) {
        mapper.deleteById(userId);
    }

    /* ------------------------------ 校验 ------------------------------ */

    private void validate(String key, Object value) {
        List<String> rule = ALLOWED.get(key);
        if (rule == null) {
            throw new BizException(CODE_INVALID, "不支持的设置项：" + key);
        }
        if (rule.equals(List.of("time"))) {
            String v = String.valueOf(value);
            if (!HHMM.matcher(v).matches()) {
                throw new BizException(CODE_INVALID, key + " 需为 HH:mm 格式的时刻");
            }
            try {
                LocalTime.parse(v);
            } catch (DateTimeParseException ex) {
                throw new BizException(CODE_INVALID, key + " 不是有效时刻：" + v);
            }
            return;
        }
        // 其余项：统一转成字符串比对，true/false 与枚举都在这一条路上
        if (!rule.contains(String.valueOf(value))) {
            throw new BizException(CODE_INVALID, key + " 的取值不合法：" + value);
        }
    }

    /* ------------------------------ 结构与 JSON 工具 ------------------------------ */

    /** 嵌套 Map → 点分路径扁平 Map（仅一层分组，路径如 appearance.theme） */
    private Map<String, Object> flatten(Map<String, Object> nested) {
        Map<String, Object> flat = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : nested.entrySet()) {
            if (e.getValue() instanceof Map<?, ?> inner) {
                for (Map.Entry<?, ?> ie : ((Map<?, ?>) inner).entrySet()) {
                    flat.put(e.getKey() + "." + ie.getKey(), ie.getValue());
                }
            } else {
                flat.put(e.getKey(), e.getValue());
            }
        }
        return flat;
    }

    /** 按点分路径写入嵌套 Map，中途缺失的分组自动创建（仅白名单键可达此处） */
    @SuppressWarnings("unchecked")
    private void setNested(Map<String, Object> target, String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> cur = target;
        for (int i = 0; i < keys.length - 1; i++) {
            Object next = cur.get(keys[i]);
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<String, Object>();
                cur.put(keys[i], next);
            }
            cur = (Map<String, Object>) next;
        }
        cur.put(keys[keys.length - 1], value);
    }

    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (Exception ex) {
            // 存量数据损坏时按「无自定义设置」处理，不让设置页打不开；
            // 下次保存会覆盖掉脏数据
            return new LinkedHashMap<>();
        }
    }

    private String write(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            throw new BizException(CODE_INVALID, "设置序列化失败");
        }
    }

    /** 默认值（与前端 DEFAULT_SETTINGS 一致，兜底给未保存过设置的用户）。 */
    private Map<String, Object> defaults() {
        Map<String, Object> appearance = new LinkedHashMap<>();
        appearance.put("theme", "system");
        appearance.put("fontSize", "standard");

        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("order", true);
        notification.put("promo", false);
        notification.put("quietMode", false);
        notification.put("quietStart", "23:00");
        notification.put("quietEnd", "07:00");

        Map<String, Object> privacy = new LinkedHashMap<>();
        privacy.put("personalization", true);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("appearance", appearance);
        root.put("notification", notification);
        root.put("privacy", privacy);
        return root;
    }
}

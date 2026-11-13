package com.xingguang.delivery.support;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.entity.AdminUser;
import com.xingguang.delivery.mapper.AdminUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 当前登录运营人员解析（RBAC）。
 *
 * <p>登录接口签发受众为 ADMIN 的签名令牌，请求放入 {@code X-Admin-Token} 头；
 * 服务端验签 + 查库校验状态/角色（见 {@link TokenCodec}，受众隔离：
 * 用户/骑手/商家令牌调不了后台接口）。
 *
 * <p><b>安全约束（S3 修复）</b>：不再无条件回落演示超管。仅当
 * {@code xingguang.auth.demo-identity-enabled=true}（本地开发/e2e）时，
 * 未携带令牌才回落到演示超级管理员；生产环境该开关必须为 false，未认证一律 401。
 */
@Component
public class CurrentAdmin {

    private static final String HEADER = "X-Admin-Token";

    /** 可执行业务写操作的角色（超级管理员 / 运营）；客服仅只读。 */
    private static final Set<String> MANAGE_ROLES = Set.of("SUPER_ADMIN", "OPERATOR");

    private final HttpServletRequest request;
    private final AdminUserMapper adminUserMapper;

    @org.springframework.beans.factory.annotation.Value("${xingguang.auth.secret:xg-dev-secret-change-me-in-production}")
    private String secret;

    @org.springframework.beans.factory.annotation.Value("${xingguang.auth.demo-identity-enabled:true}")
    private boolean demoIdentityEnabled;

    public CurrentAdmin(HttpServletRequest request, AdminUserMapper adminUserMapper) {
        this.request = request;
        this.adminUserMapper = adminUserMapper;
    }

    public AdminUser user() {
        String v = request.getHeader(HEADER);
        if (v == null || v.isBlank()) {
            // 演示兼容（仅限 demo-identity-enabled=true 的本地开发/e2e 环境）
            if (demoIdentityEnabled) {
                AdminUser demo = adminUserMapper.selectById(1L);
                if (demo != null) {
                    return demo;
                }
            }
            throw new BizException(401, "请先登录运营后台");
        }
        Long id = TokenCodec.verify(v.trim(), secret, TokenCodec.AUD_ADMIN);
        if (id < 0) {
            throw new BizException(401, "登录状态已失效，请重新登录");
        }
        AdminUser u = adminUserMapper.selectById(id);
        if (u == null || (u.getDeleted() != null && u.getDeleted() == 1)
                || (u.getStatus() != null && u.getStatus() == 0)) {
            throw new BizException(401, "账号不可用");
        }
        return u;
    }

    public Long id() {
        return user().getId();
    }

    public String role() {
        return user().getRole();
    }

    /** 校验当前账号具备管理（写）权限，否则 403。 */
    public void requireManage() {
        if (!MANAGE_ROLES.contains(role())) {
            throw new BizException(403, "当前角色（" + roleLabel(role()) + "）无操作权限，仅超级管理员 / 运营可操作");
        }
    }

    public static String roleLabel(String role) {
        return switch (role == null ? "" : role) {
            case "SUPER_ADMIN" -> "超级管理员";
            case "OPERATOR" -> "运营";
            case "CS" -> "客服";
            default -> role;
        };
    }
}

package com.xingguang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.entity.AppUser;
import com.xingguang.delivery.mapper.AppUserMapper;
import com.xingguang.delivery.support.PasswordCodec;
import com.xingguang.delivery.support.TokenCodec;
import com.xingguang.delivery.vo.AuthVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * C 端账号体系：注册、登录、会话校验。
 *
 * <p>与前端 {@code utils/validate.js} 保持同一套规则（服务端必须独立校验，
 * 不能信任客户端）。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** 未登录 / 令牌失效 —— 前端据此跳转登录页 */
    public static final int CODE_UNAUTHORIZED = 11001;
    public static final int CODE_USERNAME_TAKEN = 11002;
    public static final int CODE_EMAIL_TAKEN = 11003;
    public static final int CODE_BAD_CREDENTIALS = 11004;
    public static final int CODE_INVALID = 11005;
    public static final int CODE_PHONE_TAKEN = 11010;

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{2,19}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    private final AppUserMapper userMapper;
    private final SmsCodeService smsCodeService;

    @Value("${xingguang.auth.secret:xg-dev-secret-change-me-in-production}")
    private String secret;

    @Value("${xingguang.auth.token-ttl-hours:168}")
    private long tokenTtlHours;

    public AuthService(AppUserMapper userMapper, SmsCodeService smsCodeService) {
        this.userMapper = userMapper;
        this.smsCodeService = smsCodeService;
    }

    /* ------------------------------ 注册 ------------------------------ */

    /**
     * 注册：用户名 + 邮箱 + 手机号（短信验证）+ 密码。
     *
     * <p><b>校验顺序很关键</b>：先做格式与唯一性检查，最后才校验验证码。
     * 因为 {@link SmsCodeService#verify} 一旦通过就会把码作废，
     * 若先验码再报「用户名已占用」，用户的码就白烧了，只能重新发一次。
     */
    @Transactional
    public AuthVO register(String username, String email, String phone, String code, String password) {
        String name = username == null ? "" : username.trim();
        String mail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String tel = phone == null ? "" : phone.trim();
        String smsCode = code == null ? "" : code.trim();

        if (!USERNAME.matcher(name).matches()) {
            throw new BizException(CODE_INVALID, "用户名需为 3-20 位字母、数字或下划线，且以字母开头");
        }
        if (!EMAIL.matcher(mail).matches()) {
            throw new BizException(CODE_INVALID, "请输入有效的邮箱地址");
        }
        if (!PHONE.matcher(tel).matches()) {
            throw new BizException(CODE_INVALID, "请输入有效的 11 位手机号");
        }
        if (smsCode.isEmpty()) {
            throw new BizException(CODE_INVALID, "请输入验证码");
        }
        checkPassword(password);

        // 唯一性：数据库唯一索引是最终保障，这里先给出可读的字段级错误
        if (userMapper.selectCount(Wrappers.<AppUser>lambdaQuery().eq(AppUser::getUsername, name)) > 0) {
            throw new BizException(CODE_USERNAME_TAKEN, "该用户名已被占用");
        }
        if (userMapper.selectCount(Wrappers.<AppUser>lambdaQuery().eq(AppUser::getEmail, mail)) > 0) {
            throw new BizException(CODE_EMAIL_TAKEN, "该邮箱已被注册");
        }
        if (userMapper.selectCount(Wrappers.<AppUser>lambdaQuery().eq(AppUser::getPhone, tel)) > 0) {
            throw new BizException(CODE_PHONE_TAKEN, "该手机号已被注册，可直接登录");
        }

        // 前面都通过了才消费验证码
        smsCodeService.verify(tel, smsCode);

        AppUser u = new AppUser();
        u.setUsername(name);
        u.setEmail(mail);
        u.setPhone(tel);
        u.setPasswordHash(PasswordCodec.hash(password));
        u.setNickname(name);
        u.setBalance(0);
        u.setMemberLevel("NORMAL");
        u.setGrowth(0);
        u.setDeleted(0);
        try {
            userMapper.insert(u);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发注册撞上唯一索引（用户名/邮箱/手机号任一）
            throw new BizException(CODE_USERNAME_TAKEN, "用户名、邮箱或手机号已被占用");
        }
        log.info("[注册] 新用户 id={} username={} phone={}", u.getId(), name, maskPhone(tel));
        return tokenFor(u);
    }

    /* ------------------------------ 登录 ------------------------------ */

    public AuthVO login(String account, String password) {
        String acc = account == null ? "" : account.trim();
        if (acc.isEmpty() || password == null || password.isEmpty()) {
            throw new BizException(CODE_INVALID, "请输入账号和密码");
        }
        String mail = acc.toLowerCase(Locale.ROOT);
        AppUser u = userMapper.selectOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getUsername, acc)
                .or()
                .eq(AppUser::getEmail, mail)
                .last("limit 1"));

        // 账号不存在与密码错误返回同一提示，避免被用来枚举已注册账号
        if (u == null || u.getPasswordHash() == null || !PasswordCodec.matches(password, u.getPasswordHash())) {
            throw new BizException(CODE_BAD_CREDENTIALS, "账号或密码错误");
        }
        if (u.getDeleted() != null && u.getDeleted() == 1) {
            throw new BizException(CODE_BAD_CREDENTIALS, "账号已注销");
        }
        return tokenFor(u);
    }

    /* ---------------------------- 手机验证码 ---------------------------- */

    /** 发送验证码；返回有效期与重发间隔供前端做倒计时 */
    public SmsCodeService.SendResult sendSmsCode(String phone) {
        String p = phone == null ? "" : phone.trim();
        if (!PHONE.matcher(p).matches()) {
            throw new BizException(CODE_INVALID, "请输入有效的 11 位手机号");
        }
        return smsCodeService.send(p);
    }

    /**
     * 手机验证码登录：号码未注册时自动建号。
     *
     * <p>这是国内主流外卖 App 的做法——首次用手机号登录即完成注册，
     * 不要求用户先填表单，降低转化漏斗。
     */
    @Transactional
    public AuthVO loginBySms(String phone, String code) {
        String p = phone == null ? "" : phone.trim();
        String c = code == null ? "" : code.trim();
        if (!PHONE.matcher(p).matches()) {
            throw new BizException(CODE_INVALID, "请输入有效的 11 位手机号");
        }
        if (c.isEmpty()) {
            throw new BizException(CODE_INVALID, "请输入验证码");
        }
        smsCodeService.verify(p, c);
        return tokenFor(findOrCreateByPhone(p));
    }

    private AppUser findOrCreateByPhone(String phone) {
        AppUser u = userMapper.selectOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getPhone, phone)
                .last("limit 1"));
        if (u != null) {
            if (u.getDeleted() != null && u.getDeleted() == 1) {
                throw new BizException(CODE_BAD_CREDENTIALS, "账号已注销");
            }
            return u;
        }

        AppUser n = new AppUser();
        n.setPhone(phone);
        // 用户名由手机号派生：唯一、且符合 3-20 位字母数字规则
        n.setUsername("u" + phone);
        n.setNickname("星友" + phone.substring(phone.length() - 4));
        n.setBalance(0);
        n.setMemberLevel("NORMAL");
        n.setGrowth(0);
        n.setDeleted(0);
        try {
            userMapper.insert(n);
            log.info("[注册] 手机号自动建号 id={} phone={}", n.getId(), maskPhone(phone));
            return n;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发下另一个请求刚建好同一个号码，回查即可
            AppUser again = userMapper.selectOne(Wrappers.<AppUser>lambdaQuery()
                    .eq(AppUser::getPhone, phone)
                    .last("limit 1"));
            if (again == null) {
                throw new BizException(CODE_INVALID, "登录失败，请重试");
            }
            return again;
        }
    }

    private static String maskPhone(String phone) {
        return phone.length() < 7 ? phone : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /* ------------------------------ 会话 ------------------------------ */

    /** 当前登录用户资料；令牌无效时抛 11001 */
    public AuthVO current(long userId) {
        AppUser u = userMapper.selectById(userId);
        if (u == null || (u.getDeleted() != null && u.getDeleted() == 1)) {
            throw new BizException(CODE_UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        AuthVO vo = new AuthVO();
        vo.setUser(brief(u));
        return vo;
    }

    /** 校验令牌（受众 USER），返回用户 id；无效返回 -1 */
    public long resolve(String token) {
        return TokenCodec.verify(token, secret, TokenCodec.AUD_USER);
    }

    private AuthVO tokenFor(AppUser u) {
        long exp = System.currentTimeMillis() / 1000L + tokenTtlHours * 3600L;
        AuthVO vo = new AuthVO();
        vo.setToken(TokenCodec.issue(u.getId(), TokenCodec.AUD_USER, exp, secret));
        vo.setExpiresAt(exp);
        vo.setUser(brief(u));
        return vo;
    }

    private AuthVO.User brief(AppUser u) {
        AuthVO.User v = new AuthVO.User();
        v.setId(u.getId());
        v.setUsername(u.getUsername());
        v.setEmail(u.getEmail());
        v.setNickname(u.getNickname());
        v.setAvatar(u.getAvatar());
        v.setBalance(u.getBalance());
        v.setMemberLevel(u.getMemberLevel());
        v.setMemberLevelLabel(levelLabel(u.getMemberLevel()));
        return v;
    }

    private static String levelLabel(String level) {
        if (level == null) {
            return "普通";
        }
        return switch (level) {
            case "SILVER" -> "白银";
            case "GOLD" -> "黄金";
            case "PLATINUM" -> "铂金";
            case "BLACKGOLD" -> "黑金";
            case "BLACKDIAMOND" -> "黑钻";
            default -> "普通";
        };
    }

    /** 与服务端一致的口令强度：8-64 位且同时含字母与数字 */
    private static void checkPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw new BizException(CODE_INVALID, "密码长度需为 8-64 位");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new BizException(CODE_INVALID, "密码需同时包含字母和数字");
        }
    }
}

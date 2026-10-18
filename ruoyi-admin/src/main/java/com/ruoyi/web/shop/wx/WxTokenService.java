package com.ruoyi.web.shop.wx;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * 小程序登录态管理
 * <p>
 * 采用「UUID token + Redis」而非 JWT：每次请求都需要查 Redis 续期，
 * JWT 的无状态优势在此场景用不上，而 UUID 方案服务端可随时吊销，
 * 且不依赖 jjwt 的 API 版本。Redis key 使用独立前缀 wx_tokens:，
 * 与后台 login_tokens: 命名空间隔离，避免误清缓存时互相影响。
 *
 * @author ruoyi
 */
@Component
public class WxTokenService
{
    /** 顾客登录态 Redis key 前缀 */
    public static final String WX_TOKEN_KEY = "wx_tokens:";

    /** 请求头名称，与后台 Authorization 区分，避免语义混淆 */
    public static final String WX_TOKEN_HEADER = "X-Wx-Token";

    /** 剩余有效期低于此值（毫秒）时自动续期 */
    private static final long REFRESH_THRESHOLD_MILLIS = 24 * 60 * 60 * 1000L;

    private final RedisTemplate<Object, Object> redisTemplate;

    private final WxMiniappProperties properties;

    @SuppressWarnings("unchecked")
    public WxTokenService(RedisTemplate<Object, Object> redisTemplate, WxMiniappProperties properties)
    {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 创建登录态
     *
     * @return 令牌字符串（前端需存入 storage 并在后续请求头携带）
     */
    public String createToken(WxLoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        long now = System.currentTimeMillis();
        long expireMillis = properties.getExpireDays() * 24L * 60L * 60L * 1000L;
        loginUser.setLoginTime(now);
        loginUser.setExpireTime(now + expireMillis);
        redisTemplate.opsForValue().set(WX_TOKEN_KEY + token, loginUser,
                properties.getExpireDays(), TimeUnit.DAYS);
        return token;
    }

    /**
     * 按令牌获取登录态，不存在或已过期返回 null
     */
    public WxLoginUser getLoginUser(String token)
    {
        if (StringUtils.isEmpty(token))
        {
            return null;
        }
        Object cached = redisTemplate.opsForValue().get(WX_TOKEN_KEY + token);
        if (!(cached instanceof WxLoginUser loginUser))
        {
            return null;
        }
        verifyAndRefresh(loginUser);
        return loginUser;
    }

    /**
     * 按令牌获取登录态，无效则抛业务异常（用于需要登录的接口）
     */
    public WxLoginUser requireLoginUser(String token)
    {
        WxLoginUser loginUser = getLoginUser(token);
        if (loginUser == null)
        {
            throw new ServiceException("登录已过期，请重新进入小程序");
        }
        return loginUser;
    }

    /**
     * 剩余有效期不足一天时续期，避免顾客点单中途掉线
     */
    private void verifyAndRefresh(WxLoginUser loginUser)
    {
        Long expireTime = loginUser.getExpireTime();
        if (expireTime == null)
        {
            return;
        }
        long remain = expireTime - System.currentTimeMillis();
        if (remain > 0 && remain < REFRESH_THRESHOLD_MILLIS)
        {
            long newExpire = System.currentTimeMillis() + properties.getExpireDays() * 24L * 60L * 60L * 1000L;
            loginUser.setExpireTime(newExpire);
            redisTemplate.opsForValue().set(WX_TOKEN_KEY + loginUser.getToken(), loginUser,
                    properties.getExpireDays(), TimeUnit.DAYS);
        }
    }

    /**
     * 注销登录态
     */
    public void removeToken(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            redisTemplate.delete(WX_TOKEN_KEY + token);
        }
    }
}

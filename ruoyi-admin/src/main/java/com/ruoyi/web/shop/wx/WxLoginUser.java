package com.ruoyi.web.shop.wx;

import java.io.Serializable;

/**
 * 小程序顾客登录态
 * <p>
 * 与后台 {@code com.ruoyi.common.core.domain.model.LoginUser} 完全隔离：
 * 后台 LoginUser 强绑定 SysUser 且携带菜单权限集合，顾客没有后台账号，
 * 复用会污染权限体系。此类仅承载顾客身份标识，存于独立 Redis 命名空间
 * {@code wx_tokens:}，与后台 {@code login_tokens:} 不冲突。
 *
 * @author ruoyi
 */
public class WxLoginUser implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 顾客ID（wx_user.wx_user_id） */
    private Long wxUserId;

    /** 微信 openid */
    private String openid;

    /** 昵称 */
    private String nickName;

    /** 头像 */
    private String avatarUrl;

    /** 本次登录态令牌（UUID，与 Redis key 对应） */
    private String token;

    /** 登录时间戳（毫秒） */
    private Long loginTime;

    /** 过期时间戳（毫秒） */
    private Long expireTime;

    public WxLoginUser()
    {
    }

    public WxLoginUser(Long wxUserId, String openid, String nickName, String avatarUrl)
    {
        this.wxUserId = wxUserId;
        this.openid = openid;
        this.nickName = nickName;
        this.avatarUrl = avatarUrl;
    }

    public Long getWxUserId()
    {
        return wxUserId;
    }

    public void setWxUserId(Long wxUserId)
    {
        this.wxUserId = wxUserId;
    }

    public String getOpenid()
    {
        return openid;
    }

    public void setOpenid(String openid)
    {
        this.openid = openid;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getAvatarUrl()
    {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl)
    {
        this.avatarUrl = avatarUrl;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public Long getLoginTime()
    {
        return loginTime;
    }

    public void setLoginTime(Long loginTime)
    {
        this.loginTime = loginTime;
    }

    public Long getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Long expireTime)
    {
        this.expireTime = expireTime;
    }
}

package com.ruoyi.web.shop.wx;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import com.ruoyi.web.shop.domain.WxUser;
import com.ruoyi.web.shop.mapper.WxUserMapper;

/**
 * 小程序登录服务
 * <p>
 * 标准流程：小程序 wx.login() 拿到 code → 后端调 jscode2session 换 openid
 * → 按 openid 查找或新建顾客记录 → 签发登录态令牌返回前端。
 * <p>
 * 未配置 appid/secret 且开启 mock-enabled 时，直接把 code 当作 openid，
 * 便于在 H5 或没有小程序账号时联调全流程。上线前必须关闭 mock。
 *
 * @author ruoyi
 */
@Service
public class WxAuthService
{
    private static final Logger log = LoggerFactory.getLogger(WxAuthService.class);

    /** 微信 code2session 接口 */
    private static final String JSCODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final WxUserMapper wxUserMapper;

    private final WxTokenService tokenService;

    private final WxMiniappProperties properties;

    public WxAuthService(WxUserMapper wxUserMapper, WxTokenService tokenService, WxMiniappProperties properties)
    {
        this.wxUserMapper = wxUserMapper;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    /**
     * 小程序登录
     *
     * @param code     wx.login() 返回的临时登录凭证
     * @param nickName 昵称（可选，新版小程序需用户主动授权才能拿到）
     * @param avatarUrl 头像（可选）
     * @return 登录态（含令牌与顾客信息）
     */
    public WxLoginUser login(String code, String nickName, String avatarUrl)
    {
        if (StringUtils.isEmpty(code))
        {
            throw new ServiceException("登录凭证不能为空");
        }

        String openid = resolveOpenid(code);
        WxUser user = wxUserMapper.selectWxUserByOpenid(openid);
        if (user == null)
        {
            user = new WxUser();
            user.setOpenid(openid);
            user.setNickName(StringUtils.isEmpty(nickName) ? "微信用户" : nickName);
            user.setAvatarUrl(avatarUrl == null ? "" : avatarUrl);
            user.setStatus("0");
            wxUserMapper.insertWxUser(user);
            log.info("小程序新顾客注册 openid={} wxUserId={}", maskOpenid(openid), user.getWxUserId());
        }
        else
        {
            if ("1".equals(user.getStatus()))
            {
                throw new ServiceException("账号已被停用，请联系门店");
            }
            // 补全用户新授权的昵称头像
            boolean changed = false;
            if (StringUtils.isNotEmpty(nickName) && !nickName.equals(user.getNickName()))
            {
                user.setNickName(nickName);
                changed = true;
            }
            if (StringUtils.isNotEmpty(avatarUrl) && !avatarUrl.equals(user.getAvatarUrl()))
            {
                user.setAvatarUrl(avatarUrl);
                changed = true;
            }
            if (changed)
            {
                wxUserMapper.updateWxUser(user);
            }
            wxUserMapper.updateLastLoginTime(user.getWxUserId());
        }

        WxLoginUser loginUser = new WxLoginUser(
                user.getWxUserId(), user.getOpenid(), user.getNickName(), user.getAvatarUrl());
        loginUser.setToken(tokenService.createToken(loginUser));
        return loginUser;
    }

    /**
     * 用 code 换 openid
     */
    private String resolveOpenid(String code)
    {
        if (properties.isMockEnabled())
        {
            log.warn("小程序登录处于 MOCK 模式，code 被直接当作 openid 使用。上线前必须关闭 wx.miniapp.mock-enabled");
            return "mock_" + code;
        }
        if (!properties.isConfigured())
        {
            throw new ServiceException("微信小程序 appid/secret 未配置，无法登录");
        }

        String param = "appid=" + properties.getAppid()
                + "&secret=" + properties.getSecret()
                + "&js_code=" + code
                + "&grant_type=authorization_code";
        String response;
        try
        {
            response = HttpUtils.sendGet(JSCODE2SESSION_URL, param);
        }
        catch (Exception e)
        {
            log.error("调用微信 jscode2session 失败", e);
            throw new ServiceException("微信登录服务暂不可用，请稍后重试");
        }
        if (StringUtils.isEmpty(response))
        {
            throw new ServiceException("微信登录返回为空");
        }

        JSONObject json;
        try
        {
            json = JSONObject.parseObject(response);
        }
        catch (Exception e)
        {
            log.error("微信登录返回解析失败: {}", response, e);
            throw new ServiceException("微信登录返回格式异常");
        }

        String openid = json.getString("openid");
        if (StringUtils.isEmpty(openid))
        {
            // 常见错误：40029 code 无效或已被使用；45011 频率限制；40125 secret 错误
            String errcode = json.getString("errcode");
            String errmsg = json.getString("errmsg");
            log.error("微信登录失败 errcode={} errmsg={}", errcode, errmsg);
            throw new ServiceException("微信登录失败：" + (errmsg == null ? errcode : errmsg));
        }
        return openid;
    }

    /** openid 属敏感标识，日志中打码 */
    private static String maskOpenid(String openid)
    {
        if (openid == null || openid.length() <= 8)
        {
            return "***";
        }
        return openid.substring(0, 4) + "****" + openid.substring(openid.length() - 4);
    }
}

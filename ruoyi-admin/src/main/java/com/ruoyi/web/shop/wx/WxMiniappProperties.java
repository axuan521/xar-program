package com.ruoyi.web.shop.wx;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置
 * <p>
 * 在 application.yml 中配置：
 * <pre>
 * wx:
 *   miniapp:
 *     appid: wx1234567890abcdef
 *     secret: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *     mock-enabled: false
 * </pre>
 * 开发期没有 appid 时可设 mock-enabled=true，用前端传入的 code 直接当作 openid，
 * 便于在 H5 或微信开发者工具里跑通全流程；上线前必须关闭。
 *
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxMiniappProperties
{
    /** 小程序 AppID */
    private String appid = "";

    /** 小程序 AppSecret */
    private String secret = "";

    /** 是否启用本地模拟登录（仅供开发联调，上线必须为 false） */
    private boolean mockEnabled = false;

    /** 登录态有效期（天） */
    private int expireDays = 7;

    public String getAppid()
    {
        return appid;
    }

    public void setAppid(String appid)
    {
        this.appid = appid;
    }

    public String getSecret()
    {
        return secret;
    }

    public void setSecret(String secret)
    {
        this.secret = secret;
    }

    public boolean isMockEnabled()
    {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled)
    {
        this.mockEnabled = mockEnabled;
    }

    public int getExpireDays()
    {
        return expireDays;
    }

    public void setExpireDays(int expireDays)
    {
        this.expireDays = expireDays;
    }

    /** 是否已正确配置微信凭据 */
    public boolean isConfigured()
    {
        return appid != null && !appid.isEmpty() && secret != null && !secret.isEmpty();
    }
}

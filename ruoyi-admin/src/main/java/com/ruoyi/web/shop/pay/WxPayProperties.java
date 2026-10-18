package com.ruoyi.web.shop.pay;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;

/**
 * 微信支付 V3 配置
 * <p>
 * 全部字段走 application.yml 配置，未填齐时支付通道自动禁用，
 * 点单流程回退为「到店付款」，不影响其余功能上线。
 * <pre>
 * wx:
 *   pay:
 *     appid: wx8888888888888888              # 小程序 AppID（须与商户号完成绑定授权）
 *     mchid: "1900000109"                    # 商户号
 *     merchant-serial-number: 4A2B3C...      # 商户 API 证书序列号
 *     private-key-path: /home/ruoyi/cert/apiclient_key.pem
 *     api-v3-key: 32位APIv3密钥               # 商户平台-API安全-APIv3密钥
 *     notify-url: https://yourdomain/mp/pay/notify
 *     public-key-id: PUB_KEY_ID_0116...      # 微信支付公钥ID（验签用，推荐公钥模式）
 *     public-key-path: /home/ruoyi/cert/pub_key.pem
 * </pre>
 * 前置条件：微信支付商户号仅对个体工商户、企业等主体开放，个人主体无法申请。
 *
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "wx.pay")
public class WxPayProperties
{
    /** 小程序 AppID */
    private String appid = "";

    /** 商户号 */
    private String mchid = "";

    /** 商户 API 证书序列号 */
    private String merchantSerialNumber = "";

    /** 商户私钥文件路径（apiclient_key.pem，PKCS#8） */
    private String privateKeyPath = "";

    /** APIv3 密钥（32 位），用于回调解密 */
    private String apiV3Key = "";

    /** 支付结果回调地址，必须为 HTTPS 且外网可访问 */
    private String notifyUrl = "";

    /** 微信支付公钥ID（Wechatpay-Serial 需与之匹配） */
    private String publicKeyId = "";

    /** 微信支付公钥文件路径（或平台证书 pem），用于回调验签 */
    private String publicKeyPath = "";

    /** 是否显式关闭微信支付（true 时即使配置齐全也走到店付款） */
    private boolean disabled = false;

    /**
     * 是否允许「模拟支付」。
     * <p>
     * 仅用于开发联调：未取得商户号前，顾客在小程序点「模拟支付」即可把订单
     * 置为已支付并进入待制作，便于跑通取餐码、店员叫号等全流程。
     * 生产环境必须为 false，否则任何人都能绕过真实收款。
     */
    private boolean mockEnabled = false;

    /**
     * 配置是否齐全，决定是否启用微信支付通道
     */
    public boolean isConfigured()
    {
        return !disabled
                && StringUtils.isNotEmpty(appid)
                && StringUtils.isNotEmpty(mchid)
                && StringUtils.isNotEmpty(merchantSerialNumber)
                && StringUtils.isNotEmpty(privateKeyPath)
                && StringUtils.isNotEmpty(apiV3Key)
                && StringUtils.isNotEmpty(notifyUrl)
                && StringUtils.isNotEmpty(publicKeyPath);
    }

    public String getAppid()
    {
        return appid;
    }

    public void setAppid(String appid)
    {
        this.appid = appid;
    }

    public String getMchid()
    {
        return mchid;
    }

    public void setMchid(String mchid)
    {
        this.mchid = mchid;
    }

    public String getMerchantSerialNumber()
    {
        return merchantSerialNumber;
    }

    public void setMerchantSerialNumber(String merchantSerialNumber)
    {
        this.merchantSerialNumber = merchantSerialNumber;
    }

    public String getPrivateKeyPath()
    {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath)
    {
        this.privateKeyPath = privateKeyPath;
    }

    public String getApiV3Key()
    {
        return apiV3Key;
    }

    public void setApiV3Key(String apiV3Key)
    {
        this.apiV3Key = apiV3Key;
    }

    public String getNotifyUrl()
    {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl)
    {
        this.notifyUrl = notifyUrl;
    }

    public String getPublicKeyId()
    {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId)
    {
        this.publicKeyId = publicKeyId;
    }

    public String getPublicKeyPath()
    {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath)
    {
        this.publicKeyPath = publicKeyPath;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public boolean isMockEnabled()
    {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled)
    {
        this.mockEnabled = mockEnabled;
    }
}

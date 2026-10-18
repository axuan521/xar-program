package com.ruoyi.web.shop.pay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.PrivateKey;
import java.security.PublicKey;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 微信支付 V3 通道（小程序 JSAPI 支付）
 * <p>
 * <b>启用前提</b>：微信支付商户号仅对个体工商户、企业、事业单位等主体开放，
 * 个人主体无法申请。取得商户号并在 application.yml 填齐 wx.pay.* 配置后，
 * 本通道自动生效，订单与小程序页面无需改动。
 * <p>
 * 配置未齐全时不抛异常，由 {@link PaymentRouter} 回退到到店付款，
 * 保证点单功能可以先上线。
 * <p>
 * 实现说明：仅用 JDK 原生 RSA/AES-GCM，不引入微信支付 SDK，
 * 以控制依赖体积（服务器内存紧张）。
 *
 * @author ruoyi
 */
@Service("wxPayService")
public class WxPayServiceImpl implements PaymentService
{
    private static final Logger log = LoggerFactory.getLogger(WxPayServiceImpl.class);

    /** 微信支付 */
    public static final String PAY_TYPE_WXPAY = "2";

    /** 小程序下单接口路径（参与签名，不含域名） */
    private static final String JSAPI_PATH = "/v3/pay/transactions/jsapi";

    /** 订单查询接口路径前缀 */
    private static final String QUERY_PATH_PREFIX = "/v3/pay/transactions/out-trade-no/";

    /** 主域名 */
    private static final String API_HOST = "https://api.mch.weixin.qq.com";

    /** 备用域名，主域名不可用时切换 */
    private static final String API_HOST_BACKUP = "https://api2.mch.weixin.qq.com";

    private final WxPayProperties properties;

    private final RestTemplate restTemplate = new RestTemplate();

    /** 商户私钥，启动时加载一次并缓存（每次读文件解析开销大） */
    private PrivateKey merchantPrivateKey;

    /** 微信支付公钥，用于回调验签 */
    private PublicKey wxPublicKey;

    public WxPayServiceImpl(WxPayProperties properties)
    {
        this.properties = properties;
    }

    /**
     * 启动时加载密钥；未配置则跳过，不影响应用启动
     */
    @PostConstruct
    public void init()
    {
        if (!properties.isConfigured())
        {
            log.warn("微信支付未配置齐全（wx.pay.*），支付通道降级为「到店付款」。"
                    + "商户号需个体工商户及以上主体方可申请。");
            return;
        }
        try
        {
            merchantPrivateKey = WxPayCryptoUtils.loadPrivateKey(properties.getPrivateKeyPath());
            wxPublicKey = WxPayCryptoUtils.loadPublicKey(properties.getPublicKeyPath());
            log.info("微信支付通道已启用 mchid={} appid={}", properties.getMchid(), properties.getAppid());
        }
        catch (Exception e)
        {
            // 密钥文件读不到时不阻断启动，仅降级为到店付款
            merchantPrivateKey = null;
            wxPublicKey = null;
            log.error("微信支付密钥加载失败，已降级为「到店付款」。请检查 wx.pay.private-key-path "
                    + "与 wx.pay.public-key-path 指向的文件是否存在且格式正确", e);
        }
    }

    /**
     * 通道是否真正可用（配置齐全且密钥加载成功）
     */
    public boolean isAvailable()
    {
        return properties.isConfigured() && merchantPrivateKey != null;
    }

    @Override
    public String payType()
    {
        return PAY_TYPE_WXPAY;
    }

    /**
     * 供回调验签使用的公钥
     */
    public PublicKey getWxPublicKey()
    {
        return wxPublicKey;
    }

    public String getApiV3Key()
    {
        return properties.getApiV3Key();
    }

    @Override
    public PrepayResult prepay(String orderNo, BigDecimal amount, String subject, String openid)
    {
        if (!isAvailable())
        {
            throw new ServiceException("微信支付未启用，请改用到店付款或先完成商户配置");
        }
        if (StringUtils.isEmpty(openid))
        {
            throw new ServiceException("缺少顾客 openid，无法发起微信支付");
        }

        // 微信金额单位为「分」，必须为整数；用 setScale 确保不会出现精度尾数
        int amountFen = amount.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
        if (amountFen <= 0)
        {
            throw new ServiceException("支付金额必须大于 0");
        }

        JSONObject body = new JSONObject();
        body.put("appid", properties.getAppid());
        body.put("mchid", properties.getMchid());
        body.put("description", StringUtils.isEmpty(subject) ? "门店点单" : truncate(subject, 120));
        body.put("out_trade_no", orderNo);
        body.put("notify_url", properties.getNotifyUrl());
        body.put("amount", new JSONObject().fluentPut("total", amountFen).fluentPut("currency", "CNY"));
        body.put("payer", new JSONObject().fluentPut("openid", openid));

        // 发送的字节必须与签名的字符串完全一致，故只序列化一次
        String jsonBody = body.toJSONString();
        String prepayId;
        try
        {
            prepayId = postForPrepayId(jsonBody);
        }
        catch (Exception e)
        {
            log.error("微信下单失败 orderNo={} body={}", orderNo, jsonBody, e);
            throw new ServiceException("发起微信支付失败：" + e.getMessage());
        }

        try
        {
            String[] sign = WxPayCryptoUtils.buildRequestPaymentSign(
                    properties.getAppid(), prepayId, merchantPrivateKey);
            log.info("微信支付下单成功 orderNo={} prepayId={}", orderNo, prepayId);
            return PrepayResult.wxpay(orderNo, amount.toPlainString(), sign);
        }
        catch (Exception e)
        {
            log.error("调起支付参数签名失败 orderNo={}", orderNo, e);
            throw new ServiceException("生成支付参数失败");
        }
    }

    /**
     * 调 JSAPI 下单，返回 prepay_id。主域名失败时重试备用域名。
     */
    private String postForPrepayId(String jsonBody) throws Exception
    {
        String authorization = WxPayCryptoUtils.buildAuthorization(
                "POST", JSAPI_PATH, jsonBody,
                properties.getMchid(), properties.getMerchantSerialNumber(), merchantPrivateKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.set(HttpHeaders.USER_AGENT, "RuoYi-Shop/1.0");

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> response;
        try
        {
            response = restTemplate.exchange(API_HOST + JSAPI_PATH, HttpMethod.POST, entity, String.class);
        }
        catch (Exception primary)
        {
            log.warn("微信支付主域名调用失败，尝试备用域名: {}", primary.getMessage());
            response = restTemplate.exchange(API_HOST_BACKUP + JSAPI_PATH, HttpMethod.POST, entity, String.class);
        }

        String payload = response.getBody();
        if (StringUtils.isEmpty(payload))
        {
            throw new IllegalStateException("微信下单返回为空");
        }
        JSONObject json = JSONObject.parseObject(payload);
        String prepayId = json.getString("prepay_id");
        if (StringUtils.isEmpty(prepayId))
        {
            // 常见错误：PARAM_ERROR 参数有误、NO_AUTH 商户无权限、
            // SIGN_ERROR 签名错误（多为证书序列号或私钥不匹配）
            throw new IllegalStateException("code=" + json.getString("code")
                    + " message=" + json.getString("message"));
        }
        return prepayId;
    }

    /**
     * 主动查询订单支付状态（回调丢失时对账用）
     */
    @Override
    public boolean queryPaid(String orderNo)
    {
        if (!isAvailable())
        {
            return false;
        }
        try
        {
            String path = QUERY_PATH_PREFIX + orderNo + "?mchid=" + properties.getMchid();
            String authorization = WxPayCryptoUtils.buildAuthorization(
                    "GET", path, "",
                    properties.getMchid(), properties.getMerchantSerialNumber(), merchantPrivateKey);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
            headers.set(HttpHeaders.USER_AGENT, "RuoYi-Shop/1.0");

            ResponseEntity<String> response = restTemplate.exchange(API_HOST + path, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            JSONObject json = JSONObject.parseObject(response.getBody());
            // SUCCESS 才是已支付；NOTPAY/USERPAYING/CLOSED/REVOKED/PAYERROR 均视为未支付
            return "SUCCESS".equals(json.getString("trade_state"));
        }
        catch (Exception e)
        {
            log.error("查询微信支付订单状态失败 orderNo={}", orderNo, e);
            return false;
        }
    }

    private static String truncate(String text, int max)
    {
        return text.length() <= max ? text : text.substring(0, max);
    }
}

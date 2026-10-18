package com.ruoyi.web.controller.shop;

import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.web.shop.pay.OrderPaymentCallback;
import com.ruoyi.web.shop.pay.WxPayCryptoUtils;
import com.ruoyi.web.shop.pay.WxPayProperties;
import com.ruoyi.web.shop.pay.WxPayServiceImpl;

/**
 * 微信支付结果回调
 * <p>
 * 标注 {@code @Anonymous} 让若依放行（微信服务器不带后台 JWT），
 * 安全性完全依赖本类的验签逻辑，<b>不得跳过任何一步</b>：
 * <ol>
 * <li>校验时间戳在 5 分钟内，防重放</li>
 * <li>校验 Wechatpay-Serial 与配置的公钥ID一致（若已配置）</li>
 * <li>用微信支付公钥对「时间戳\n随机串\n原始报文\n」验签，确认请求来自微信</li>
 * <li>用 APIv3 密钥 AES-256-GCM 解密 resource 得到支付结果明文</li>
 * <li>幂等更新订单</li>
 * </ol>
 * 应答规范：成功返回 200 + {@code {"code":"SUCCESS"}}；
 * 失败必须返回非 200（此处用 500）+ {@code {"code":"FAIL"}}，
 * 微信才会按 15s/15s/30s/3m/... 的节奏重试，最长 24 小时。
 * <p>
 * 验签必须使用<b>原始请求体字节</b>，不能用反序列化再序列化的 JSON，
 * 否则字节不一致会导致验签永远失败。
 *
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/mp/pay")
public class WxPayNotifyController
{
    private static final Logger log = LoggerFactory.getLogger(WxPayNotifyController.class);

    /** 时间戳允许偏差（秒），超过则判定为重放攻击 */
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300L;

    private final WxPayProperties properties;

    private final WxPayServiceImpl wxPayService;

    /** 订单模块实现，未就绪时为空，回调查询接口仍可启动 */
    private final ObjectProvider<OrderPaymentCallback> callbackProvider;

    public WxPayNotifyController(WxPayProperties properties, WxPayServiceImpl wxPayService,
            ObjectProvider<OrderPaymentCallback> callbackProvider)
    {
        this.properties = properties;
        this.wxPayService = wxPayService;
        this.callbackProvider = callbackProvider;
    }

    /**
     * 支付结果通知（配置在 wx.pay.notify-url，必须 HTTPS 且外网可达）
     */
    @PostMapping("/notify")
    public ResponseEntity<String> notify(HttpServletRequest request)
    {
        String body;
        try
        {
            body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            log.error("读取支付回调报文失败", e);
            return fail("读取报文失败");
        }

        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String signature = request.getHeader("Wechatpay-Signature");
        String serial = request.getHeader("Wechatpay-Serial");

        if (StringUtils.isEmpty(timestamp) || StringUtils.isEmpty(nonce) || StringUtils.isEmpty(signature))
        {
            log.warn("支付回调缺少验签头 serial={} 报文={}", serial, brief(body));
            return fail("缺少签名头");
        }

        // 1. 时间戳校验：防止旧报文被重放
        if (!isTimestampValid(timestamp))
        {
            log.warn("支付回调时间戳超出允许范围 timestamp={}", timestamp);
            return fail("时间戳无效");
        }

        // 2. 证书序列号校验（公钥模式下 Wechatpay-Serial 即公钥ID）
        String configuredKeyId = properties.getPublicKeyId();
        if (StringUtils.isNotEmpty(configuredKeyId) && !configuredKeyId.equals(serial))
        {
            log.warn("支付回调证书序列号不匹配 expected={} actual={}", configuredKeyId, serial);
            return fail("证书序列号不匹配");
        }

        // 3. 验签：确认请求确实来自微信支付
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";
        if (!WxPayCryptoUtils.verify(message, signature, wxPayService.getWxPublicKey()))
        {
            log.error("支付回调验签失败，可能为伪造请求或公钥配置错误 serial={} 报文={}", serial, brief(body));
            return fail("验签失败");
        }

        // 4. 解密支付结果
        JSONObject payload;
        try
        {
            JSONObject envelope = JSONObject.parseObject(body);
            JSONObject resource = envelope.getJSONObject("resource");
            if (resource == null)
            {
                return fail("报文缺少 resource");
            }
            String plain = WxPayCryptoUtils.decryptAesGcm(
                    properties.getApiV3Key(),
                    resource.getString("nonce"),
                    resource.getString("associated_data"),
                    resource.getString("ciphertext"));
            payload = JSONObject.parseObject(plain);
        }
        catch (Exception e)
        {
            log.error("支付回调解密失败，请检查 APIv3 密钥是否正确", e);
            return fail("解密失败");
        }

        // 5. 交给订单模块幂等处理
        String orderNo = payload.getString("out_trade_no");
        String tradeState = payload.getString("trade_state");
        String transactionId = payload.getString("transaction_id");
        log.info("支付回调验签解密成功 orderNo={} tradeState={}", orderNo, tradeState);

        OrderPaymentCallback callback = callbackProvider.getIfAvailable();
        if (callback == null)
        {
            // 订单模块未就绪时返回失败，让微信稍后重试，避免支付结果丢失
            log.warn("订单回调处理器未注册，orderNo={} 将由微信重试", orderNo);
            return fail("业务处理器未就绪");
        }

        try
        {
            if ("SUCCESS".equals(tradeState))
            {
                Integer paidFen = null;
                JSONObject amount = payload.getJSONObject("amount");
                if (amount != null)
                {
                    paidFen = amount.getInteger("payer_total");
                    if (paidFen == null)
                    {
                        paidFen = amount.getInteger("total");
                    }
                }
                callback.handlePaySuccess(orderNo, transactionId, paidFen);
            }
            else
            {
                // USERPAYING 为支付中不应标记失败，交由主动查询对账
                if (!"USERPAYING".equals(tradeState))
                {
                    callback.handlePayFail(orderNo, tradeState + ":" + payload.getString("trade_state_desc"));
                }
            }
        }
        catch (Exception e)
        {
            log.error("处理支付回调业务失败 orderNo={}", orderNo, e);
            return fail("业务处理失败");
        }

        return ResponseEntity.ok("{\"code\":\"SUCCESS\",\"message\":\"成功\"}");
    }

    /**
     * 时间戳是否在允许偏差内（秒级时间戳）
     */
    private static boolean isTimestampValid(String timestamp)
    {
        try
        {
            long ts = Long.parseLong(timestamp);
            long now = System.currentTimeMillis() / 1000;
            return Math.abs(now - ts) <= TIMESTAMP_TOLERANCE_SECONDS;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    /**
     * 失败应答：必须非 200，微信才会重试
     */
    private static ResponseEntity<String> fail(String message)
    {
        JSONObject json = new JSONObject();
        json.put("code", "FAIL");
        json.put("message", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(json.toJSONString());
    }

    /** 日志只截取报文前 200 字，避免敏感信息刷屏 */
    private static String brief(String body)
    {
        if (body == null)
        {
            return "";
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}

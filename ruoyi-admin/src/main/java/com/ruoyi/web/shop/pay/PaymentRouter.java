package com.ruoyi.web.shop.pay;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 支付通道路由器
 * <p>
 * 订单模块只注入 {@link PaymentService}，由本路由器（{@code @Primary}）决定走哪条通道：
 * <ul>
 * <li>微信支付配置齐全且密钥加载成功 → 微信支付</li>
 * <li>否则 → 到店付款（顾客扫门店收款码，店员核销）</li>
 * </ul>
 * 这样商户号下来后只需填配置重启，业务代码零改动。
 * <p>
 * 注：本类自身也实现 {@link PaymentService} 并标 {@code @Primary}，
 * 因此注入点不会因存在多个实现而产生歧义。
 *
 * @author ruoyi
 */
@Primary
@Service("paymentService")
public class PaymentRouter implements PaymentService
{
    private static final Logger log = LoggerFactory.getLogger(PaymentRouter.class);

    private final OfflinePaymentServiceImpl offlinePaymentService;

    private final WxPayServiceImpl wxPayService;

    private final WxPayProperties wxPayProperties;

    public PaymentRouter(OfflinePaymentServiceImpl offlinePaymentService, WxPayServiceImpl wxPayService,
            WxPayProperties wxPayProperties)
    {
        this.offlinePaymentService = offlinePaymentService;
        this.wxPayService = wxPayService;
        this.wxPayProperties = wxPayProperties;
    }

    /**
     * 选取当前生效的支付通道
     */
    public PaymentService current()
    {
        return wxPayService.isAvailable() ? wxPayService : offlinePaymentService;
    }

    /**
     * 微信支付是否可用（供小程序端展示「在线支付/到店付款」文案）
     */
    public boolean isWxPayEnabled()
    {
        return wxPayService.isAvailable();
    }

    /**
     * 是否允许模拟支付（仅开发联调，生产必须关闭）
     */
    public boolean isMockPayEnabled()
    {
        return wxPayProperties.isMockEnabled();
    }

    @Override
    public String payType()
    {
        return current().payType();
    }

    @Override
    public PrepayResult prepay(String orderNo, BigDecimal amount, String subject, String openid)
    {
        PaymentService target = current();
        log.info("支付路由 orderNo={} 通道={}", orderNo, target.getClass().getSimpleName());
        return target.prepay(orderNo, amount, subject, openid);
    }

    @Override
    public boolean queryPaid(String orderNo)
    {
        return current().queryPaid(orderNo);
    }
}

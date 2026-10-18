package com.ruoyi.web.shop.pay;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 到店付款通道（默认启用，无需任何资质）
 * <p>
 * 适用场景：个人主体小程序无法开通微信支付时的合规替代方案。
 * 顾客在小程序下单取得取餐码后，扫门店张贴的「个人经营收款码」或
 * 「商家收款码」完成付款（该收款码凭身份证即可开通，资金直接进个人账户，
 * 不经平台中转，无二清风险），再由店员在小程序店员端点击「已收款」核销。
 * <p>
 * 本通道不产生任何支付流水号，订单 pay_status 由店员核销动作驱动。
 *
 * @author ruoyi
 */
@Service("offlinePaymentService")
public class OfflinePaymentServiceImpl implements PaymentService
{
    private static final Logger log = LoggerFactory.getLogger(OfflinePaymentServiceImpl.class);

    /** 到店付款 */
    public static final String PAY_TYPE_OFFLINE = "1";

    @Override
    public String payType()
    {
        return PAY_TYPE_OFFLINE;
    }

    @Override
    public PrepayResult prepay(String orderNo, BigDecimal amount, String subject, String openid)
    {
        log.info("到店付款下单 orderNo={} amount={}", orderNo, amount);
        return PrepayResult.offline(orderNo, amount.toPlainString());
    }

    @Override
    public boolean queryPaid(String orderNo)
    {
        // 线下收款无接口可查，支付状态完全由店员核销操作决定
        return false;
    }
}

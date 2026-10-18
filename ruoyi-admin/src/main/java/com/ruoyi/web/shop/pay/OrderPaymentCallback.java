package com.ruoyi.web.shop.pay;

/**
 * 支付成功后的业务回调处理
 * <p>
 * 由订单模块实现（更新订单为已支付、写支付流水号与支付时间）。
 * 支付回调 Controller 通过 {@code ObjectProvider} 获取本接口实现，
 * 因此订单模块尚未就绪时支付层仍可独立编译与启动。
 * <p>
 * <b>实现要求</b>：必须幂等。微信会在未收到正确应答时重复推送回调
 * （间隔 15s/15s/30s/3m/10m/20m/30m/30m/30m/60m/3h/3h/3h/6h/6h，共 24h4m），
 * 已支付的订单重复通知时应直接返回成功而不重复累加金额或重复推送。
 *
 * @author ruoyi
 */
public interface OrderPaymentCallback
{
    /**
     * 处理支付成功
     *
     * @param orderNo       商户订单号
     * @param transactionId 微信支付订单号
     * @param paidAmountFen 实际支付金额（分）
     * @return true 表示处理成功（或此前已处理过）
     */
    boolean handlePaySuccess(String orderNo, String transactionId, Integer paidAmountFen);

    /**
     * 处理支付失败/关闭
     *
     * @param orderNo 商户订单号
     * @param reason  失败原因
     */
    void handlePayFail(String orderNo, String reason);
}

package com.ruoyi.web.shop.pay.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.web.shop.domain.ShopOrder;
import com.ruoyi.web.shop.mapper.ShopOrderMapper;
import com.ruoyi.web.shop.pay.OrderPaymentCallback;

/**
 * 订单支付回调处理器
 * <p>
 * 实现 {@link OrderPaymentCallback} 接口，微信支付成功后更新订单状态。
 * <p>
 * 微信支付流程：顾客下单 → 订单状态"待付款" → 微信支付成功回调 → 更新支付状态为"已支付" → 订单状态变为"待制作"
 *
 * @author ruoyi
 */
@Component
public class ShopOrderPaymentCallback implements OrderPaymentCallback
{
    private static final Logger log = LoggerFactory.getLogger(ShopOrderPaymentCallback.class);

    private final ShopOrderMapper orderMapper;

    public ShopOrderPaymentCallback(ShopOrderMapper orderMapper)
    {
        this.orderMapper = orderMapper;
    }

    @Override
    public boolean handlePaySuccess(String orderNo, String transactionId, Integer paidAmountFen)
    {
        ShopOrder order = orderMapper.selectShopOrderByOrderNo(orderNo);
        if (order == null)
        {
            log.warn("支付回调：订单不存在 orderNo={}", orderNo);
            return false;
        }
        if ("1".equals(order.getPayStatus()))
        {
            log.info("支付回调：订单已支付，忽略重复回调 orderNo={}", orderNo);
            return true;
        }
        // 更新支付状态为已支付，订单状态变为待制作
        orderMapper.updatePayStatus(order.getOrderId(), "1", transactionId, new java.util.Date());
        orderMapper.updateOrderStatus(order.getOrderId(), "1");
        log.info("支付回调：订单支付成功 orderNo={} transactionId={} paidFen={}", orderNo, transactionId, paidAmountFen);
        return true;
    }

    @Override
    public void handlePayFail(String orderNo, String reason)
    {
        log.warn("支付回调：订单支付失败 orderNo={} reason={}", orderNo, reason);
        // 支付失败不更新订单状态，保持"待付款"，顾客可重新发起支付
    }
}

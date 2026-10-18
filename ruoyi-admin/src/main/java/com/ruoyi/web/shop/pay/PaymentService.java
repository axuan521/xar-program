package com.ruoyi.web.shop.pay;

import java.math.BigDecimal;

/**
 * 支付通道抽象
 * <p>
 * 订单模块只依赖本接口，不感知具体支付方式。默认注入「到店付款」实现；
 * 当 wx.pay 配置齐全（个体工商户取得商户号后）自动切换为「微信支付」实现，
 * 订单表、状态机、小程序页面均无需改动。
 * <p>
 * 切换由 {@link PaymentChannelFactory} 根据配置决定，避免两个实现同时注入产生歧义。
 *
 * @author ruoyi
 */
public interface PaymentService
{
    /**
     * 当前通道类型（对应 shop_order.pay_type）
     */
    String payType();

    /**
     * 发起支付
     *
     * @param orderNo    商户订单号（唯一）
     * @param amount     应付金额（元）
     * @param subject    商品描述，微信支付会展示在收银台
     * @param openid     付款顾客的 openid，微信支付必填
     * @return 前端调起支付所需参数
     */
    PrepayResult prepay(String orderNo, BigDecimal amount, String subject, String openid);

    /**
     * 查询支付结果
     * <p>
     * 用于回调丢失时主动对账。到店付场景由店员人工核销，返回未支付。
     *
     * @param orderNo 商户订单号
     * @return true 表示已支付
     */
    boolean queryPaid(String orderNo);
}

package com.ruoyi.web.shop.pay;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付发起结果
 * <p>
 * 两种支付方式返回不同内容，前端按 payType 分支处理：
 * <ul>
 * <li>到店付款：payType=1，params 为空，前端展示收款码图片与金额，顾客线下扫码付款</li>
 * <li>微信支付：payType=2，params 含 wx.requestPayment 所需的
 *     timeStamp/nonceStr/package/signType/paySign，前端直接调起收银台</li>
 * </ul>
 *
 * @author ruoyi
 */
public class PrepayResult implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 支付方式（1到店付款 2微信支付） */
    private String payType;

    /** 订单编号 */
    private String orderNo;

    /** 应付金额（元），用于到店付时展示 */
    private String amountYuan;

    /** 调起支付所需参数（到店付为空 Map） */
    private Map<String, String> params = new HashMap<>();

    public static PrepayResult offline(String orderNo, String amountYuan)
    {
        PrepayResult result = new PrepayResult();
        result.setPayType("1");
        result.setOrderNo(orderNo);
        result.setAmountYuan(amountYuan);
        return result;
    }

    public static PrepayResult wxpay(String orderNo, String amountYuan, String[] sign)
    {
        PrepayResult result = new PrepayResult();
        result.setPayType("2");
        result.setOrderNo(orderNo);
        result.setAmountYuan(amountYuan);
        result.getParams().put("timeStamp", sign[0]);
        result.getParams().put("nonceStr", sign[1]);
        result.getParams().put("package", sign[2]);
        result.getParams().put("signType", sign[3]);
        result.getParams().put("paySign", sign[4]);
        return result;
    }

    public String getPayType()
    {
        return payType;
    }

    public void setPayType(String payType)
    {
        this.payType = payType;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getAmountYuan()
    {
        return amountYuan;
    }

    public void setAmountYuan(String amountYuan)
    {
        this.amountYuan = amountYuan;
    }

    public Map<String, String> getParams()
    {
        return params;
    }

    public void setParams(Map<String, String> params)
    {
        this.params = params;
    }
}

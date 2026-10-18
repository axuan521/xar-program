package com.ruoyi.web.shop.service;

import java.util.List;
import com.ruoyi.web.shop.domain.ShopOrder;
import com.ruoyi.web.shop.pay.PrepayResult;

/**
 * 订单 服务层
 *
 * @author ruoyi
 */
public interface IShopOrderService
{
    /**
     * 查询订单列表（后台）
     */
    public List<ShopOrder> selectShopOrderList(ShopOrder shopOrder);

    /**
     * 查询订单详情
     */
    public ShopOrder selectShopOrderById(Long orderId);

    /**
     * 查询顾客订单列表（小程序）
     */
    public List<ShopOrder> selectShopOrderByWxUserId(Long wxUserId, String orderStatus);

    /**
     * 查询待处理订单（店员工作台）
     */
    public List<ShopOrder> selectPendingOrders();

    /**
     * 按取餐码查询订单（店员扫码核销用）
     *
     * @param pickupCode 取餐码
     * @return 订单信息
     */
    public ShopOrder selectShopOrderByPickupCode(String pickupCode);

    /**
     * 顾客下单（小程序调用）
     *
     * @param wxUserId    顾客ID
     * @param openid      顾客openid
     * @param items       购物车项列表，每项包含 productId、flavorIds、quantity
     * @param remark      顾客备注
     * @return 订单信息（含取餐码）
     */
    public ShopOrder createOrder(Long wxUserId, String openid, List<OrderItemRequest> items, String remark);

    /**
     * 发起支付（小程序调起收银台前调用）
     * <p>
     * 到店付款通道返回 payType=1，前端展示门店收款指引；
     * 微信支付通道返回 payType=2 及 wx.requestPayment 所需参数。
     *
     * @param orderId  订单ID
     * @param wxUserId 当前顾客ID（用于归属校验）
     * @return 支付发起结果
     */
    public PrepayResult preparePay(Long orderId, Long wxUserId);

    /**
     * 模拟支付（仅开发联调，需显式开启 wx.pay.mock-enabled）
     * <p>
     * 不产生真实资金流，直接把订单置为「已支付 + 待制作」，
     * 便于在未取得微信支付商户号时跑通取餐码与店员叫号流程。
     *
     * @param orderId  订单ID
     * @param wxUserId 当前顾客ID（用于归属校验）
     * @return 更新后的订单
     */
    public ShopOrder mockPay(Long orderId, Long wxUserId);

    /**
     * 取消订单（仅待付款/待制作状态可取消）
     */
    public int cancelOrder(Long orderId, String cancelReason);

    /**
     * 店员核销订单（到店付款场景）
     *
     * @param orderId  订单ID
     * @param verifyBy 核销操作人
     */
    public void verifyOrder(Long orderId, String verifyBy);

    /**
     * 更新订单状态（制作中/待取餐）
     */
    public int updateOrderStatus(Long orderId, String orderStatus);

    /**
     * 完成订单
     */
    public int finishOrder(Long orderId);

    /**
     * 删除订单（逻辑删除）
     */
    public int deleteShopOrderByIds(Long[] orderIds);

    /**
     * 订单明细请求对象
     */
    public static class OrderItemRequest
    {
        private Long productId;
        private List<Long> flavorIds;
        private Integer quantity;

        public Long getProductId()
        {
            return productId;
        }

        public void setProductId(Long productId)
        {
            this.productId = productId;
        }

        public List<Long> getFlavorIds()
        {
            return flavorIds;
        }

        public void setFlavorIds(List<Long> flavorIds)
        {
            this.flavorIds = flavorIds;
        }

        public Integer getQuantity()
        {
            return quantity;
        }

        public void setQuantity(Integer quantity)
        {
            this.quantity = quantity;
        }
    }
}

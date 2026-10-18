package com.ruoyi.web.shop.mapper;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.web.shop.domain.ShopOrder;

/**
 * 订单 数据层
 *
 * @author ruoyi
 */
public interface ShopOrderMapper
{
    /**
     * 查询订单列表
     */
    public List<ShopOrder> selectShopOrderList(ShopOrder shopOrder);

    /**
     * 查询订单详情（含明细列表与顾客信息）
     */
    public ShopOrder selectShopOrderById(Long orderId);

    /**
     * 按订单编号查询订单
     */
    public ShopOrder selectShopOrderByOrderNo(String orderNo);

    /**
     * 查询顾客订单列表（按创建时间倒序）
     */
    public List<ShopOrder> selectShopOrderByWxUserId(@Param("wxUserId") Long wxUserId, @Param("orderStatus") String orderStatus);

    /**
     * 查询某日已分配的最大取餐序号（用于生成新取餐码）
     */
    public Integer selectMaxPickupNo(@Param("pickupDate") LocalDate pickupDate);

    /**
     * 查询待制作/制作中/待取餐的订单（店员工作台用）
     */
    public List<ShopOrder> selectPendingOrders();

    /**
     * 按取餐码查询订单（店员扫码核销用）
     *
     * @param pickupCode 取餐码
     * @return 订单信息
     */
    public ShopOrder selectShopOrderByPickupCode(@Param("pickupCode") String pickupCode);

    /**
     * 新增订单
     */
    public int insertShopOrder(ShopOrder shopOrder);

    /**
     * 更新订单状态
     */
    public int updateOrderStatus(@Param("orderId") Long orderId, @Param("orderStatus") String orderStatus);

    /**
     * 更新支付状态与支付时间（微信支付回调或到店核销时调用）
     */
    public int updatePayStatus(@Param("orderId") Long orderId, @Param("payStatus") String payStatus,
                               @Param("payNo") String payNo, @Param("payTime") java.util.Date payTime);

    /**
     * 核销订单（更新状态为已完成，记录核销人与时间）
     */
    public int verifyOrder(@Param("orderId") Long orderId, @Param("verifyBy") String verifyBy);

    /**
     * 取消订单
     */
    public int cancelOrder(@Param("orderId") Long orderId, @Param("cancelReason") String cancelReason);

    /**
     * 完成订单（更新状态为已完成，记录完成时间）
     */
    public int finishOrder(@Param("orderId") Long orderId);

    /**
     * 删除订单（逻辑删除）
     */
    public int deleteShopOrderById(Long orderId);

    /**
     * 批量删除订单（逻辑删除）
     */
    public int deleteShopOrderByIds(Long[] orderIds);
}

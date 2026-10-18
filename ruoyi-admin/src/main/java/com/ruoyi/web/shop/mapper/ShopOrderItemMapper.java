package com.ruoyi.web.shop.mapper;

import java.util.List;
import com.ruoyi.web.shop.domain.ShopOrderItem;

/**
 * 订单明细 数据层
 *
 * @author ruoyi
 */
public interface ShopOrderItemMapper
{
    /**
     * 查询订单明细列表
     */
    public List<ShopOrderItem> selectShopOrderItemByOrderId(Long orderId);

    /**
     * 批量新增订单明细
     */
    public int batchInsertShopOrderItem(List<ShopOrderItem> itemList);

    /**
     * 删除订单明细
     */
    public int deleteShopOrderItemByOrderId(Long orderId);
}

package com.ruoyi.web.shop.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品口味规格对象 shop_product_flavor
 * <p>
 * 同一商品的口味按 group_name 分组（如"温度""糖度""加料"），
 * 组内多个选项供顾客单选或多选；extra_price &gt; 0 表示加价项。
 *
 * @author ruoyi
 */
public class ShopProductFlavor implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 口味ID */
    private Long flavorId;

    /** 所属商品 */
    private Long productId;

    /** 口味组名 */
    private String groupName;

    /** 口味选项名 */
    private String flavorName;

    /** 加价金额 */
    private BigDecimal extraPrice;

    /** 是否默认选中（0否 1是） */
    private String isDefault;

    /** 组内显示顺序 */
    private Integer orderNum;

    /** 组显示顺序 */
    private Integer groupOrderNum;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getFlavorId()
    {
        return flavorId;
    }

    public void setFlavorId(Long flavorId)
    {
        this.flavorId = flavorId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public String getFlavorName()
    {
        return flavorName;
    }

    public void setFlavorName(String flavorName)
    {
        this.flavorName = flavorName;
    }

    public BigDecimal getExtraPrice()
    {
        return extraPrice;
    }

    public void setExtraPrice(BigDecimal extraPrice)
    {
        this.extraPrice = extraPrice;
    }

    public String getIsDefault()
    {
        return isDefault;
    }

    public void setIsDefault(String isDefault)
    {
        this.isDefault = isDefault;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public Integer getGroupOrderNum()
    {
        return groupOrderNum;
    }

    public void setGroupOrderNum(Integer groupOrderNum)
    {
        this.groupOrderNum = groupOrderNum;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}

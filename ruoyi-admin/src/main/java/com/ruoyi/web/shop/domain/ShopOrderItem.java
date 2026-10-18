package com.ruoyi.web.shop.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;

/**
 * 订单明细对象 shop_order_item
 * <p>
 * 商品名与单价在此快照存储：商品日后改价或下架，历史订单金额不受影响。
 *
 * @author ruoyi
 */
public class ShopOrderItem
{
    /** 明细ID */
    private Long itemId;

    /** 所属订单 */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品名称快照 */
    @Excel(name = "商品名称")
    private String productName;

    /** 商品图片快照 */
    private String productImage;

    /** 下单时单价快照（元） */
    @Excel(name = "单价")
    private BigDecimal unitPrice;

    /** 口味描述（少冰/三分糖/加珍珠） */
    @Excel(name = "口味")
    private String flavorText;

    /** 口味加价合计（元） */
    private BigDecimal flavorAmount;

    /** 数量 */
    @Excel(name = "数量")
    private Integer quantity;

    /** 小计（单价+加价）×数量 */
    @Excel(name = "小计")
    private BigDecimal subtotal;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getProductImage()
    {
        return productImage;
    }

    public void setProductImage(String productImage)
    {
        this.productImage = productImage;
    }

    public BigDecimal getUnitPrice()
    {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice)
    {
        this.unitPrice = unitPrice;
    }

    public String getFlavorText()
    {
        return flavorText;
    }

    public void setFlavorText(String flavorText)
    {
        this.flavorText = flavorText;
    }

    public BigDecimal getFlavorAmount()
    {
        return flavorAmount;
    }

    public void setFlavorAmount(BigDecimal flavorAmount)
    {
        this.flavorAmount = flavorAmount;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal()
    {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal)
    {
        this.subtotal = subtotal;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}

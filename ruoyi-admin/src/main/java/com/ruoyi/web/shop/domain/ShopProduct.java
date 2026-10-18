package com.ruoyi.web.shop.domain;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品对象 shop_product
 *
 * @author ruoyi
 */
public class ShopProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long productId;

    /** 所属分类 */
    @Excel(name = "分类ID")
    private Long categoryId;

    /** 分类名称（联表查询，非表字段） */
    @Excel(name = "所属分类")
    private String categoryName;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String productName;

    /** 商品图片 */
    private String productImage;

    /** 售价（元） */
    @Excel(name = "售价")
    private BigDecimal price;

    /** 商品描述 */
    @Excel(name = "描述")
    private String description;

    /** 显示顺序 */
    private Integer orderNum;

    /** 是否热销（0否 1是） */
    @Excel(name = "热销", readConverterExp = "0=否,1=是")
    private String isHot;

    /** 是否可售（0售罄/下架 1可售） */
    @Excel(name = "可售", readConverterExp = "0=售罄,1=可售")
    private String isAvailable;

    /** 状态（0正常 1停用） */
    private String status;

    /** 删除标志 */
    private String delFlag;

    /** 口味规格列表（非表字段，由 shop_product_flavor 聚合） */
    private List<ShopProductFlavor> flavors;

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
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

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public String getIsHot()
    {
        return isHot;
    }

    public void setIsHot(String isHot)
    {
        this.isHot = isHot;
    }

    public String getIsAvailable()
    {
        return isAvailable;
    }

    public void setIsAvailable(String isAvailable)
    {
        this.isAvailable = isAvailable;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public List<ShopProductFlavor> getFlavors()
    {
        return flavors;
    }

    public void setFlavors(List<ShopProductFlavor> flavors)
    {
        this.flavors = flavors;
    }
}

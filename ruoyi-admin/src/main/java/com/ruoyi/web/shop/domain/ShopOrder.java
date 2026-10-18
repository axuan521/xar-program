package com.ruoyi.web.shop.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 点单订单对象 shop_order
 * <p>
 * 取餐码规则：pickup_date + pickup_no 联合唯一，每日从 1 开始自增，
 * 现场叫号只需报 3 位数字（如"A017"），不会与历史订单冲突。
 * <p>
 * 订单状态机：0待付款 → 1待制作 → 2制作中 → 3待取餐 → 4已完成
 *                                            ↘ 5已取消（任意待制作前状态可取消）
 * <p>
 * 支付方式：1到店付款（默认，个人主体合规）/ 2微信支付（商户号启用后自动切换）
 *
 * @author ruoyi
 */
public class ShopOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 订单编号（业务流水号，如 SH202610170001） */
    @Excel(name = "订单编号")
    private String orderNo;

    /** 下单顾客ID */
    private Long wxUserId;

    /** 下单顾客openid（冗余，便于按 openid 查询） */
    private String openid;

    /** 订单总额（元） */
    @Excel(name = "订单总额")
    private BigDecimal totalAmount;

    /** 支付方式（1到店付款 2微信支付） */
    @Excel(name = "支付方式", readConverterExp = "1=到店付款,2=微信支付")
    private String payType;

    /** 支付状态（0未支付 1已支付 2已退款） */
    @Excel(name = "支付状态", readConverterExp = "0=未支付,1=已支付,2=已退款")
    private String payStatus;

    /** 支付流水号（微信支付回填，到店付可为空） */
    private String payNo;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /** 订单状态（0待付款 1待制作 2制作中 3待取餐 4已完成 5已取消） */
    @Excel(name = "订单状态", readConverterExp = "0=待付款,1=待制作,2=制作中,3=待取餐,4=已完成,5=已取消")
    private String orderStatus;

    /** 取餐日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pickupDate;

    /** 当日取餐序号（1开始自增） */
    private Integer pickupNo;

    /** 取餐码（冗余存储，便于查询展示，格式如 A017） */
    @Excel(name = "取餐码")
    private String pickupCode;

    /** 核销/收款确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date verifyTime;

    /** 核销操作人 */
    private String verifyBy;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;

    /** 顾客备注（少辣、打包等） */
    private String customerRemark;

    /** 取消原因 */
    private String cancelReason;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 订单明细列表（非表字段，由 shop_order_item 聚合） */
    private List<ShopOrderItem> items;

    /** 顾客昵称（联表查询，非表字段） */
    @Excel(name = "顾客昵称")
    private String nickName;

    /** 顾客手机号（联表查询，非表字段） */
    private String phone;

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public Long getWxUserId()
    {
        return wxUserId;
    }

    public void setWxUserId(Long wxUserId)
    {
        this.wxUserId = wxUserId;
    }

    public String getOpenid()
    {
        return openid;
    }

    public void setOpenid(String openid)
    {
        this.openid = openid;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public String getPayType()
    {
        return payType;
    }

    public void setPayType(String payType)
    {
        this.payType = payType;
    }

    public String getPayStatus()
    {
        return payStatus;
    }

    public void setPayStatus(String payStatus)
    {
        this.payStatus = payStatus;
    }

    public String getPayNo()
    {
        return payNo;
    }

    public void setPayNo(String payNo)
    {
        this.payNo = payNo;
    }

    public Date getPayTime()
    {
        return payTime;
    }

    public void setPayTime(Date payTime)
    {
        this.payTime = payTime;
    }

    public String getOrderStatus()
    {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus)
    {
        this.orderStatus = orderStatus;
    }

    public LocalDate getPickupDate()
    {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate)
    {
        this.pickupDate = pickupDate;
    }

    public Integer getPickupNo()
    {
        return pickupNo;
    }

    public void setPickupNo(Integer pickupNo)
    {
        this.pickupNo = pickupNo;
    }

    public String getPickupCode()
    {
        return pickupCode;
    }

    public void setPickupCode(String pickupCode)
    {
        this.pickupCode = pickupCode;
    }

    public Date getVerifyTime()
    {
        return verifyTime;
    }

    public void setVerifyTime(Date verifyTime)
    {
        this.verifyTime = verifyTime;
    }

    public String getVerifyBy()
    {
        return verifyBy;
    }

    public void setVerifyBy(String verifyBy)
    {
        this.verifyBy = verifyBy;
    }

    public Date getFinishTime()
    {
        return finishTime;
    }

    public void setFinishTime(Date finishTime)
    {
        this.finishTime = finishTime;
    }

    public String getCustomerRemark()
    {
        return customerRemark;
    }

    public void setCustomerRemark(String customerRemark)
    {
        this.customerRemark = customerRemark;
    }

    public String getCancelReason()
    {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason)
    {
        this.cancelReason = cancelReason;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public List<ShopOrderItem> getItems()
    {
        return items;
    }

    public void setItems(List<ShopOrderItem> items)
    {
        this.items = items;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }
}

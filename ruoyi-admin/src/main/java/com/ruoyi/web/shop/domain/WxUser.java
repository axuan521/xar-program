package com.ruoyi.web.shop.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;

/**
 * 小程序顾客对象 wx_user
 * <p>
 * 与后台 sys_user 无关，顾客仅凭 openid 标识身份。
 * 个人主体小程序无法调用 getPhoneNumber，phone 字段仅作预留。
 *
 * @author ruoyi
 */
public class WxUser
{
    /** 顾客ID */
    private Long wxUserId;

    /** 微信openid */
    @Excel(name = "OpenID")
    private String openid;

    /** 微信昵称 */
    @Excel(name = "昵称")
    private String nickName;

    /** 头像地址 */
    private String avatarUrl;

    /** 手机号（个人主体取不到，预留） */
    @Excel(name = "手机号")
    private String phone;

    /** 累计下单数 */
    @Excel(name = "累计下单数")
    private Integer orderCount;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后登录", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "首次进入", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;

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

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getAvatarUrl()
    {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl)
    {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public Integer getOrderCount()
    {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount)
    {
        this.orderCount = orderCount;
    }

    public Date getLastLoginTime()
    {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime)
    {
        this.lastLoginTime = lastLoginTime;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }
}

package com.ruoyi.web.shop.mapper;

import java.util.List;
import com.ruoyi.web.shop.domain.WxUser;

/**
 * 小程序顾客 数据层
 *
 * @author ruoyi
 */
public interface WxUserMapper
{
    /**
     * 查询顾客列表
     */
    public List<WxUser> selectWxUserList(WxUser wxUser);

    /**
     * 按 openid 查询顾客（登录时判断是否首次进入）
     */
    public WxUser selectWxUserByOpenid(String openid);

    /**
     * 按 ID 查询顾客
     */
    public WxUser selectWxUserById(Long wxUserId);

    /**
     * 新增顾客
     */
    public int insertWxUser(WxUser wxUser);

    /**
     * 修改顾客
     */
    public int updateWxUser(WxUser wxUser);

    /**
     * 更新最后登录时间
     */
    public int updateLastLoginTime(Long wxUserId);

    /**
     * 累计下单数 +1
     */
    public int increaseOrderCount(Long wxUserId);

    /**
     * 批量删除顾客（逻辑删除）
     */
    public int deleteWxUserByIds(Long[] wxUserIds);
}

package com.ruoyi.web.shop.service;

import java.util.List;
import com.ruoyi.web.shop.domain.WxUser;

/**
 * 小程序顾客 服务层
 *
 * @author ruoyi
 */
public interface IWxUserService
{
    /**
     * 查询顾客列表
     *
     * @param wxUser 顾客信息
     * @return 顾客集合
     */
    public List<WxUser> selectWxUserList(WxUser wxUser);

    /**
     * 查询顾客详情
     *
     * @param wxUserId 顾客ID
     * @return 顾客信息
     */
    public WxUser selectWxUserById(Long wxUserId);

    /**
     * 修改顾客状态（启用/停用）
     *
     * @param wxUserId 顾客ID
     * @param status 状态（0正常 1停用）
     * @return 结果
     */
    public int updateWxUserStatus(Long wxUserId, String status);

    /**
     * 修改顾客信息
     *
     * @param wxUser 顾客信息
     * @return 结果
     */
    public int updateWxUser(WxUser wxUser);

    /**
     * 批量删除顾客
     *
     * @param wxUserIds 需要删除的顾客ID
     * @return 结果
     */
    public int deleteWxUserByIds(Long[] wxUserIds);
}

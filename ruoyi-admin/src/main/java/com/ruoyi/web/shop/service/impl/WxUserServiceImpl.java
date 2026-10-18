package com.ruoyi.web.shop.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.web.shop.domain.WxUser;
import com.ruoyi.web.shop.mapper.WxUserMapper;
import com.ruoyi.web.shop.service.IWxUserService;

/**
 * 小程序顾客 服务层实现
 *
 * @author ruoyi
 */
@Service
public class WxUserServiceImpl implements IWxUserService
{
    @Autowired
    private WxUserMapper wxUserMapper;

    /**
     * 查询顾客列表
     *
     * @param wxUser 顾客信息
     * @return 顾客集合
     */
    @Override
    public List<WxUser> selectWxUserList(WxUser wxUser)
    {
        return wxUserMapper.selectWxUserList(wxUser);
    }

    /**
     * 查询顾客详情
     *
     * @param wxUserId 顾客ID
     * @return 顾客信息
     */
    @Override
    public WxUser selectWxUserById(Long wxUserId)
    {
        return wxUserMapper.selectWxUserById(wxUserId);
    }

    /**
     * 修改顾客状态（启用/停用）
     *
     * @param wxUserId 顾客ID
     * @param status 状态（0正常 1停用）
     * @return 结果
     */
    @Override
    public int updateWxUserStatus(Long wxUserId, String status)
    {
        WxUser wxUser = new WxUser();
        wxUser.setWxUserId(wxUserId);
        wxUser.setStatus(status);
        return wxUserMapper.updateWxUser(wxUser);
    }

    /**
     * 修改顾客信息
     *
     * @param wxUser 顾客信息
     * @return 结果
     */
    @Override
    public int updateWxUser(WxUser wxUser)
    {
        return wxUserMapper.updateWxUser(wxUser);
    }

    /**
     * 批量删除顾客
     *
     * @param wxUserIds 需要删除的顾客ID
     * @return 结果
     */
    @Override
    public int deleteWxUserByIds(Long[] wxUserIds)
    {
        return wxUserMapper.deleteWxUserByIds(wxUserIds);
    }
}

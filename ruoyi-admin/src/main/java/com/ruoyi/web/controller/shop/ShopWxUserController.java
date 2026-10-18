package com.ruoyi.web.controller.shop;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.web.shop.domain.WxUser;
import com.ruoyi.web.shop.service.IWxUserService;

/**
 * 小程序顾客管理Controller
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/shop/wxuser")
public class ShopWxUserController extends BaseController
{
    @Autowired
    private IWxUserService wxUserService;

    /**
     * 查询小程序顾客列表
     */
    @PreAuthorize("@ss.hasPermi('shop:wxuser:list')")
    @GetMapping("/list")
    public TableDataInfo list(WxUser wxUser)
    {
        startPage();
        List<WxUser> list = wxUserService.selectWxUserList(wxUser);
        return getDataTable(list);
    }

    /**
     * 导出小程序顾客列表
     */
    @PreAuthorize("@ss.hasPermi('shop:wxuser:export')")
    @Log(title = "小程序顾客", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WxUser wxUser)
    {
        List<WxUser> list = wxUserService.selectWxUserList(wxUser);
        ExcelUtil<WxUser> util = new ExcelUtil<WxUser>(WxUser.class);
        util.exportExcel(response, list, "小程序顾客数据");
    }

    /**
     * 获取小程序顾客详细信息
     */
    @PreAuthorize("@ss.hasPermi('shop:wxuser:query')")
    @GetMapping(value = "/{wxUserId}")
    public AjaxResult getInfo(@PathVariable Long wxUserId)
    {
        return AjaxResult.success(wxUserService.selectWxUserById(wxUserId));
    }

    /**
     * 修改小程序顾客状态
     */
    @PreAuthorize("@ss.hasPermi('shop:wxuser:edit')")
    @Log(title = "小程序顾客", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody WxUser wxUser)
    {
        return toAjax(wxUserService.updateWxUserStatus(wxUser.getWxUserId(), wxUser.getStatus()));
    }

    /**
     * 修改小程序顾客信息
     */
    @PreAuthorize("@ss.hasPermi('shop:wxuser:edit')")
    @Log(title = "小程序顾客", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WxUser wxUser)
    {
        return toAjax(wxUserService.updateWxUser(wxUser));
    }

    /**
     * 删除小程序顾客
     */
    @PreAuthorize("@ss.hasPermi('shop:wxuser:remove')")
    @Log(title = "小程序顾客", businessType = BusinessType.DELETE)
    @DeleteMapping("/{wxUserIds}")
    public AjaxResult remove(@PathVariable Long[] wxUserIds)
    {
        return toAjax(wxUserService.deleteWxUserByIds(wxUserIds));
    }
}

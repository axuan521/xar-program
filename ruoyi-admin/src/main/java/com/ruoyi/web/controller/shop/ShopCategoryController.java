package com.ruoyi.web.controller.shop;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.web.shop.domain.ShopCategory;
import com.ruoyi.web.shop.service.IShopCategoryService;

/**
 * 商品分类管理（Web 后台）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/shop/category")
public class ShopCategoryController extends BaseController
{
    private final IShopCategoryService categoryService;

    public ShopCategoryController(IShopCategoryService categoryService)
    {
        this.categoryService = categoryService;
    }

    @PreAuthorize("@ss.hasPermi('shop:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(ShopCategory shopCategory)
    {
        startPage();
        List<ShopCategory> list = categoryService.selectShopCategoryList(shopCategory);
        return getDataTable(list);
    }

    /**
     * 查询全部分类（商品表单里的下拉框用，不分页）
     */
    @PreAuthorize("@ss.hasPermi('shop:category:list')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        return success(categoryService.selectShopCategoryList(new ShopCategory()));
    }

    @PreAuthorize("@ss.hasPermi('shop:category:query')")
    @GetMapping("/{categoryId}")
    public AjaxResult getInfo(@PathVariable Long categoryId)
    {
        return success(categoryService.selectShopCategoryById(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('shop:category:add')")
    @Log(title = "商品分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ShopCategory shopCategory)
    {
        shopCategory.setCreateBy(SecurityUtils.getUsername());
        return toAjax(categoryService.insertShopCategory(shopCategory));
    }

    @PreAuthorize("@ss.hasPermi('shop:category:edit')")
    @Log(title = "商品分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ShopCategory shopCategory)
    {
        shopCategory.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(categoryService.updateShopCategory(shopCategory));
    }

    @PreAuthorize("@ss.hasPermi('shop:category:remove')")
    @Log(title = "商品分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(categoryService.deleteShopCategoryByIds(categoryIds));
    }
}

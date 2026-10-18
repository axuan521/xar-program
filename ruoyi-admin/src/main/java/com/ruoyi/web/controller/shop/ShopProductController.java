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
import com.ruoyi.web.shop.domain.ShopProduct;
import com.ruoyi.web.shop.service.IShopProductService;

/**
 * 商品管理（Web 后台）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/shop/product")
public class ShopProductController extends BaseController
{
    private final IShopProductService productService;

    public ShopProductController(IShopProductService productService)
    {
        this.productService = productService;
    }

    @PreAuthorize("@ss.hasPermi('shop:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(ShopProduct shopProduct)
    {
        startPage();
        List<ShopProduct> list = productService.selectShopProductList(shopProduct);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('shop:product:query')")
    @GetMapping("/{productId}")
    public AjaxResult getInfo(@PathVariable Long productId)
    {
        return success(productService.selectShopProductById(productId));
    }

    @PreAuthorize("@ss.hasPermi('shop:product:add')")
    @Log(title = "商品管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ShopProduct shopProduct)
    {
        shopProduct.setCreateBy(SecurityUtils.getUsername());
        return toAjax(productService.insertShopProduct(shopProduct));
    }

    @PreAuthorize("@ss.hasPermi('shop:product:edit')")
    @Log(title = "商品管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ShopProduct shopProduct)
    {
        shopProduct.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(productService.updateShopProduct(shopProduct));
    }

    @PreAuthorize("@ss.hasPermi('shop:product:remove')")
    @Log(title = "商品管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        return toAjax(productService.deleteShopProductByIds(productIds));
    }
}

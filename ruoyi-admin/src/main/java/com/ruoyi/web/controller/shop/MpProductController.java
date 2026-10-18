package com.ruoyi.web.controller.shop;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.web.shop.domain.ShopProduct;
import com.ruoyi.web.shop.service.IShopCategoryService;
import com.ruoyi.web.shop.service.IShopProductService;

/**
 * 小程序顾客端 - 商品与分类只读接口
 * <p>
 * 全部 {@code @Anonymous}：菜单属于公开数据，未登录的顾客进入小程序
 * 也能浏览商品（下单时再强制登录），避免把登录门槛前移到第一步
 * 影响转化率。
 * <p>
 * 接口路径统一用 /mp/ 前缀，与若依后台 /api 的 baseURL 隔离。
 *
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/mp/product")
public class MpProductController
{
    private final IShopCategoryService categoryService;

    private final IShopProductService productService;

    public MpProductController(IShopCategoryService categoryService, IShopProductService productService)
    {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    /**
     * 查询全部分类（仅正常状态）
     */
    @GetMapping("/category/list")
    public AjaxResult categoryList()
    {
        return AjaxResult.success(categoryService.selectShopCategoryAll());
    }

    /**
     * 查询可售商品列表
     *
     * @param categoryId 分类ID（可选，传则按分类过滤）
     */
    @GetMapping("/list")
    public AjaxResult productList(@RequestParam(required = false) Long categoryId)
    {
        return AjaxResult.success(productService.selectMenuProducts(categoryId));
    }

    /**
     * 查询商品详情（含口味规格）
     */
    @GetMapping("/{productId}")
    public AjaxResult detail(@PathVariable Long productId)
    {
        ShopProduct product = productService.selectShopProductById(productId);
        if (product == null || !"0".equals(product.getStatus()))
        {
            return AjaxResult.error("商品不存在或已下架");
        }
        return AjaxResult.success(product);
    }
}

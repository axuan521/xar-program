package com.ruoyi.web.shop.service;

import java.util.List;
import com.ruoyi.web.shop.domain.ShopCategory;

/**
 * 商品分类 服务层
 *
 * @author ruoyi
 */
public interface IShopCategoryService
{
    public List<ShopCategory> selectShopCategoryList(ShopCategory shopCategory);

    /** 小程序菜单用：仅正常状态分类 */
    public List<ShopCategory> selectShopCategoryAll();

    public ShopCategory selectShopCategoryById(Long categoryId);

    public int insertShopCategory(ShopCategory shopCategory);

    public int updateShopCategory(ShopCategory shopCategory);

    /**
     * 删除分类。分类下仍有商品时拒绝删除，避免商品变成孤儿数据。
     */
    public int deleteShopCategoryByIds(Long[] categoryIds);
}

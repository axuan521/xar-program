package com.ruoyi.web.shop.mapper;

import java.util.List;
import com.ruoyi.web.shop.domain.ShopCategory;

/**
 * 商品分类 数据层
 *
 * @author ruoyi
 */
public interface ShopCategoryMapper
{
    /**
     * 查询分类列表
     */
    public List<ShopCategory> selectShopCategoryList(ShopCategory shopCategory);

    /**
     * 查询所有正常状态的分类（小程序菜单用，含商品数统计）
     */
    public List<ShopCategory> selectShopCategoryAll();

    /**
     * 按ID查询分类
     */
    public ShopCategory selectShopCategoryById(Long categoryId);

    /**
     * 新增分类
     */
    public int insertShopCategory(ShopCategory shopCategory);

    /**
     * 修改分类
     */
    public int updateShopCategory(ShopCategory shopCategory);

    /**
     * 删除分类（逻辑删除）
     */
    public int deleteShopCategoryById(Long categoryId);

    /**
     * 批量删除分类（逻辑删除）
     */
    public int deleteShopCategoryByIds(Long[] categoryIds);

    /**
     * 校验分类名称是否唯一
     */
    public ShopCategory checkCategoryNameUnique(String categoryName);
}

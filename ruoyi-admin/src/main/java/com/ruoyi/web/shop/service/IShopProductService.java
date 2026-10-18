package com.ruoyi.web.shop.service;

import java.util.List;
import com.ruoyi.web.shop.domain.ShopProduct;

/**
 * 商品 服务层
 *
 * @author ruoyi
 */
public interface IShopProductService
{
    public List<ShopProduct> selectShopProductList(ShopProduct shopProduct);

    /** 小程序菜单用：仅正常可售商品 */
    public List<ShopProduct> selectShopProductAll();

    /**
     * 小程序菜单用：按分类查询可售商品，并一次性带出口味规格
     *
     * @param categoryId 分类ID，为空则查询全部
     */
    public List<ShopProduct> selectMenuProducts(Long categoryId);

    /** 按ID查询商品，含口味规格列表 */
    public ShopProduct selectShopProductById(Long productId);

    /**
     * 新增商品，同时保存其口味规格
     */
    public int insertShopProduct(ShopProduct shopProduct);

    /**
     * 修改商品，口味规格采用「先清后插」策略
     */
    public int updateShopProduct(ShopProduct shopProduct);

    public int deleteShopProductByIds(Long[] productIds);
}

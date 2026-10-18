package com.ruoyi.web.shop.mapper;

import java.util.List;
import com.ruoyi.web.shop.domain.ShopProduct;

/**
 * 商品 数据层
 *
 * @author ruoyi
 */
public interface ShopProductMapper
{
    /**
     * 查询商品列表（后台，含分类名）
     */
    public List<ShopProduct> selectShopProductList(ShopProduct shopProduct);

    /**
     * 查询所有可售商品（小程序菜单用，按分类与顺序排列）
     */
    public List<ShopProduct> selectShopProductAll();

    /**
     * 按ID查询商品
     */
    public ShopProduct selectShopProductById(Long productId);

    /**
     * 按ID批量查询商品（下单时校验价格与可售状态，防止前端篡改）
     */
    public List<ShopProduct> selectShopProductByIds(Long[] productIds);

    /**
     * 新增商品
     */
    public int insertShopProduct(ShopProduct shopProduct);

    /**
     * 修改商品
     */
    public int updateShopProduct(ShopProduct shopProduct);

    /**
     * 删除商品（逻辑删除）
     */
    public int deleteShopProductById(Long productId);

    /**
     * 批量删除商品（逻辑删除）
     */
    public int deleteShopProductByIds(Long[] productIds);

    /**
     * 统计某分类下的商品数（删除分类前校验）
     */
    public int countProductByCategoryId(Long categoryId);
}

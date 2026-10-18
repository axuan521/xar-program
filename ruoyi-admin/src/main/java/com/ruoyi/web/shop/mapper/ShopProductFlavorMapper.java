package com.ruoyi.web.shop.mapper;

import java.util.List;
import com.ruoyi.web.shop.domain.ShopProductFlavor;

/**
 * 商品口味规格 数据层
 *
 * @author ruoyi
 */
public interface ShopProductFlavorMapper
{
    /**
     * 查询某商品的全部口味（按组顺序、组内顺序排列）
     */
    public List<ShopProductFlavor> selectFlavorByProductId(Long productId);

    /**
     * 按ID查询口味（下单时校验加价金额，防止前端篡改）
     */
    public ShopProductFlavor selectFlavorById(Long flavorId);

    /**
     * 按ID批量查询口味（下单校验用）
     */
    public List<ShopProductFlavor> selectFlavorByIds(Long[] flavorIds);

    /**
     * 按商品ID批量查询口味（小程序菜单一次带出所有口味，避免 N+1 查询）
     */
    public List<ShopProductFlavor> selectFlavorByProductIds(Long[] productIds);

    /**
     * 批量新增口味
     */
    public int batchInsertFlavor(List<ShopProductFlavor> flavorList);

    /**
     * 修改口味
     */
    public int updateFlavor(ShopProductFlavor flavor);

    /**
     * 删除某商品的全部口味（商品保存时先清后插）
     */
    public int deleteFlavorByProductId(Long productId);

    /**
     * 按ID删除口味
     */
    public int deleteFlavorById(Long flavorId);
}

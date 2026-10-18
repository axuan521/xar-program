package com.ruoyi.web.shop.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.web.shop.domain.ShopProduct;
import com.ruoyi.web.shop.domain.ShopProductFlavor;
import com.ruoyi.web.shop.mapper.ShopProductFlavorMapper;
import com.ruoyi.web.shop.mapper.ShopProductMapper;
import com.ruoyi.web.shop.service.IShopProductService;

/**
 * 商品 服务实现
 *
 * @author ruoyi
 */
@Service
public class ShopProductServiceImpl implements IShopProductService
{
    private final ShopProductMapper productMapper;

    private final ShopProductFlavorMapper flavorMapper;

    public ShopProductServiceImpl(ShopProductMapper productMapper, ShopProductFlavorMapper flavorMapper)
    {
        this.productMapper = productMapper;
        this.flavorMapper = flavorMapper;
    }

    @Override
    public List<ShopProduct> selectShopProductList(ShopProduct shopProduct)
    {
        return productMapper.selectShopProductList(shopProduct);
    }

    @Override
    public List<ShopProduct> selectShopProductAll()
    {
        return productMapper.selectShopProductAll();
    }

    @Override
    public List<ShopProduct> selectMenuProducts(Long categoryId)
    {
        ShopProduct query = new ShopProduct();
        query.setCategoryId(categoryId);
        // 只取上架且可售的商品，售罄/下架由数据库过滤，避免把无效数据传到小程序
        query.setStatus("0");
        query.setIsAvailable("1");
        List<ShopProduct> list = productMapper.selectShopProductList(query);
        if (list.isEmpty())
        {
            return list;
        }
        // 一次查出所有口味再分组回填，避免逐个商品查询
        Long[] productIds = list.stream().map(ShopProduct::getProductId).toArray(Long[]::new);
        Map<Long, List<ShopProductFlavor>> flavorMap = new HashMap<>();
        for (ShopProductFlavor flavor : flavorMapper.selectFlavorByProductIds(productIds))
        {
            flavorMap.computeIfAbsent(flavor.getProductId(), k -> new ArrayList<>()).add(flavor);
        }
        for (ShopProduct product : list)
        {
            product.setFlavors(flavorMap.getOrDefault(product.getProductId(), new ArrayList<>()));
        }
        return list;
    }

    @Override
    public ShopProduct selectShopProductById(Long productId)
    {
        ShopProduct product = productMapper.selectShopProductById(productId);
        if (product != null)
        {
            product.setFlavors(flavorMapper.selectFlavorByProductId(productId));
        }
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertShopProduct(ShopProduct shopProduct)
    {
        validate(shopProduct);
        int rows = productMapper.insertShopProduct(shopProduct);
        // insert 后 productId 由 useGeneratedKeys 回填，据此写入其口味
        saveFlavors(shopProduct.getProductId(), shopProduct.getFlavors());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateShopProduct(ShopProduct shopProduct)
    {
        validate(shopProduct);
        if (productMapper.selectShopProductById(shopProduct.getProductId()) == null)
        {
            throw new ServiceException("商品不存在或已删除");
        }
        int rows = productMapper.updateShopProduct(shopProduct);
        // 口味采用先清后插，避免前端增删选项后出现残留脏数据
        flavorMapper.deleteFlavorByProductId(shopProduct.getProductId());
        saveFlavors(shopProduct.getProductId(), shopProduct.getFlavors());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteShopProductByIds(Long[] productIds)
    {
        for (Long productId : productIds)
        {
            flavorMapper.deleteFlavorByProductId(productId);
        }
        return productMapper.deleteShopProductByIds(productIds);
    }

    /**
     * 保存商品口味，并按组规范化 is_default（每组只允许一个默认项）
     */
    private void saveFlavors(Long productId, List<ShopProductFlavor> flavors)
    {
        if (flavors == null || flavors.isEmpty())
        {
            return;
        }
        List<ShopProductFlavor> rows = new ArrayList<>(flavors.size());
        java.util.Set<String> defaultGroups = new java.util.HashSet<>();
        for (ShopProductFlavor flavor : flavors)
        {
            if (StringUtils.isEmpty(flavor.getGroupName()) || StringUtils.isEmpty(flavor.getFlavorName()))
            {
                throw new ServiceException("口味的组名与选项名不能为空");
            }
            flavor.setProductId(productId);
            if (flavor.getExtraPrice() == null)
            {
                flavor.setExtraPrice(BigDecimal.ZERO);
            }
            if (flavor.getExtraPrice().compareTo(BigDecimal.ZERO) < 0)
            {
                throw new ServiceException("口味加价不能为负数");
            }
            if (flavor.getOrderNum() == null)
            {
                flavor.setOrderNum(0);
            }
            if (flavor.getGroupOrderNum() == null)
            {
                flavor.setGroupOrderNum(0);
            }
            if (StringUtils.isEmpty(flavor.getStatus()))
            {
                flavor.setStatus("0");
            }
            // 同组已有默认项时，后续项强制取消默认，保证每组唯一
            if ("1".equals(flavor.getIsDefault()))
            {
                if (!defaultGroups.add(flavor.getGroupName()))
                {
                    flavor.setIsDefault("0");
                }
            }
            else
            {
                flavor.setIsDefault("0");
            }
            rows.add(flavor);
        }
        // 分批插入，避免单条 SQL 过长
        for (int from = 0; from < rows.size(); from += 50)
        {
            flavorMapper.batchInsertFlavor(rows.subList(from, Math.min(from + 50, rows.size())));
        }
    }

    /**
     * 商品基础校验
     */
    private void validate(ShopProduct product)
    {
        if (StringUtils.isEmpty(product.getProductName()))
        {
            throw new ServiceException("商品名称不能为空");
        }
        if (product.getCategoryId() == null)
        {
            throw new ServiceException("请选择商品分类");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("商品售价必须大于 0");
        }
        if (product.getPrice().scale() > 2)
        {
            throw new ServiceException("售价最多保留两位小数");
        }
    }
}

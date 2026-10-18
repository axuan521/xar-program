package com.ruoyi.web.shop.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.web.shop.domain.ShopCategory;
import com.ruoyi.web.shop.mapper.ShopCategoryMapper;
import com.ruoyi.web.shop.mapper.ShopProductMapper;
import com.ruoyi.web.shop.service.IShopCategoryService;

/**
 * 商品分类 服务实现
 *
 * @author ruoyi
 */
@Service
public class ShopCategoryServiceImpl implements IShopCategoryService
{
    private final ShopCategoryMapper categoryMapper;

    private final ShopProductMapper productMapper;

    public ShopCategoryServiceImpl(ShopCategoryMapper categoryMapper, ShopProductMapper productMapper)
    {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<ShopCategory> selectShopCategoryList(ShopCategory shopCategory)
    {
        return categoryMapper.selectShopCategoryList(shopCategory);
    }

    @Override
    public List<ShopCategory> selectShopCategoryAll()
    {
        return categoryMapper.selectShopCategoryAll();
    }

    @Override
    public ShopCategory selectShopCategoryById(Long categoryId)
    {
        return categoryMapper.selectShopCategoryById(categoryId);
    }

    @Override
    public int insertShopCategory(ShopCategory shopCategory)
    {
        checkCategoryNameUnique(shopCategory);
        return categoryMapper.insertShopCategory(shopCategory);
    }

    @Override
    public int updateShopCategory(ShopCategory shopCategory)
    {
        checkCategoryNameUnique(shopCategory);
        return categoryMapper.updateShopCategory(shopCategory);
    }

    @Override
    public int deleteShopCategoryByIds(Long[] categoryIds)
    {
        // 分类下仍有商品时拒绝删除，否则商品会失去分类归属而无法在菜单展示
        for (Long categoryId : categoryIds)
        {
            int count = productMapper.countProductByCategoryId(categoryId);
            if (count > 0)
            {
                ShopCategory category = categoryMapper.selectShopCategoryById(categoryId);
                String name = category == null ? String.valueOf(categoryId) : category.getCategoryName();
                throw new ServiceException("分类「" + name + "」下仍有 " + count + " 个商品，请先移除或改分类");
            }
        }
        return categoryMapper.deleteShopCategoryByIds(categoryIds);
    }

    /**
     * 校验分类名称唯一
     */
    private void checkCategoryNameUnique(ShopCategory shopCategory)
    {
        if (StringUtils.isEmpty(shopCategory.getCategoryName()))
        {
            return;
        }
        ShopCategory exist = categoryMapper.checkCategoryNameUnique(shopCategory.getCategoryName());
        if (exist != null && !exist.getCategoryId().equals(shopCategory.getCategoryId()))
        {
            throw new ServiceException("分类名称「" + shopCategory.getCategoryName() + "」已存在");
        }
    }
}

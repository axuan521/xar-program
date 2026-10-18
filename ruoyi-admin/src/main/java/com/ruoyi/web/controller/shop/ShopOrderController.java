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
import com.ruoyi.web.shop.domain.ShopOrder;
import com.ruoyi.web.shop.service.IShopOrderService;

/**
 * 订单管理（Web 后台）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/shop/order")
public class ShopOrderController extends BaseController
{
    private final IShopOrderService orderService;

    public ShopOrderController(IShopOrderService orderService)
    {
        this.orderService = orderService;
    }

    @PreAuthorize("@ss.hasPermi('shop:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(ShopOrder shopOrder)
    {
        startPage();
        List<ShopOrder> list = orderService.selectShopOrderList(shopOrder);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('shop:order:query')")
    @GetMapping("/{orderId}")
    public AjaxResult getInfo(@PathVariable Long orderId)
    {
        return success(orderService.selectShopOrderById(orderId));
    }

    /**
     * 待处理订单列表（店员工作台也可调用）
     */
    @PreAuthorize("@ss.hasPermi('shop:order:list')")
    @GetMapping("/pending")
    public AjaxResult pending()
    {
        return success(orderService.selectPendingOrders());
    }

    /**
     * 店员按取餐码查单（扫码核销）
     * <p>
     * 与顾客端 /mp/order/find-by-pickup-code 的区别：本接口需后台登录态与
     * shop:order:list 权限，且允许查到「到店付款未收款」的订单，便于店员先收款再制作。
     */
    @PreAuthorize("@ss.hasPermi('shop:order:list')")
    @GetMapping("/pickup/{pickupCode}")
    public AjaxResult getByPickupCode(@PathVariable String pickupCode)
    {
        if (pickupCode == null || pickupCode.trim().isEmpty())
        {
            return error("取餐码不能为空");
        }
        ShopOrder order = orderService.selectShopOrderByPickupCode(pickupCode.trim().toUpperCase());
        if (order == null)
        {
            return error("未找到今日该取餐码对应的订单");
        }
        if ("4".equals(order.getOrderStatus()) || "5".equals(order.getOrderStatus()))
        {
            return error("该订单已完成或已取消");
        }
        return success(order);
    }

    /**
     * 店员核销订单（到店付款场景）
     */
    @PreAuthorize("@ss.hasPermi('shop:order:verify')")
    @Log(title = "订单核销", businessType = BusinessType.UPDATE)
    @PostMapping("/verify/{orderId}")
    public AjaxResult verify(@PathVariable Long orderId)
    {
        orderService.verifyOrder(orderId, SecurityUtils.getUsername());
        return success();
    }

    /**
     * 更新订单状态（制作中/待取餐）
     */
    @PreAuthorize("@ss.hasPermi('shop:order:status')")
    @Log(title = "订单状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult updateStatus(@RequestBody ShopOrder shopOrder)
    {
        return toAjax(orderService.updateOrderStatus(shopOrder.getOrderId(), shopOrder.getOrderStatus()));
    }

    /**
     * 取消订单
     */
    @PreAuthorize("@ss.hasPermi('shop:order:cancel')")
    @Log(title = "订单取消", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{orderId}")
    public AjaxResult cancel(@PathVariable Long orderId, @RequestBody ShopOrder shopOrder)
    {
        return toAjax(orderService.cancelOrder(orderId, shopOrder.getCancelReason()));
    }

    /**
     * 完成订单
     */
    @PreAuthorize("@ss.hasPermi('shop:order:status')")
    @Log(title = "订单完成", businessType = BusinessType.UPDATE)
    @PutMapping("/finish/{orderId}")
    public AjaxResult finish(@PathVariable Long orderId)
    {
        return toAjax(orderService.finishOrder(orderId));
    }

    @PreAuthorize("@ss.hasPermi('shop:order:remove')")
    @Log(title = "订单删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(orderService.deleteShopOrderByIds(orderIds));
    }
}

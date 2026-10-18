package com.ruoyi.web.shop.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.web.shop.domain.ShopOrder;
import com.ruoyi.web.shop.domain.ShopOrderItem;
import com.ruoyi.web.shop.domain.ShopProduct;
import com.ruoyi.web.shop.domain.ShopProductFlavor;
import com.ruoyi.web.shop.mapper.ShopOrderItemMapper;
import com.ruoyi.web.shop.mapper.ShopOrderMapper;
import com.ruoyi.web.shop.mapper.ShopProductFlavorMapper;
import com.ruoyi.web.shop.mapper.ShopProductMapper;
import com.ruoyi.web.shop.pay.PaymentRouter;
import com.ruoyi.web.shop.pay.PrepayResult;
import com.ruoyi.web.shop.pay.WxPayProperties;
import com.ruoyi.web.shop.service.IShopOrderService;

/**
 * 订单 服务实现
 *
 * @author ruoyi
 */
@Service
public class ShopOrderServiceImpl implements IShopOrderService
{
    private static final Logger log = LoggerFactory.getLogger(ShopOrderServiceImpl.class);

    /** 订单编号前缀 + 日期 + 6位流水 */
    private static final DateTimeFormatter ORDER_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 取餐码前缀 */
    private static final String PICKUP_CODE_PREFIX = "A";

    private final ShopOrderMapper orderMapper;

    private final ShopOrderItemMapper orderItemMapper;

    private final ShopProductMapper productMapper;

    private final ShopProductFlavorMapper flavorMapper;

    private final PaymentRouter paymentRouter;

    private final WxPayProperties wxPayProperties;

    public ShopOrderServiceImpl(ShopOrderMapper orderMapper, ShopOrderItemMapper orderItemMapper,
            ShopProductMapper productMapper, ShopProductFlavorMapper flavorMapper,
            PaymentRouter paymentRouter, WxPayProperties wxPayProperties)
    {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.flavorMapper = flavorMapper;
        this.paymentRouter = paymentRouter;
        this.wxPayProperties = wxPayProperties;
    }

    @Override
    public List<ShopOrder> selectShopOrderList(ShopOrder shopOrder)
    {
        List<ShopOrder> list = orderMapper.selectShopOrderList(shopOrder);
        for (ShopOrder order : list)
        {
            order.setItems(orderItemMapper.selectShopOrderItemByOrderId(order.getOrderId()));
        }
        return list;
    }

    @Override
    public ShopOrder selectShopOrderById(Long orderId)
    {
        ShopOrder order = orderMapper.selectShopOrderById(orderId);
        if (order != null)
        {
            order.setItems(orderItemMapper.selectShopOrderItemByOrderId(orderId));
        }
        return order;
    }

    @Override
    public List<ShopOrder> selectShopOrderByWxUserId(Long wxUserId, String orderStatus)
    {
        List<ShopOrder> list = orderMapper.selectShopOrderByWxUserId(wxUserId, orderStatus);
        for (ShopOrder order : list)
        {
            order.setItems(orderItemMapper.selectShopOrderItemByOrderId(order.getOrderId()));
        }
        return list;
    }

    @Override
    public List<ShopOrder> selectPendingOrders()
    {
        List<ShopOrder> list = orderMapper.selectPendingOrders();
        for (ShopOrder order : list)
        {
            order.setItems(orderItemMapper.selectShopOrderItemByOrderId(order.getOrderId()));
        }
        return list;
    }

    @Override
    public ShopOrder selectShopOrderByPickupCode(String pickupCode)
    {
        ShopOrder order = orderMapper.selectShopOrderByPickupCode(pickupCode);
        if (order != null)
        {
            order.setItems(orderItemMapper.selectShopOrderItemByOrderId(order.getOrderId()));
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopOrder createOrder(Long wxUserId, String openid, List<OrderItemRequest> items, String remark)
    {
        if (items == null || items.isEmpty())
        {
            throw new ServiceException("购物车不能为空");
        }

        // 1. 服务端重算金额：按 productId 批量取库内真实价格，按 flavorIds 累加加价
        List<Long> productIds = new ArrayList<>();
        List<Long> allFlavorIds = new ArrayList<>();
        for (OrderItemRequest req : items)
        {
            if (req.getProductId() == null)
            {
                throw new ServiceException("商品ID不能为空");
            }
            if (req.getQuantity() == null || req.getQuantity() <= 0)
            {
                throw new ServiceException("商品数量必须大于 0");
            }
            productIds.add(req.getProductId());
            if (req.getFlavorIds() != null)
            {
                allFlavorIds.addAll(req.getFlavorIds());
            }
        }

        Map<Long, ShopProduct> productMap = new HashMap<>();
        for (ShopProduct p : productMapper.selectShopProductByIds(productIds.toArray(new Long[0])))
        {
            productMap.put(p.getProductId(), p);
        }
        Map<Long, ShopProductFlavor> flavorMap = new HashMap<>();
        if (!allFlavorIds.isEmpty())
        {
            for (ShopProductFlavor f : flavorMapper.selectFlavorByIds(allFlavorIds.toArray(new Long[0])))
            {
                flavorMap.put(f.getFlavorId(), f);
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ShopOrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest req : items)
        {
            ShopProduct product = productMap.get(req.getProductId());
            if (product == null)
            {
                throw new ServiceException("商品不存在或已下架");
            }
            if ("0".equals(product.getIsAvailable()))
            {
                throw new ServiceException("「" + product.getProductName() + "」已售罄");
            }

            BigDecimal flavorAmount = BigDecimal.ZERO;
            StringBuilder flavorText = new StringBuilder();
            if (req.getFlavorIds() != null && !req.getFlavorIds().isEmpty())
            {
                for (Long flavorId : req.getFlavorIds())
                {
                    ShopProductFlavor flavor = flavorMap.get(flavorId);
                    if (flavor != null && flavor.getProductId().equals(req.getProductId()))
                    {
                        flavorAmount = flavorAmount.add(flavor.getExtraPrice());
                        if (flavorText.length() > 0)
                        {
                            flavorText.append("/");
                        }
                        flavorText.append(flavor.getFlavorName());
                        if (flavor.getExtraPrice().compareTo(BigDecimal.ZERO) > 0)
                        {
                            flavorText.append("(+").append(flavor.getExtraPrice().stripTrailingZeros().toPlainString()).append(")");
                        }
                    }
                }
            }

            BigDecimal unitPrice = product.getPrice().add(flavorAmount);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(req.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            ShopOrderItem item = new ShopOrderItem();
            item.setProductId(product.getProductId());
            item.setProductName(product.getProductName());
            item.setProductImage(product.getProductImage());
            item.setUnitPrice(unitPrice);
            item.setFlavorText(flavorText.toString());
            item.setFlavorAmount(flavorAmount);
            item.setQuantity(req.getQuantity());
            item.setSubtotal(subtotal);
            orderItems.add(item);

            totalAmount = totalAmount.add(subtotal);
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("订单金额必须大于 0");
        }

        // 2. 生成订单编号（SH + 日期 + 6位随机流水）
        String orderNo = "SH" + LocalDate.now().format(ORDER_DATE_FMT)
                + IdUtils.fastUUID().substring(0, 6).toUpperCase();

        // 3. 分配当日取餐码（A + 3位数字，每日从 1 自增）
        LocalDate pickupDate = LocalDate.now();
        Integer maxNo = orderMapper.selectMaxPickupNo(pickupDate);
        int nextNo = (maxNo == null ? 0 : maxNo) + 1;
        String pickupCode = PICKUP_CODE_PREFIX + String.format("%03d", nextNo);

        // 4. 确定支付通道。真正的预下单放到「去支付」时执行：
        //    顾客可能下单后并不立即付款，提前调微信下单会占用预支付单且增加失败面。
        String payType = paymentRouter.payType();

        // 5. 落库订单与明细
        ShopOrder order = new ShopOrder();
        order.setOrderNo(orderNo);
        order.setWxUserId(wxUserId);
        order.setOpenid(openid);
        order.setTotalAmount(totalAmount);
        order.setPayType(payType);
        order.setPayStatus("0");
        // 到店付款场景：订单创建后即为"待付款"，等店员收款时置为已支付
        // 微信支付场景：预下单后订单为"待付款"，回调成功后置为已支付
        order.setOrderStatus("0");
        order.setPickupDate(pickupDate);
        order.setPickupNo(nextNo);
        order.setPickupCode(pickupCode);
        order.setCustomerRemark(StringUtils.isEmpty(remark) ? "" : remark);
        order.setCreateBy(openid);
        orderMapper.insertShopOrder(order);

        for (ShopOrderItem item : orderItems)
        {
            item.setOrderId(order.getOrderId());
        }
        orderItemMapper.batchInsertShopOrderItem(orderItems);

        order.setItems(orderItems);
        log.info("订单创建成功 orderNo={} pickupCode={} amount={}", orderNo, pickupCode, totalAmount);
        return order;
    }

    @Override
    public PrepayResult preparePay(Long orderId, Long wxUserId)
    {
        ShopOrder order = selectShopOrderById(orderId);
        assertPayable(order, wxUserId);
        return paymentRouter.prepay(order.getOrderNo(), order.getTotalAmount(), buildSubject(order), order.getOpenid());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopOrder mockPay(Long orderId, Long wxUserId)
    {
        if (!wxPayProperties.isMockEnabled())
        {
            throw new ServiceException("模拟支付未开启");
        }
        ShopOrder order = selectShopOrderById(orderId);
        assertPayable(order, wxUserId);
        // 模拟流水号加 MOCK 前缀，便于后台区分真实支付与联调数据
        orderMapper.updatePayStatus(orderId, "1", "MOCK" + order.getOrderNo(), new Date());
        orderMapper.updateOrderStatus(orderId, "1");
        log.info("模拟支付成功 orderId={} orderNo={}", orderId, order.getOrderNo());
        return selectShopOrderById(orderId);
    }

    /**
     * 校验订单归属与可支付状态
     */
    private void assertPayable(ShopOrder order, Long wxUserId)
    {
        if (order == null || wxUserId == null || !wxUserId.equals(order.getWxUserId()))
        {
            throw new ServiceException("订单不存在");
        }
        if (!"0".equals(order.getPayStatus()))
        {
            throw new ServiceException("订单已支付，请勿重复支付");
        }
        if (!"0".equals(order.getOrderStatus()))
        {
            throw new ServiceException("订单当前状态不可支付");
        }
    }

    /**
     * 收银台展示的商品描述
     */
    private String buildSubject(ShopOrder order)
    {
        if (order.getItems() == null || order.getItems().isEmpty())
        {
            return "门店点单";
        }
        ShopOrderItem first = order.getItems().get(0);
        return order.getItems().size() == 1
                ? first.getProductName()
                : first.getProductName() + "等" + order.getItems().size() + "件";
    }

    @Override
    public int cancelOrder(Long orderId, String cancelReason)
    {
        return orderMapper.cancelOrder(orderId, cancelReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyOrder(Long orderId, String verifyBy)
    {
        ShopOrder order = orderMapper.selectShopOrderById(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }

        // 场景一：到店付款，店员确认收款 → 已支付 + 待制作
        if ("0".equals(order.getPayStatus()))
        {
            if (!"0".equals(order.getOrderStatus()) && !"1".equals(order.getOrderStatus()))
            {
                throw new ServiceException("当前订单状态不可收款");
            }
            orderMapper.updatePayStatus(orderId, "1", null, new Date());
            if ("0".equals(order.getOrderStatus()))
            {
                orderMapper.updateOrderStatus(orderId, "1");
            }
            orderMapper.verifyOrder(orderId, verifyBy);
            log.info("订单收款确认 orderId={} orderNo={} verifyBy={}", orderId, order.getOrderNo(), verifyBy);
            return;
        }

        // 场景二：已支付且已制作完成，核销取餐码 → 已完成
        if ("1".equals(order.getPayStatus()))
        {
            if (!"3".equals(order.getOrderStatus()))
            {
                throw new ServiceException("订单尚未制作完成，不能核销取餐");
            }
            orderMapper.verifyOrder(orderId, verifyBy);
            orderMapper.finishOrder(orderId);
            log.info("订单取餐核销 orderId={} orderNo={} verifyBy={}", orderId, order.getOrderNo(), verifyBy);
            return;
        }

        throw new ServiceException("订单支付状态异常，无法核销");
    }

    @Override
    public int updateOrderStatus(Long orderId, String orderStatus)
    {
        return orderMapper.updateOrderStatus(orderId, orderStatus);
    }

    @Override
    public int finishOrder(Long orderId)
    {
        return orderMapper.finishOrder(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteShopOrderByIds(Long[] orderIds)
    {
        for (Long orderId : orderIds)
        {
            orderItemMapper.deleteShopOrderItemByOrderId(orderId);
        }
        return orderMapper.deleteShopOrderByIds(orderIds);
    }
}

package com.ruoyi.web.controller.shop;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.web.shop.pay.PaymentRouter;
import com.ruoyi.web.shop.service.IShopOrderService;
import com.ruoyi.web.shop.service.IShopOrderService.OrderItemRequest;
import com.ruoyi.web.shop.wx.WxLoginUser;
import com.ruoyi.web.shop.wx.WxTokenService;

/**
 * 小程序顾客端 - 订单接口
 * <p>
 * 全部接口标注 {@code @Anonymous}，跳过后台 JWT 过滤器；
 * 需要登录的接口在方法内通过 X-Wx-Token 请求头自行校验。
 * <p>
 * 接口路径使用 /mp/ 前缀，避免与前端 .env.production 的 /api baseURL 冲突。
 *
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/mp/order")
public class MpOrderController
{
    private final IShopOrderService orderService;

    private final WxTokenService tokenService;

    private final PaymentRouter paymentRouter;

    public MpOrderController(IShopOrderService orderService, WxTokenService tokenService,
            PaymentRouter paymentRouter)
    {
        this.orderService = orderService;
        this.tokenService = tokenService;
        this.paymentRouter = paymentRouter;
    }

    /** 下单请求体 */
    public record CreateOrderRequest(List<OrderItemRequest> items, String remark)
    {
    }

    /**
     * 顾客下单
     */
    @PostMapping("/create")
    public AjaxResult create(@RequestHeader(value = WxTokenService.WX_TOKEN_HEADER, required = false) String token,
            @RequestBody CreateOrderRequest req)
    {
        WxLoginUser loginUser = tokenService.requireLoginUser(token);
        var order = orderService.createOrder(loginUser.getWxUserId(), loginUser.getOpenid(), req.items(), req.remark());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("orderId", order.getOrderId());
        ajax.put("orderNo", order.getOrderNo());
        ajax.put("pickupCode", order.getPickupCode());
        ajax.put("totalAmount", order.getTotalAmount());
        ajax.put("payType", order.getPayType());
        ajax.put("payStatus", order.getPayStatus());
        ajax.put("orderStatus", order.getOrderStatus());
        return ajax;
    }

    /**
     * 发起支付
     * <p>
     * 到店付款：payType=1，前端展示门店收款指引与取餐码；
     * 微信支付：payType=2，params 直接传给 wx.requestPayment 调起收银台。
     */
    @GetMapping("/pay/{orderId}")
    public AjaxResult pay(@RequestHeader(value = WxTokenService.WX_TOKEN_HEADER, required = false) String token,
            @PathVariable Long orderId)
    {
        WxLoginUser loginUser = tokenService.requireLoginUser(token);
        var prepay = orderService.preparePay(orderId, loginUser.getWxUserId());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("payType", prepay.getPayType());
        ajax.put("orderNo", prepay.getOrderNo());
        ajax.put("amountYuan", prepay.getAmountYuan());
        ajax.put("params", prepay.getParams());
        ajax.put("wxPayEnabled", paymentRouter.isWxPayEnabled());
        ajax.put("mockPayEnabled", paymentRouter.isMockPayEnabled());
        return ajax;
    }

    /**
     * 模拟支付（仅开发联调，需显式开启 wx.pay.mock-enabled）
     */
    @PostMapping("/mock-pay/{orderId}")
    public AjaxResult mockPay(@RequestHeader(value = WxTokenService.WX_TOKEN_HEADER, required = false) String token,
            @PathVariable Long orderId)
    {
        WxLoginUser loginUser = tokenService.requireLoginUser(token);
        return AjaxResult.success(orderService.mockPay(orderId, loginUser.getWxUserId()));
    }

    /**
     * 查询当前顾客的订单列表
     */
    @GetMapping("/my")
    public AjaxResult myOrders(@RequestHeader(value = WxTokenService.WX_TOKEN_HEADER, required = false) String token,
            @RequestParam(required = false) String orderStatus)
    {
        WxLoginUser loginUser = tokenService.requireLoginUser(token);
        return AjaxResult.success(orderService.selectShopOrderByWxUserId(loginUser.getWxUserId(), orderStatus));
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderId}")
    public AjaxResult detail(@RequestHeader(value = WxTokenService.WX_TOKEN_HEADER, required = false) String token,
            @PathVariable Long orderId)
    {
        WxLoginUser loginUser = tokenService.requireLoginUser(token);
        var order = orderService.selectShopOrderById(orderId);
        if (order == null || !order.getWxUserId().equals(loginUser.getWxUserId()))
        {
            return AjaxResult.error("订单不存在");
        }
        return AjaxResult.success(order);
    }

    /**
     * 取消订单（仅待付款/待制作状态可取消）
     */
    @PutMapping("/cancel/{orderId}")
    public AjaxResult cancel(@RequestHeader(value = WxTokenService.WX_TOKEN_HEADER, required = false) String token,
            @PathVariable Long orderId)
    {
        WxLoginUser loginUser = tokenService.requireLoginUser(token);
        var order = orderService.selectShopOrderById(orderId);
        if (order == null || !order.getWxUserId().equals(loginUser.getWxUserId()))
        {
            return AjaxResult.error("订单不存在");
        }
        return AjaxResult.success(orderService.cancelOrder(orderId, "顾客取消"));
    }

    /**
     * 查询支付方式是否支持微信支付（前端据此展示"在线支付/到店付款"）
     */
    @GetMapping("/payType")
    public AjaxResult payType()
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("wxPayEnabled", paymentRouter.isWxPayEnabled());
        ajax.put("mockPayEnabled", paymentRouter.isMockPayEnabled());
        return ajax;
    }

    /**
     * 按取餐码查询订单（店员扫码核销用）
     * <p>
     * 仅返回当日且处于「待制作/制作中/待取餐」状态的订单，
     * 防止店员扫到历史取餐码误核销。
     *
     * @param pickupCode 取餐码（如 A017）
     */
    @GetMapping("/find-by-pickup-code")
    public AjaxResult findByPickupCode(@RequestParam String pickupCode)
    {
        if (pickupCode == null || pickupCode.trim().isEmpty())
        {
            return AjaxResult.error("取餐码不能为空");
        }
        var order = orderService.selectShopOrderByPickupCode(pickupCode.trim());
        if (order == null)
        {
            return AjaxResult.error("未找到该取餐码对应的订单");
        }
        // 已支付且未完成/未取消的订单才可核销
        if (!"1".equals(order.getPayStatus()) || "4".equals(order.getOrderStatus()) || "5".equals(order.getOrderStatus()))
        {
            return AjaxResult.error("该订单当前状态不可核销");
        }
        return AjaxResult.success(order);
    }
}

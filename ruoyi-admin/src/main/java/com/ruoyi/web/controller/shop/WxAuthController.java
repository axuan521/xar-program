package com.ruoyi.web.controller.shop;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.web.shop.wx.WxAuthService;
import com.ruoyi.web.shop.wx.WxLoginUser;
import com.ruoyi.web.shop.wx.WxTokenService;

/**
 * 小程序顾客端 - 登录认证
 * <p>
 * 全部接口标注 {@code @Anonymous}，由若依 PermitAllUrlProperties 自动加入白名单，
 * 跳过后台 JWT 过滤器；需要登录的接口在方法内通过 X-Wx-Token 请求头自行校验，
 * 与后台认证体系完全隔离，不改动 SecurityConfig。
 * <p>
 * 店员端不走此控制器：店员有后台账号，直接复用若依 /login 拿 JWT，
 * 再调 /mp/staff/** 下的接口（由 @PreAuthorize 校验 shop:order:* 权限）。
 *
 * @author ruoyi
 */
@Anonymous
@RestController
@RequestMapping("/mp/auth")
public class WxAuthController
{
    private final WxAuthService wxAuthService;

    private final WxTokenService tokenService;

    public WxAuthController(WxAuthService wxAuthService, WxTokenService tokenService)
    {
        this.wxAuthService = wxAuthService;
        this.tokenService = tokenService;
    }

    /** 登录请求体 */
    public record LoginRequest(String code, String nickName, String avatarUrl)
    {
    }

    /**
     * 小程序登录
     * <p>
     * 前端 wx.login() 拿到 code 后调用本接口，返回 token 需存入 storage，
     * 后续请求通过 X-Wx-Token 请求头携带。
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginRequest req)
    {
        WxLoginUser loginUser = wxAuthService.login(req.code(), req.nickName(), req.avatarUrl());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("token", loginUser.getToken());
        ajax.put("wxUserId", loginUser.getWxUserId());
        ajax.put("nickName", loginUser.getNickName());
        ajax.put("avatarUrl", loginUser.getAvatarUrl());
        return ajax;
    }

    /**
     * 获取当前登录顾客信息
     */
    @PostMapping("/info")
    public AjaxResult info(@RequestHeader(value = WxTokenService.WX_TOKEN_HEADER, required = false) String token)
    {
        WxLoginUser loginUser = tokenService.requireLoginUser(token);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("wxUserId", loginUser.getWxUserId());
        ajax.put("nickName", loginUser.getNickName());
        ajax.put("avatarUrl", loginUser.getAvatarUrl());
        return ajax;
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public AjaxResult logout(@RequestHeader(value = WxTokenService.WX_TOKEN_HEADER, required = false) String token)
    {
        tokenService.removeToken(token);
        return AjaxResult.success();
    }
}

# -*- coding: utf-8 -*-
"""生成小程序所需的 tabBar 图标与占位图（纯 PIL 绘制，无外部依赖素材）。"""
import os
from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src")
TAB_DIR = os.path.join(BASE, "static", "tab")
STATIC_DIR = os.path.join(BASE, "static")
os.makedirs(TAB_DIR, exist_ok=True)

NORMAL = (153, 153, 153, 255)   # #999999
ACTIVE = (255, 107, 53, 255)    # #FF6B35
S = 4                            # 超采样倍数
SIZE = 81


def canvas(size=SIZE, scale=S):
    img = Image.new("RGBA", (size * scale, size * scale), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def finish(img, path, size=SIZE):
    img = img.resize((size, size), Image.LANCZOS)
    img.save(path, "PNG")


def draw_menu(color, path):
    """奶茶杯：杯身梯形 + 杯盖 + 吸管"""
    img, d = canvas()
    s = S
    # 吸管
    d.line([(46 * s, 12 * s), (40 * s, 32 * s)], fill=color, width=5 * s)
    # 杯盖
    d.rounded_rectangle([22 * s, 24 * s, 59 * s, 34 * s], radius=4 * s, fill=color)
    # 杯身（上宽下窄）
    d.polygon([
        (25 * s, 36 * s), (56 * s, 36 * s),
        (51 * s, 68 * s), (30 * s, 68 * s)
    ], fill=color)
    # 杯身中间挖空一道，做出珍珠奶茶的层次感
    d.polygon([
        (30 * s, 44 * s), (51 * s, 44 * s),
        (49 * s, 50 * s), (32 * s, 50 * s)
    ], fill=(255, 255, 255, 255))
    finish(img, path)


def draw_order(color, path):
    """订单小票：圆角矩形 + 三条横线 + 底部锯齿"""
    img, d = canvas()
    s = S
    d.rounded_rectangle([20 * s, 14 * s, 61 * s, 68 * s], radius=6 * s, fill=color)
    # 挖出三条横线
    for i, y in enumerate((30, 42, 54)):
        x2 = 52 * s if i < 2 else 44 * s
        d.rounded_rectangle([28 * s, (y - 2) * s, x2, (y + 2) * s], radius=2 * s,
                            fill=(255, 255, 255, 255))
    finish(img, path)


def draw_mine(color, path):
    """人像：圆头 + 半圆肩"""
    img, d = canvas()
    s = S
    d.ellipse([32 * s, 14 * s, 49 * s, 31 * s], fill=color)
    d.pieslice([18 * s, 36 * s, 63 * s, 81 * s], 180, 360, fill=color)
    finish(img, path)


def draw_logo(path):
    """登录页 Logo：暖色渐变圆形 + 白色奶茶杯"""
    size = 240
    img = Image.new("RGBA", (size * S, size * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # 渐变圆
    for y in range(size * S):
        t = y / (size * S)
        r = int(255 * (1 - t) + 255 * t)
        g = int(107 * (1 - t) + 140 * t)
        b = int(53 * (1 - t) + 90 * t)
        d.line([(0, y), (size * S, y)], fill=(r, g, b, 255))
    mask = Image.new("L", (size * S, size * S), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size * S - 1, size * S - 1], fill=255)
    img.putalpha(mask)

    cup = Image.new("RGBA", (size * S, size * S), (0, 0, 0, 0))
    cd = ImageDraw.Draw(cup)
    k = S * 2
    white = (255, 255, 255, 255)
    cd.line([(150 * k // 2, 52 * k // 2), (132 * k // 2, 92 * k // 2)], fill=white, width=7 * k // 2)
    cd.rounded_rectangle([62 * k // 2, 92 * k // 2, 178 * k // 2, 110 * k // 2], radius=6, fill=white)
    cd.polygon([(70 * k // 2, 118 * k // 2), (170 * k // 2, 118 * k // 2),
                (155 * k // 2, 196 * k // 2), (85 * k // 2, 196 * k // 2)], fill=white)
    cd.polygon([(84 * k // 2, 142 * k // 2), (156 * k // 2, 142 * k // 2),
                (150 * k // 2, 158 * k // 2), (90 * k // 2, 158 * k // 2)],
               fill=(255, 107, 53, 255))
    img = Image.alpha_composite(img, cup)
    finish(img, path, size)


def draw_default_product(path):
    """商品占位图：浅灰底 + 灰色杯形"""
    size = 300
    img = Image.new("RGB", (size, size), (244, 245, 247))
    d = ImageDraw.Draw(img)
    color = (205, 209, 214)
    d.rounded_rectangle([90, 96, 210, 118], radius=8, fill=color)
    d.polygon([(96, 126), (204, 126), (188, 214), (112, 214)], fill=color)
    d.line([(176, 60), (158, 100)], fill=color, width=10)
    img.save(path, "PNG")


def draw_default_avatar(path):
    size = 160
    img = Image.new("RGB", (size, size), (238, 240, 243))
    d = ImageDraw.Draw(img)
    color = (190, 195, 201)
    d.ellipse([58, 34, 102, 78], fill=color)
    d.pieslice([34, 84, 126, 176], 180, 360, fill=color)
    img.save(path, "PNG")


def draw_empty_cart(path):
    size = 300
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    light = (222, 226, 231, 255)
    d.rounded_rectangle([62, 96, 238, 112], radius=8, fill=light)
    d.polygon([(74, 118), (226, 118), (208, 224), (92, 224)], fill=light)
    d.arc([96, 62, 204, 150], 200, 340, fill=light, width=10)
    img.save(path, "PNG")


def draw_pickup(path):
    """取餐牌占位/装饰图"""
    size = 240
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([30, 48, 210, 192], radius=20, fill=(255, 247, 232, 255))
    d.rounded_rectangle([52, 74, 188, 120], radius=10, fill=(255, 107, 53, 255))
    d.rounded_rectangle([52, 140, 140, 160], radius=8, fill=(255, 214, 165, 255))
    img.save(path, "PNG")


if __name__ == "__main__":
    draw_menu(NORMAL, os.path.join(TAB_DIR, "menu.png"))
    draw_menu(ACTIVE, os.path.join(TAB_DIR, "menu-active.png"))
    draw_order(NORMAL, os.path.join(TAB_DIR, "order.png"))
    draw_order(ACTIVE, os.path.join(TAB_DIR, "order-active.png"))
    draw_mine(NORMAL, os.path.join(TAB_DIR, "mine.png"))
    draw_mine(ACTIVE, os.path.join(TAB_DIR, "mine-active.png"))
    draw_logo(os.path.join(STATIC_DIR, "logo.png"))
    draw_default_product(os.path.join(STATIC_DIR, "default-product.png"))
    draw_default_avatar(os.path.join(STATIC_DIR, "default-avatar.png"))
    draw_empty_cart(os.path.join(STATIC_DIR, "empty-cart.png"))
    draw_pickup(os.path.join(STATIC_DIR, "pickup.png"))
    print("icons generated")

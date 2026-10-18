# TabBar 图标说明

此目录已内置可直接使用的 81x81 PNG 图标（透明背景）：

- `menu.png` / `menu-active.png` - 点单
- `order.png` / `order-active.png` - 订单
- `mine.png` / `mine-active.png` - 我的

如需替换品牌图标，保持文件名与尺寸（建议 81x81）不变即可，无需修改 `pages.json`。

其他占位图位于 `src/static/`：

- `logo.png` - 登录页 Logo
- `default-avatar.png` - 默认用户头像
- `default-product.png` - 默认商品占位图
- `empty-cart.png` - 空购物车插画
- `pickup.png` - 取餐相关装饰图

重新生成图标（可选，需要 Pillow）：

```bash
python tools/gen_icons.py
```

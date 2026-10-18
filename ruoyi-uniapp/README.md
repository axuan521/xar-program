# 若依扫码点单小程序（ruoyi-uniapp）

基于 **uni-app + Vue 3 + Pinia** 的扫码点单微信小程序，配套若依（RuoYi-Vue）后台的「点单管理」模块使用。

顾客扫码进入 → 选规格加购 → 提交订单 → 支付（微信支付 / 到店付款 / 模拟支付）→ 获得取餐码；
店员在小程序工作台或 Web 后台处理订单、叫号、核销。

---

## 一、已实现功能

### 顾客端

| 功能 | 说明 |
| --- | --- |
| 扫码点单 | 支持扫码带入桌号（`?tableNo=8` / `?scene=8`），也可手动选择桌号 |
| 菜单浏览 | 左侧分类 + 右侧商品，热销标签、售罄自动隐藏、分类已选角标 |
| 口味选择 | 按「温度 / 糖度 / 加料」分组；加料组支持多选并自动累加价格 |
| 购物车 | 同商品同规格自动合并、数量增减、删除、清空、本地持久化 |
| 确认订单 | 商品明细、桌号、备注快捷标签（少冰/去冰/少糖/打包/不要吸管） |
| 支付 | 微信支付（商户号配置齐全时）、到店付款、**模拟支付**（开发联调） |
| 取餐码 | 下单即生成当日取餐码（A001 起），支付页/订单详情/订单列表均可查看与复制 |
| 订单中心 | 全部 / 待付款 / 待制作 / 制作中 / 待取餐 / 已完成 筛选，下拉刷新，状态自动轮询 |
| 我的 | 登录/退出、订单快捷入口、桌号管理、店员入口、清除缓存 |

### 店员端（小程序内）

| 功能 | 说明 |
| --- | --- |
| 店员登录 | 使用若依后台账号 + 图形验证码登录，复用后台 JWT |
| 工作台 | 待收款 / 待制作 / 制作中 / 待取餐 实时统计，10 秒自动刷新 |
| 订单处理 | 确认收款 → 开始制作 → 制作完成 → 确认取餐，完整状态流转 |
| 扫码核销 | 扫顾客取餐码或手动输入，展示订单明细后一键处理 |
| 权限 | 复用若依 `shop:order:list` / `shop:order:verify` / `shop:order:status` 权限 |

### Web 后台

`ruoyi-ui` 的「点单管理 → 订单管理」已同步修正状态流转：

- 待付款 + 到店付款：新增「确认收款」按钮
- 待制作：开始制作（→ 制作中）
- 制作中：制作完成（→ 待取餐）
- 待取餐：确认取餐（→ 已完成）
- 列表新增「支付状态」列与详情展示

---

## 二、目录结构

```
ruoyi-uniapp/
├── src/
│   ├── api/                 # 接口定义（auth/product/order/staff）
│   ├── components/          # 通用组件
│   ├── config/              # 后端地址、门店信息、状态字典
│   ├── pages/
│   │   ├── menu/            # 点单首页（分类 + 商品 + 购物车栏）
│   │   ├── product-detail/  # 商品详情与口味选择
│   │   ├── cart/            # 购物车
│   │   ├── confirm/         # 确认订单
│   │   ├── pay/             # 收银台（微信支付/到店付款/模拟支付）
│   │   ├── order-list/      # 我的订单
│   │   ├── order-detail/    # 订单详情（取餐码、状态轮询）
│   │   ├── mine/            # 我的
│   │   ├── login/           # 顾客登录
│   │   └── staff/           # 店员登录 / 工作台 / 扫码核销
│   ├── static/              # tabBar 图标与占位图
│   ├── store/               # Pinia：购物车、顾客信息
│   ├── utils/               # 请求封装、鉴权、格式化工具
│   ├── App.vue
│   ├── main.js
│   ├── manifest.json
│   ├── pages.json
│   └── uni.scss
├── tools/gen_icons.py       # 图标生成脚本（可选）
├── .env.development         # 开发环境后端地址
├── .env.production          # 生产环境后端地址
├── index.html
├── package.json
└── vite.config.js
```

---

## 三、快速开始

### 1. 安装依赖

```bash
cd ruoyi-uniapp
npm install
```

### 2. 配置后端地址

编辑 `.env.development`：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

生产环境编辑 `.env.production`，必须是已在小程序后台配置的 **HTTPS 合法域名**：

```bash
VITE_API_BASE_URL=https://shop.example.com
```

> 微信开发者工具本地联调时，请在「详情 → 本地设置」勾选 **不校验合法域名**。
> 真机预览请把 `localhost` 改成电脑局域网 IP，并确保手机与电脑同一网络。

### 3. 配置小程序 AppID

编辑 `src/manifest.json`：

```json
"mp-weixin": {
  "appid": "wx1234567890abcdef"
}
```

### 4. 后端配置

`ruoyi-admin/src/main/resources/application.yml`：

```yaml
wx:
  miniapp:
    appid:            # 小程序 AppID
    secret:           # 小程序 AppSecret
    mock-enabled: true   # 本地无 AppID 时用 code 模拟 openid；上线必须 false
  pay:
    disabled: false
    mock-enabled: true   # 开发期模拟支付；上线必须 false
```

- **没有小程序账号 / 没有商户号**：保持 `mock-enabled: true`，可在 H5 或开发者工具跑通「登录 → 下单 → 模拟支付 → 取餐码 → 店员叫号」全流程。
- **有商户号后**：填齐 `wx.pay.*`（`appid/mchid/merchant-serial-number/private-key-path/api-v3-key/notify-url/public-key-path`），支付通道自动切换为微信支付，**前端无需改代码**。
- 生产环境配置见 `application-prod.yml`，其中的 `mock-enabled` 必须为 `false`。

### 5. 初始化数据库

执行 `sql/shop_20261017.sql`，会创建：

- `shop_category`、`shop_product`、`shop_product_flavor`
- `wx_user`、`shop_order`、`shop_order_item`
- 「点单管理」后台菜单（商品分类 / 商品管理 / 订单管理 / 小程序顾客）

### 6. 运行

```bash
# 微信小程序（产物在 dist/dev/mp-weixin）
npm run dev:mp-weixin

# H5 调试（浏览器直接跑，便于快速改样式）
npm run dev:h5
```

用微信开发者工具导入 `dist/dev/mp-weixin` 目录预览。

生产构建：

```bash
npm run build:mp-weixin   # 产物 dist/build/mp-weixin
npm run build:h5
```

---

## 四、店员端使用

1. 在「我的 → 店员工作台」或登录页底部进入店员登录。
2. 输入若依后台账号（如 `admin`）与密码，按提示输入图形验证码。
3. 登录后进入工作台：
   - **确认收款**：到店付款订单，店员收到钱后点击，订单进入「待制作」
   - **开始制作**：待制作 → 制作中
   - **制作完成**：制作中 → 待取餐，顾客端会看到取餐码
   - **确认取餐**：待取餐 → 已完成
4. 「扫码核销」支持扫顾客取餐码二维码、手动输入 `A001`。

> 店员账号需要在「系统管理 → 角色管理」中分配 `shop:order:list`、`shop:order:verify`、`shop:order:status` 权限。

---

## 五、支付说明

### 到店付款（默认，个人主体可用）

1. 顾客下单后订单为「待付款」，页面展示取餐码与到店付款指引。
2. 顾客到收银台报取餐码付款。
3. 店员在小程序工作台或 Web 后台点「确认收款」，订单转为「待制作」。

### 微信支付（需个体工商户/企业商户号）

1. 后端 `wx.pay.*` 配置齐全后自动启用。
2. 顾客提交订单 → 收银台调起 `wx.requestPayment`。
3. 微信回调 `/mp/pay/notify` 验签解密后，订单自动转为「已支付 + 待制作」。

### 模拟支付（仅开发）

后端开启 `wx.pay.mock-enabled: true` 时，收银台会出现「模拟支付」按钮。
点击后订单直接置为「已支付 + 待制作」，并写入 `MOCK` 前缀支付流水，便于联调取餐码与店员叫号。
**上线前务必关闭。**

---

## 六、扫码点单二维码

小程序码 / 普通二维码指向：

```
pages/menu/index?tableNo=8
```

或使用微信「扫普通链接二维码打开小程序」配置，二维码内容：

```
https://your-domain.com/order?tableNo=8
```

小程序 `onLoad` 会解析 `tableNo`、`scene`、`table` 参数并写入桌号；下单时桌号会拼进订单备注（`[桌号8]`）。

---

## 七、后端接口清单

### 顾客端（匿名 + `X-Wx-Token`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/mp/auth/login` | 微信登录 |
| POST | `/mp/auth/info` | 当前顾客信息 |
| POST | `/mp/auth/logout` | 退出 |
| GET | `/mp/product/category/list` | 分类列表 |
| GET | `/mp/product/list` | 可售商品（含口味） |
| GET | `/mp/product/{id}` | 商品详情 |
| POST | `/mp/order/create` | 创建订单 |
| GET | `/mp/order/my` | 我的订单 |
| GET | `/mp/order/{id}` | 订单详情 |
| PUT | `/mp/order/cancel/{id}` | 取消订单 |
| GET | `/mp/order/payType` | 支付通道状态 |
| GET | `/mp/order/pay/{id}` | 发起支付（返回微信支付参数/到店付款标识） |
| POST | `/mp/order/mock-pay/{id}` | 模拟支付（需开启配置） |

### 店员端（后台 JWT + 权限）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/shop/order/pending` | 待处理订单（含待收款） |
| GET | `/shop/order/pickup/{code}` | 按取餐码查单 |
| POST | `/shop/order/verify/{id}` | 确认收款 / 确认取餐 |
| PUT | `/shop/order/status` | 开始制作 / 制作完成 |
| PUT | `/shop/order/finish/{id}` | 完成订单 |

---

## 八、常见问题

**Q：H5 调试登录时报“微信登录失败”？**
A：H5 无法调用 `uni.login`，代码会自动使用本地稳定的 mock code，配合后端 `wx.miniapp.mock-enabled: true` 即可登录。

**Q：微信开发者工具请求 localhost 失败？**
A：勾选「不校验合法域名」；真机调试请改用局域网 IP 或 HTTPS 域名。

**Q：店员登录提示“该账号没有点单管理权限”？**
A：在若依后台给该角色分配 `shop:order:list`、`shop:order:verify`、`shop:order:status` 权限，重新登录。

**Q：订单一直显示待付款？**
A：到店付款订单需要店员点「确认收款」；微信支付需等回调；开发期可直接用「模拟支付」。

**Q：商品图片不显示？**
A：`shop_product.product_image` 存相对路径时，小程序会自动拼接后端域名；也可填完整 HTTPS 地址。后台需能通过 `/profile/**` 访问上传文件。

---

## License

MIT

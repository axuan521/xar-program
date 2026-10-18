<script>
import { getCustomerInfo } from '@/api/auth'
import { useUserStore } from '@/store'
import { isCustomerLogin } from '@/utils/auth'

export default {
  onLaunch() {
    // 请求层检测到顾客登录态失效时，同步清理 Pinia 中的登录状态
    uni.$on('shop:customer-logout', () => {
      useUserStore().logout()
    })

    // 小程序菜单是公开数据，启动不强制登录；下单/查看订单时再引导登录
    if (isCustomerLogin()) {
      this.refreshCustomer()
    }
  },

  onShow() {},

  onHide() {},

  methods: {
    async refreshCustomer() {
      try {
        const info = await getCustomerInfo()
        if (info && info.wxUserId) {
          useUserStore().updateInfo(info)
        }
      } catch (e) {
        // 登录态失效时静默处理，下次进入下单流程会重新引导登录
      }
    }
  }
}
</script>

<style>
/* ============ 全局基础样式 ============ */
page {
  background-color: #f5f6f8;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  font-size: 28rpx;
  color: #333;
  -webkit-font-smoothing: antialiased;
}

view,
text,
image,
scroll-view,
button,
input,
textarea {
  box-sizing: border-box;
}

/* 去掉 button 默认样式，统一由业务类控制 */
button {
  padding: 0;
  margin: 0;
  background: transparent;
  line-height: normal;
  border-radius: 0;
}

button::after {
  border: none;
}

image {
  display: block;
}

/* ============ 通用布局 ============ */
.container {
  padding: 24rpx;
}

.flex {
  display: flex;
}

.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.flex-column {
  display: flex;
  flex-direction: column;
}

.flex-1 {
  flex: 1;
  min-width: 0;
}

/* ============ 文本 ============ */
.text-primary {
  color: #ff6b35;
}

.text-gray {
  color: #999;
}

.text-muted {
  color: #bbb;
}

.text-bold {
  font-weight: 600;
}

.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.ellipsis-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ============ 间距 ============ */
.mt-10 { margin-top: 10rpx; }
.mt-20 { margin-top: 20rpx; }
.mt-30 { margin-top: 30rpx; }
.mb-20 { margin-bottom: 20rpx; }
.px-20 { padding-left: 20rpx; padding-right: 20rpx; }

/* ============ 卡片 ============ */
.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

/* ============ 价格 ============ */
.price {
  color: #ff6b35;
  font-weight: 600;
}

.price-symbol {
  font-size: 22rpx;
  margin-right: 2rpx;
}

/* ============ 按钮 ============ */
.btn-primary {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
  border: none;
  border-radius: 999rpx;
  font-size: 30rpx;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-plain {
  background: #fff;
  color: #666;
  border: 2rpx solid #ddd;
  border-radius: 999rpx;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-primary:active,
.btn-plain:active {
  opacity: 0.8;
}

.btn-disabled {
  background: #d9d9d9 !important;
  color: #fff !important;
}

/* ============ 安全区 ============ */
.safe-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* ============ 页面底部占位（避开固定结算栏） ============ */
.bottom-placeholder {
  height: 140rpx;
}

.bottom-placeholder-lg {
  height: 200rpx;
}
</style>

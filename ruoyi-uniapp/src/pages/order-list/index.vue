<template>
  <view class="order-page">
    <!-- 未登录 -->
    <view v-if="!userStore.isLogin" class="login-guide">
      <image src="/static/logo.png" mode="aspectFit" class="guide-logo" />
      <text class="guide-title">登录后查看订单</text>
      <text class="guide-desc">登录可同步你的点单记录与取餐码</text>
      <view class="guide-btn" @click="goLogin">微信一键登录</view>
    </view>

    <template v-else>
      <!-- 状态筛选 -->
      <scroll-view class="tabs" scroll-x :show-scrollbar="false">
        <view class="tabs-inner">
          <view
            v-for="tab in tabs"
            :key="tab.value"
            :class="['tab', { active: currentTab === tab.value }]"
            @click="switchTab(tab.value)"
          >
            <text>{{ tab.label }}</text>
          </view>
        </view>
      </scroll-view>

      <!-- 订单列表 -->
      <view class="order-list">
        <view v-for="order in orders" :key="order.orderId" class="order-card" @click="goDetail(order.orderId)">
          <!-- 头部 -->
          <view class="order-head">
            <view class="order-head-left">
              <text class="order-no">NO.{{ order.orderNo }}</text>
              <text class="order-time">{{ formatFriendlyTime(order.createTime) }}</text>
            </view>
            <text :class="['status-text', 'status-' + order.orderStatus]">
              {{ statusText(order.orderStatus) }}
            </text>
          </view>

          <!-- 取餐码 -->
          <view v-if="order.pickupCode && order.orderStatus !== '5'" class="pickup-row">
            <view class="pickup-left">
              <text class="pickup-label">取餐码</text>
              <text class="pickup-value">{{ order.pickupCode }}</text>
            </view>
            <text class="pickup-copy" @click.stop="copyCode(order.pickupCode)">复制</text>
          </view>

          <!-- 商品 -->
          <view class="order-items">
            <view v-for="item in (order.items || []).slice(0, 3)" :key="item.itemId" class="order-item">
              <text class="item-name">{{ item.productName }}</text>
              <text v-if="item.flavorText" class="item-flavor">{{ item.flavorText }}</text>
              <text class="item-qty">×{{ item.quantity }}</text>
            </view>
            <text v-if="(order.items || []).length > 3" class="more-text">
              还有 {{ order.items.length - 3 }} 件商品...
            </text>
          </view>

          <!-- 金额 + 操作 -->
          <view class="order-foot">
            <view class="order-amount">
              <text class="amount-label">共 {{ order.totalQuantity || countQuantity(order) }} 件</text>
              <text class="amount-symbol">¥</text>
              <text class="amount-value">{{ formatMoney(order.totalAmount) }}</text>
            </view>

            <view class="order-actions">
              <template v-if="order.orderStatus === '0'">
                <view class="action-btn plain" @click.stop="cancel(order)">取消</view>
                <view class="action-btn primary" @click.stop="goPay(order.orderId)">去支付</view>
              </template>
              <template v-else-if="order.orderStatus === '1' || order.orderStatus === '2'">
                <view class="action-btn plain" @click.stop="goDetail(order.orderId)">查看详情</view>
              </template>
              <template v-else-if="order.orderStatus === '3'">
                <view class="action-btn primary" @click.stop="goDetail(order.orderId)">查看取餐码</view>
              </template>
              <template v-else>
                <view class="action-btn plain" @click.stop="goMenu">再来一单</view>
              </template>
            </view>
          </view>
        </view>

        <view v-if="!loading && orders.length === 0" class="empty-block">
          <text class="empty-icon">📋</text>
          <text class="empty-title">暂无相关订单</text>
          <text class="empty-desc">去点一杯喜欢的饮品吧</text>
          <view class="empty-btn" @click="goMenu">去点单</view>
        </view>
      </view>
    </template>
  </view>
</template>

<script>
import { getMyOrders, cancelOrder } from '@/api/order'
import { useUserStore } from '@/store'
import { formatMoney, formatFriendlyTime, copyText } from '@/utils'

export default {
  data() {
    return {
      loading: false,
      currentTab: '',
      orders: [],
      tabs: [
        { label: '全部', value: '' },
        { label: '待付款', value: '0' },
        { label: '待制作', value: '1' },
        { label: '制作中', value: '2' },
        { label: '待取餐', value: '3' },
        { label: '已完成', value: '4' }
      ]
    }
  },

  computed: {
    userStore() {
      return useUserStore()
    }
  },

  onShow() {
    // 「我的」页快捷入口通过本地缓存指定要展示的状态页签
    const presetTab = uni.getStorageSync('order_list_tab')
    if (presetTab !== '' && presetTab !== undefined && presetTab !== null) {
      uni.removeStorageSync('order_list_tab')
      this.currentTab = presetTab === 'all' ? '' : presetTab
    }
    if (this.userStore.isLogin) {
      this.loadOrders()
    } else {
      this.orders = []
    }
  },

  onPullDownRefresh() {
    if (!this.userStore.isLogin) {
      uni.stopPullDownRefresh()
      return
    }
    this.loadOrders().finally(() => uni.stopPullDownRefresh())
  },

  methods: {
    formatMoney,
    formatFriendlyTime,

    statusText(status) {
      return {
        '0': '待付款',
        '1': '待制作',
        '2': '制作中',
        '3': '待取餐',
        '4': '已完成',
        '5': '已取消'
      }[status] || '未知'
    },

    countQuantity(order) {
      return (order.items || []).reduce((sum, item) => sum + (item.quantity || 0), 0)
    },

    async loadOrders() {
      if (this.loading) return
      this.loading = true
      try {
        const list = await getMyOrders(this.currentTab)
        this.orders = list || []
      } catch (e) {
        this.orders = []
      } finally {
        this.loading = false
      }
    },

    switchTab(value) {
      if (this.currentTab === value) return
      this.currentTab = value
      this.loadOrders()
    },

    copyCode(code) {
      copyText(code, '取餐码已复制')
    },

    goDetail(orderId) {
      uni.navigateTo({ url: `/pages/order-detail/index?orderId=${orderId}` })
    },

    goPay(orderId) {
      uni.navigateTo({ url: `/pages/pay/index?orderId=${orderId}` })
    },

    goMenu() {
      uni.switchTab({ url: '/pages/menu/index' })
    },

    goLogin() {
      uni.navigateTo({
        url: `/pages/login/index?redirect=${encodeURIComponent('/pages/order-list/index')}`
      })
    },

    cancel(order) {
      uni.showModal({
        title: '取消订单',
        content: '确定取消该订单吗？已支付订单取消后请联系店员退款。',
        confirmText: '确定取消',
        success: async res => {
          if (!res.confirm) return
          try {
            await cancelOrder(order.orderId)
            uni.showToast({ title: '订单已取消', icon: 'success' })
            this.loadOrders()
          } catch (e) {
            // 统一提示
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.order-page {
  min-height: 100vh;
  background: #f5f6f8;
}

/* ===== 登录引导 ===== */
.login-guide {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 150rpx 40rpx;
}

.guide-logo {
  width: 160rpx;
  height: 160rpx;
  margin-bottom: 30rpx;
}

.guide-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
}

.guide-desc {
  font-size: 24rpx;
  color: #aaa;
  margin-top: 12rpx;
}

.guide-btn {
  margin-top: 50rpx;
  width: 380rpx;
  height: 88rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
  font-size: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== 标签 ===== */
.tabs {
  background: #fff;
  white-space: nowrap;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tabs-inner {
  display: flex;
  padding: 0 12rpx;
}

.tab {
  padding: 26rpx 24rpx;
  font-size: 26rpx;
  color: #666;
  position: relative;
}

.tab.active {
  color: #ff6b35;
  font-weight: 600;
}

.tab.active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 8rpx;
  transform: translateX(-50%);
  width: 36rpx;
  height: 6rpx;
  border-radius: 6rpx;
  background: #ff6b35;
}

/* ===== 订单卡片 ===== */
.order-list {
  padding: 20rpx;
}

.order-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 26rpx;
  margin-bottom: 20rpx;
}

.order-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.order-head-left {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.order-no {
  font-size: 22rpx;
  color: #999;
}

.order-time {
  font-size: 22rpx;
  color: #bbb;
  margin-top: 6rpx;
}

.status-text {
  font-size: 26rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.status-0 { color: #ff6b35; }
.status-1 { color: #fa8c16; }
.status-2 { color: #1890ff; }
.status-3 { color: #52c41a; }
.status-4 { color: #999; }
.status-5 { color: #bbb; }

/* 取餐码 */
.pickup-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff8f4;
  border-radius: 14rpx;
  padding: 16rpx 20rpx;
  margin-top: 18rpx;
}

.pickup-left {
  display: flex;
  align-items: baseline;
}

.pickup-label {
  font-size: 22rpx;
  color: #999;
  margin-right: 14rpx;
}

.pickup-value {
  font-size: 44rpx;
  font-weight: 700;
  color: #ff6b35;
  letter-spacing: 4rpx;
  line-height: 1.1;
}

.pickup-copy {
  font-size: 22rpx;
  color: #ff6b35;
}

/* 商品 */
.order-items {
  margin-top: 18rpx;
  border-bottom: 1rpx solid #f7f7f7;
  padding-bottom: 12rpx;
}

.order-item {
  display: flex;
  align-items: center;
  padding: 8rpx 0;
}

.item-name {
  font-size: 26rpx;
  color: #333;
  flex-shrink: 0;
}

.item-flavor {
  font-size: 22rpx;
  color: #aaa;
  margin-left: 12rpx;
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.item-qty {
  font-size: 22rpx;
  color: #999;
  margin-left: auto;
  padding-left: 12rpx;
}

.more-text {
  display: block;
  font-size: 22rpx;
  color: #bbb;
  padding: 8rpx 0;
}

/* 底部 */
.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18rpx;
}

.order-amount {
  display: flex;
  align-items: baseline;
}

.amount-label {
  font-size: 22rpx;
  color: #999;
  margin-right: 10rpx;
}

.amount-symbol {
  font-size: 22rpx;
  color: #ff6b35;
  font-weight: 600;
}

.amount-value {
  font-size: 34rpx;
  color: #ff6b35;
  font-weight: 700;
}

.order-actions {
  display: flex;
  align-items: center;
}

.action-btn {
  height: 62rpx;
  padding: 0 30rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 14rpx;
}

.action-btn.plain {
  border: 2rpx solid #e5e5e5;
  color: #666;
}

.action-btn.primary {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
}

.action-btn:active {
  opacity: 0.8;
}

/* 空态 */
.empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 140rpx 0;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #555;
}

.empty-desc {
  font-size: 24rpx;
  color: #aaa;
  margin-top: 10rpx;
}

.empty-btn {
  margin-top: 36rpx;
  width: 240rpx;
  height: 76rpx;
  border-radius: 999rpx;
  background: #fff4ee;
  color: #ff6b35;
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

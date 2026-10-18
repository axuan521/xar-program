<template>
  <view class="staff-page">
    <!-- 顶部 -->
    <view class="header">
      <view class="header-left">
        <text class="hello">你好，{{ staffName }}</text>
        <text class="hello-sub">今天也要好好做饮品呀</text>
      </view>
      <view class="header-right">
        <view class="scan-entry" @click="goVerify">
          <text class="scan-icon">📷</text>
          <text class="scan-text">扫码核销</text>
        </view>
        <view class="logout-btn" @click="logout">退出</view>
      </view>
    </view>

    <!-- 统计卡片 -->
    <view class="stats-row">
      <view
        v-for="item in statCards"
        :key="item.status"
        :class="['stat-card', { active: filterStatus === item.status }]"
        @click="toggleFilter(item.status)"
      >
        <text class="stat-value">{{ item.count }}</text>
        <text class="stat-label">{{ item.label }}</text>
      </view>
    </view>

    <!-- 工具条 -->
    <view class="toolbar">
      <view class="toolbar-left">
        <text class="toolbar-title">{{ filterStatus === '' ? '全部待处理' : currentLabel }}</text>
        <text class="toolbar-count">{{ filteredOrders.length }} 单</text>
      </view>
      <view class="toolbar-right">
        <text class="refresh-text" @click="loadOrders">刷新</text>
      </view>
    </view>

    <!-- 订单列表 -->
    <view class="order-list">
      <view v-for="order in filteredOrders" :key="order.orderId" class="order-card">
        <!-- 头部 -->
        <view class="order-head">
          <view class="pickup-code-box">
            <text class="pickup-label">取餐码</text>
            <text class="pickup-code">{{ order.pickupCode }}</text>
          </view>
          <view class="order-head-right">
            <text :class="['pay-tag', order.payStatus === '1' ? 'paid' : 'unpaid']">
              {{ order.payStatus === '1' ? '已支付' : '待收款' }}
            </text>
            <text class="order-no">{{ shortOrderNo(order.orderNo) }}</text>
            <text class="order-time">{{ formatTimeHM(order.createTime) }}</text>
          </view>
        </view>

        <!-- 商品 -->
        <view class="order-items">
          <view v-for="item in order.items" :key="item.itemId" class="order-item">
            <view class="item-line">
              <text class="item-name">{{ item.productName }}</text>
              <text class="item-qty">×{{ item.quantity }}</text>
            </view>
            <text v-if="item.flavorText" class="item-flavor">{{ item.flavorText }}</text>
          </view>
        </view>

        <!-- 备注 -->
        <view v-if="order.customerRemark" class="remark-box">
          <text class="remark-label">备注</text>
          <text class="remark-text">{{ order.customerRemark }}</text>
        </view>

        <!-- 金额 & 操作 -->
        <view class="order-foot">
          <view class="order-amount">
            <text class="amount-label">合计</text>
            <text class="amount-symbol">¥</text>
            <text class="amount-value">{{ formatMoney(order.totalAmount) }}</text>
          </view>

          <view class="actions">
            <view
              v-if="order.payStatus !== '1'"
              class="action primary"
              @click="handleCollect(order)"
            >
              确认收款
            </view>
            <view
              v-else-if="order.orderStatus === '1'"
              class="action primary"
              @click="handleStatus(order, '2', '已开始制作')"
            >
              开始制作
            </view>
            <view
              v-else-if="order.orderStatus === '2'"
              class="action success"
              @click="handleStatus(order, '3', '制作完成，等待取餐')"
            >
              制作完成
            </view>
            <view
              v-else-if="order.orderStatus === '3'"
              class="action success"
              @click="handlePickup(order)"
            >
              确认取餐
            </view>
          </view>
        </view>
      </view>

      <view v-if="!loading && filteredOrders.length === 0" class="empty-block">
        <text class="empty-icon">☕</text>
        <text class="empty-title">暂无待处理订单</text>
        <text class="empty-desc">新订单会自动刷新出来</text>
      </view>
    </view>

    <view class="bottom-tip">
      <text>每 10 秒自动刷新 · 下拉可手动刷新</text>
    </view>
  </view>
</template>

<script>
import { getPendingOrders, verifyOrder, updateOrderStatus } from '@/api/staff'
import { getStaffInfo, clearStaffLogin, isStaffLogin } from '@/utils/auth'
import { formatMoney, formatTimeHM } from '@/utils'

export default {
  data() {
    return {
      orders: [],
      loading: false,
      filterStatus: '',
      staffInfo: {},
      timer: null
    }
  },

  computed: {
    staffName() {
      return this.staffInfo.nickName || this.staffInfo.userName || '店员'
    },

    statCards() {
      return [
        { label: '待收款', status: '0', count: this.countByStatus('0') },
        { label: '待制作', status: '1', count: this.countByStatus('1') },
        { label: '制作中', status: '2', count: this.countByStatus('2') },
        { label: '待取餐', status: '3', count: this.countByStatus('3') }
      ]
    },

    filteredOrders() {
      if (this.filterStatus === '') return this.orders
      return this.orders.filter(o => o.orderStatus === this.filterStatus)
    },

    currentLabel() {
      const map = { '0': '待收款', '1': '待制作', '2': '制作中', '3': '待取餐' }
      return map[this.filterStatus] || '全部待处理'
    }
  },

  onLoad() {
    if (!isStaffLogin()) {
      uni.redirectTo({ url: '/pages/staff/login' })
      return
    }
    this.staffInfo = getStaffInfo()
    this.loadOrders()
    this.startTimer()
  },

  onShow() {
    if (!isStaffLogin()) return
    this.staffInfo = getStaffInfo()
    this.loadOrders()
    this.startTimer()
  },

  onHide() {
    this.stopTimer()
  },

  onUnload() {
    this.stopTimer()
  },

  onPullDownRefresh() {
    this.loadOrders().finally(() => uni.stopPullDownRefresh())
  },

  methods: {
    formatMoney,
    formatTimeHM,

    countByStatus(status) {
      if (status === '0') {
        // 待收款：未支付的到店付款订单
        return this.orders.filter(o => o.payStatus !== '1' && o.orderStatus === '0').length
      }
      return this.orders.filter(o => o.orderStatus === status && o.payStatus === '1').length
    },

    shortOrderNo(orderNo) {
      if (!orderNo) return ''
      return orderNo.length > 12 ? '...' + orderNo.slice(-10) : orderNo
    },

    startTimer() {
      this.stopTimer()
      this.timer = setInterval(() => {
        this.loadOrders(true)
      }, 10000)
    },

    stopTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },

    async loadOrders(silent) {
      if (this.loading) return
      this.loading = true
      try {
        const list = await getPendingOrders()
        this.orders = list || []
      } catch (e) {
        if (!silent) this.orders = []
      } finally {
        this.loading = false
      }
    },

    toggleFilter(status) {
      this.filterStatus = this.filterStatus === status ? '' : status
    },

    async handleCollect(order) {
      uni.showModal({
        title: '确认收款',
        content: `确认已收到「${order.pickupCode}」订单的 ¥${formatMoney(order.totalAmount)}？`,
        confirmText: '已收款',
        success: async res => {
          if (!res.confirm) return
          uni.showLoading({ title: '处理中...' })
          try {
            await verifyOrder(order.orderId)
            uni.hideLoading()
            uni.showToast({ title: '收款成功', icon: 'success' })
            this.loadOrders()
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    },

    async handleStatus(order, status, tip) {
      uni.showLoading({ title: '处理中...' })
      try {
        await updateOrderStatus(order.orderId, status)
        uni.hideLoading()
        uni.showToast({ title: tip, icon: 'none' })
        this.loadOrders()
      } catch (e) {
        uni.hideLoading()
      }
    },

    handlePickup(order) {
      uni.showModal({
        title: '确认取餐',
        content: `确认顾客已取走「${order.pickupCode}」的订单？`,
        confirmText: '已取餐',
        success: async res => {
          if (!res.confirm) return
          uni.showLoading({ title: '处理中...' })
          try {
            await verifyOrder(order.orderId)
            uni.hideLoading()
            uni.showToast({ title: '取餐完成', icon: 'success' })
            this.loadOrders()
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    },

    goVerify() {
      uni.navigateTo({ url: '/pages/staff/verify' })
    },

    logout() {
      uni.showModal({
        title: '退出工作台',
        content: '确定退出店员登录吗？',
        success: res => {
          if (!res.confirm) return
          clearStaffLogin()
          uni.redirectTo({ url: '/pages/staff/login' })
        }
      })
    }
  }
}
</script>

<style scoped>
.staff-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 40rpx;
}

/* ===== 顶部 ===== */
.header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 40rpx 28rpx 60rpx;
  background: linear-gradient(135deg, #2b2b2b 0%, #444 100%);
}

.header-left {
  display: flex;
  flex-direction: column;
}

.hello {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
}

.hello-sub {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 8rpx;
}

.header-right {
  display: flex;
  align-items: center;
}

.scan-entry {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.14);
  border-radius: 999rpx;
  padding: 12rpx 22rpx;
  margin-right: 14rpx;
}

.scan-icon {
  font-size: 24rpx;
  margin-right: 8rpx;
}

.scan-text {
  font-size: 22rpx;
  color: #fff;
}

.logout-btn {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  border: 2rpx solid rgba(255, 255, 255, 0.25);
  border-radius: 999rpx;
  padding: 12rpx 22rpx;
}

/* ===== 统计 ===== */
.stats-row {
  display: flex;
  margin: -36rpx 20rpx 0;
  background: #fff;
  border-radius: 20rpx;
  padding: 20rpx 0;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.05);
  position: relative;
  z-index: 2;
}

.stat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12rpx 0;
  border-radius: 14rpx;
}

.stat-card.active {
  background: #fff4ee;
}

.stat-value {
  font-size: 44rpx;
  font-weight: 700;
  color: #ff6b35;
  line-height: 1.2;
}

.stat-label {
  font-size: 22rpx;
  color: #888;
  margin-top: 4rpx;
}

/* ===== 工具条 ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 28rpx 12rpx;
}

.toolbar-left {
  display: flex;
  align-items: baseline;
}

.toolbar-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
}

.toolbar-count {
  font-size: 22rpx;
  color: #999;
  margin-left: 12rpx;
}

.refresh-text {
  font-size: 24rpx;
  color: #ff6b35;
}

/* ===== 订单卡片 ===== */
.order-list {
  padding: 0 20rpx;
}

.order-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.order-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.pickup-code-box {
  display: flex;
  flex-direction: column;
  background: #fff4ee;
  border-radius: 14rpx;
  padding: 12rpx 24rpx;
}

.pickup-label {
  font-size: 20rpx;
  color: #c98a6a;
}

.pickup-code {
  font-size: 46rpx;
  font-weight: 700;
  color: #ff6b35;
  letter-spacing: 4rpx;
  line-height: 1.15;
}

.order-head-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.pay-tag {
  font-size: 20rpx;
  border-radius: 999rpx;
  padding: 4rpx 16rpx;
}

.pay-tag.paid {
  background: #f0fff0;
  color: #52c41a;
}

.pay-tag.unpaid {
  background: #fff7e6;
  color: #fa8c16;
}

.order-no {
  font-size: 20rpx;
  color: #bbb;
  margin-top: 8rpx;
}

.order-time {
  font-size: 20rpx;
  color: #bbb;
  margin-top: 4rpx;
}

/* 商品 */
.order-items {
  margin-top: 18rpx;
  border-top: 1rpx dashed #f0f0f0;
  padding-top: 16rpx;
}

.order-item {
  padding: 6rpx 0;
}

.item-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-name {
  font-size: 26rpx;
  color: #333;
}

.item-qty {
  font-size: 24rpx;
  color: #ff6b35;
  font-weight: 600;
}

.item-flavor {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-top: 2rpx;
}

/* 备注 */
.remark-box {
  display: flex;
  align-items: flex-start;
  background: #fffbe6;
  border-radius: 12rpx;
  padding: 14rpx 18rpx;
  margin-top: 14rpx;
}

.remark-label {
  font-size: 22rpx;
  color: #d48806;
  font-weight: 600;
  margin-right: 10rpx;
  flex-shrink: 0;
}

.remark-text {
  font-size: 22rpx;
  color: #ad6800;
  line-height: 1.5;
  flex: 1;
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
  margin-right: 8rpx;
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

.actions {
  display: flex;
  align-items: center;
}

.action {
  height: 66rpx;
  padding: 0 34rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action.primary {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
}

.action.success {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  color: #fff;
}

.action:active {
  opacity: 0.85;
}

/* 空态 */
.empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

.empty-icon {
  font-size: 90rpx;
  margin-bottom: 16rpx;
}

.empty-title {
  font-size: 30rpx;
  color: #555;
  font-weight: 600;
}

.empty-desc {
  font-size: 22rpx;
  color: #aaa;
  margin-top: 10rpx;
}

.bottom-tip {
  text-align: center;
  font-size: 20rpx;
  color: #c5c8cc;
  padding: 20rpx 0;
}
</style>

<template>
  <view class="detail-page">
    <!-- 状态头部 -->
    <view :class="['status-hero', 'hero-' + (order.orderStatus || '0')]">
      <view class="status-icon">{{ statusIcon }}</view>
      <view class="status-text-box">
        <text class="status-title">{{ statusText }}</text>
        <text class="status-desc">{{ statusDesc }}</text>
      </view>
    </view>

    <!-- 取餐码 -->
    <view v-if="order.pickupCode && order.orderStatus !== '5'" class="pickup-card" @click="copyPickupCode">
      <view class="pickup-left">
        <text class="pickup-label">取餐码</text>
        <text class="pickup-code">{{ order.pickupCode }}</text>
        <text class="pickup-tip">请凭此码到取餐台取餐</text>
      </view>
      <view class="pickup-right">
        <text class="copy-btn">复制</text>
      </view>
    </view>

    <!-- 商品明细 -->
    <view class="card">
      <view class="card-title">商品明细</view>

      <view v-for="item in (order.items || [])" :key="item.itemId" class="order-item">
        <image
          :src="itemImage(item)"
          mode="aspectFill"
          class="item-image"
          @error="onImageError(item)"
        />
        <view class="item-main">
          <text class="item-name">{{ item.productName }}</text>
          <text v-if="item.flavorText" class="item-flavor">{{ item.flavorText }}</text>
          <view class="item-foot">
            <text class="item-price">¥{{ formatMoney(item.unitPrice) }}</text>
            <text class="item-qty">×{{ item.quantity }}</text>
          </view>
        </view>
        <text class="item-subtotal">¥{{ formatMoney(item.subtotal) }}</text>
      </view>

      <view class="amount-row">
        <text class="amount-label">商品金额</text>
        <text class="amount-value">¥{{ formatMoney(order.totalAmount) }}</text>
      </view>
      <view class="amount-row total">
        <text class="amount-label">实付金额</text>
        <text class="amount-total">¥{{ formatMoney(order.totalAmount) }}</text>
      </view>
    </view>

    <!-- 订单信息 -->
    <view class="card">
      <view class="card-title">订单信息</view>

      <view class="info-row">
        <text class="info-label">订单编号</text>
        <view class="info-value-row">
          <text class="info-value">{{ order.orderNo }}</text>
          <text class="copy-small" @click="copyText(order.orderNo, '订单编号已复制')">复制</text>
        </view>
      </view>
      <view class="info-row">
        <text class="info-label">下单时间</text>
        <text class="info-value">{{ formatTime(order.createTime) }}</text>
      </view>
      <view v-if="order.payTime" class="info-row">
        <text class="info-label">支付时间</text>
        <text class="info-value">{{ formatTime(order.payTime) }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">支付方式</text>
        <text class="info-value">{{ order.payType === '2' ? '微信支付' : '到店付款' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">支付状态</text>
        <text :class="['info-value', order.payStatus === '1' ? 'success' : 'warning']">
          {{ order.payStatus === '1' ? '已支付' : order.payStatus === '2' ? '已退款' : '未支付' }}
        </text>
      </view>
      <view v-if="order.customerRemark" class="info-row">
        <text class="info-label">订单备注</text>
        <text class="info-value remark">{{ order.customerRemark }}</text>
      </view>
      <view v-if="order.cancelReason" class="info-row">
        <text class="info-label">取消原因</text>
        <text class="info-value">{{ order.cancelReason }}</text>
      </view>
    </view>

    <view class="bottom-placeholder-lg"></view>

    <!-- 底部操作 -->
    <view class="bottom-bar safe-bottom">
      <template v-if="order.orderStatus === '0'">
        <view class="btn-plain half" @click="cancelOrder">取消订单</view>
        <view class="btn-primary half" @click="goPay">去支付</view>
      </template>
      <template v-else-if="order.orderStatus === '1' || order.orderStatus === '2'">
        <view class="btn-plain half" @click="refresh(true)">刷新状态</view>
        <view class="btn-primary half" @click="goMenu">再点一单</view>
      </template>
      <template v-else-if="order.orderStatus === '3'">
        <view class="btn-plain half" @click="copyPickupCode">复制取餐码</view>
        <view class="btn-primary half" @click="goMenu">再点一单</view>
      </template>
      <template v-else>
        <view class="btn-primary full" @click="goMenu">再来一单</view>
      </template>
    </view>
  </view>
</template>

<script>
import { getOrderDetail, cancelOrder } from '@/api/order'
import { formatMoney, formatTime, copyText, resolveImage } from '@/utils'

export default {
  data() {
    return {
      orderId: null,
      order: {},
      pollTimer: null
    }
  },

  computed: {
    statusText() {
      return {
        '0': '待付款',
        '1': '待制作',
        '2': '制作中',
        '3': '待取餐',
        '4': '已完成',
        '5': '已取消'
      }[this.order.orderStatus] || '订单详情'
    },

    statusIcon() {
      return {
        '0': '💰',
        '1': '📝',
        '2': '👨‍🍳',
        '3': '🎫',
        '4': '✅',
        '5': '✕'
      }[this.order.orderStatus] || '🧾'
    },

    statusDesc() {
      const map = {
        '0': '请尽快完成支付，付款后门店开始制作',
        '1': '门店已接单，正在排队制作',
        '2': '饮品制作中，请稍候',
        '3': '制作完成，请凭取餐码到取餐台取餐',
        '4': '订单已完成，感谢惠顾',
        '5': '订单已取消'
      }
      return map[this.order.orderStatus] || ''
    }
  },

  onLoad(options) {
    this.orderId = options.orderId
    this.refresh()
  },

  onPullDownRefresh() {
    this.refresh().finally(() => uni.stopPullDownRefresh())
  },

  onShow() {
    if (this.orderId) this.startPoll()
  },

  onHide() {
    this.stopPoll()
  },

  onUnload() {
    this.stopPoll()
  },

  methods: {
    formatMoney,
    formatTime,
    copyText,

    async refresh(manual) {
      try {
        this.order = (await getOrderDetail(this.orderId)) || {}
        if (manual) uni.showToast({ title: '已刷新', icon: 'none' })
        this.startPoll()
      } catch (e) {
        // 统一提示
      }
    },

    /** 制作中/待取餐时定时刷新，状态变化自动更新 */
    startPoll() {
      const status = this.order.orderStatus
      if (!['0', '1', '2', '3'].includes(status)) {
        this.stopPoll()
        return
      }
      if (this.pollTimer) return
      this.pollTimer = setInterval(() => {
        this.silentRefresh()
      }, 8000)
    },

    stopPoll() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },

    async silentRefresh() {
      try {
        const data = await getOrderDetail(this.orderId)
        const changed = (data.orderStatus || '') !== (this.order.orderStatus || '')
        this.order = data || {}
        if (changed) {
          uni.showToast({ title: this.statusText, icon: 'none' })
        }
        if (!['0', '1', '2', '3'].includes(this.order.orderStatus)) {
          this.stopPoll()
        }
      } catch (e) {
        // 静默
      }
    },

    itemImage(item) {
      if (item.imageError || !item.productImage) return '/static/default-product.png'
      return resolveImage(item.productImage)
    },

    onImageError(item) {
      item.imageError = true
    },

    copyPickupCode() {
      if (!this.order.pickupCode) return
      copyText(this.order.pickupCode, '取餐码已复制')
    },

    goPay() {
      uni.navigateTo({ url: `/pages/pay/index?orderId=${this.orderId}` })
    },

    goMenu() {
      uni.switchTab({ url: '/pages/menu/index' })
    },

    cancelOrder() {
      uni.showModal({
        title: '取消订单',
        content: '确定取消该订单吗？',
        confirmText: '确定取消',
        success: async res => {
          if (!res.confirm) return
          try {
            await cancelOrder(this.orderId)
            uni.showToast({ title: '订单已取消', icon: 'success' })
            this.refresh()
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
.detail-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 150rpx;
}

/* ===== 状态头 ===== */
.status-hero {
  display: flex;
  align-items: center;
  padding: 44rpx 32rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
}

.hero-1,
.hero-2 {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.hero-3 {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
}

.hero-4,
.hero-5 {
  background: linear-gradient(135deg, #8c8c8c 0%, #b0b0b0 100%);
}

.status-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  font-size: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
}

.status-text-box {
  display: flex;
  flex-direction: column;
}

.status-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
}

.status-desc {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.88);
  margin-top: 8rpx;
}

/* ===== 取餐码 ===== */
.pickup-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 26rpx 30rpx;
  border: 2rpx dashed #ffd2bf;
}

.pickup-left {
  display: flex;
  align-items: baseline;
}

.pickup-label {
  font-size: 24rpx;
  color: #999;
  margin-right: 18rpx;
}

.pickup-code {
  font-size: 58rpx;
  font-weight: 700;
  color: #ff6b35;
  letter-spacing: 6rpx;
  line-height: 1;
}

.pickup-tip {
  font-size: 20rpx;
  color: #bbb;
  margin-left: 18rpx;
}

.copy-btn {
  font-size: 22rpx;
  color: #ff6b35;
  background: #fff4ee;
  border-radius: 999rpx;
  padding: 8rpx 20rpx;
}

/* ===== 卡片 ===== */
.card {
  background: #fff;
  border-radius: 20rpx;
  margin: 20rpx;
  padding: 28rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
  margin-bottom: 20rpx;
}

/* ===== 商品 ===== */
.order-item {
  display: flex;
  align-items: center;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f7f7f7;
}

.order-item:last-of-type {
  border-bottom: none;
}

.item-image {
  width: 110rpx;
  height: 110rpx;
  border-radius: 14rpx;
  flex-shrink: 0;
  background: #f5f6f8;
}

.item-main {
  flex: 1;
  margin-left: 18rpx;
  min-width: 0;
}

.item-name {
  font-size: 28rpx;
  font-weight: 500;
  color: #222;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.item-flavor {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}

.item-foot {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
}

.item-price {
  font-size: 24rpx;
  color: #666;
}

.item-qty {
  font-size: 22rpx;
  color: #999;
  margin-left: 14rpx;
}

.item-subtotal {
  font-size: 28rpx;
  font-weight: 600;
  color: #222;
  margin-left: 16rpx;
}

/* ===== 金额 ===== */
.amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 0;
}

.amount-label {
  font-size: 26rpx;
  color: #666;
}

.amount-value {
  font-size: 26rpx;
  color: #333;
}

.amount-row.total {
  border-top: 1rpx solid #f5f5f5;
  margin-top: 10rpx;
  padding-top: 20rpx;
}

.amount-total {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff6b35;
}

/* ===== 订单信息 ===== */
.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 12rpx 0;
}

.info-label {
  font-size: 24rpx;
  color: #999;
  flex-shrink: 0;
}

.info-value-row {
  display: flex;
  align-items: center;
  margin-left: 20rpx;
}

.info-value {
  font-size: 24rpx;
  color: #333;
  text-align: right;
  word-break: break-all;
}

.info-value.success {
  color: #52c41a;
}

.info-value.warning {
  color: #fa8c16;
}

.info-value.remark {
  color: #ff6b35;
  max-width: 480rpx;
}

.copy-small {
  font-size: 20rpx;
  color: #ff6b35;
  margin-left: 12rpx;
}

/* ===== 底部 ===== */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 128rpx;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
}

.half {
  flex: 1;
  height: 84rpx;
  margin: 0 10rpx;
  font-size: 28rpx;
  font-weight: 600;
}

.full {
  flex: 1;
  height: 84rpx;
  font-size: 28rpx;
  font-weight: 600;
}

.btn-primary {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-plain {
  background: #f5f6f8;
  color: #666;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-primary:active,
.btn-plain:active {
  opacity: 0.85;
}
</style>

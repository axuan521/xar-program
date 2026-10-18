<template>
  <view class="pay-page">
    <!-- 支付结果 / 取餐码 -->
    <view :class="['hero', paid ? 'hero-success' : '']">
      <template v-if="paid">
        <view class="hero-icon">✓</view>
        <text class="hero-title">支付成功</text>
        <text class="hero-sub">请留意叫号，凭取餐码取餐</text>
      </template>
      <template v-else>
        <view class="hero-icon">¥</view>
        <text class="hero-title">待支付</text>
        <text class="hero-sub">{{ payTypeText }}</text>
      </template>

      <view class="amount-box">
        <text class="amount-symbol">¥</text>
        <text class="amount-value">{{ formatMoney(order.totalAmount) }}</text>
      </view>

      <view v-if="order.pickupCode" class="pickup-box" @click="copyPickupCode">
        <text class="pickup-label">取餐码</text>
        <text class="pickup-code">{{ order.pickupCode }}</text>
        <text class="pickup-copy">点击复制</text>
      </view>
    </view>

    <!-- 支付方式 -->
    <view v-if="!paid" class="card pay-card">
      <view class="card-title">支付方式</view>

      <!-- 微信支付 -->
      <view v-if="payConfig.wxPayEnabled" class="pay-option active">
        <view class="pay-option-left">
          <text class="pay-option-icon">💚</text>
          <view class="pay-option-text">
            <text class="pay-option-name">微信支付</text>
            <text class="pay-option-desc">推荐使用，支付后自动进入制作队列</text>
          </view>
        </view>
        <view class="pay-check">✓</view>
      </view>

      <!-- 到店付款 -->
      <view v-else class="pay-option active">
        <view class="pay-option-left">
          <text class="pay-option-icon">🏪</text>
          <view class="pay-option-text">
            <text class="pay-option-name">到店付款</text>
            <text class="pay-option-desc">请到收银台出示取餐码 {{ order.pickupCode }} 付款</text>
          </view>
        </view>
        <view class="pay-check">✓</view>
      </view>

      <!-- 模拟支付 -->
      <view v-if="payConfig.mockPayEnabled" class="mock-tip">
        <text class="mock-tip-title">🧪 开发模式</text>
        <text class="mock-tip-text">
          当前未配置微信支付商户号，可点击下方「模拟支付」直接完成支付，体验取餐码与店员叫号流程。
          上线前请在后端关闭 wx.pay.mock-enabled。
        </text>
      </view>
    </view>

    <!-- 订单摘要 -->
    <view class="card">
      <view class="card-title">订单信息</view>
      <view class="info-row">
        <text class="info-label">订单编号</text>
        <text class="info-value">{{ order.orderNo }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">商品数量</text>
        <text class="info-value">{{ totalQuantity }} 件</text>
      </view>
      <view v-if="order.customerRemark" class="info-row">
        <text class="info-label">备注</text>
        <text class="info-value remark">{{ order.customerRemark }}</text>
      </view>

      <view class="order-items">
        <view v-for="item in (order.items || [])" :key="item.itemId" class="order-item">
          <text class="order-item-name">{{ item.productName }}</text>
          <text class="order-item-qty">×{{ item.quantity }}</text>
          <text class="order-item-price">¥{{ formatMoney(item.subtotal) }}</text>
        </view>
      </view>
    </view>

    <view class="bottom-placeholder-lg"></view>

    <!-- 底部按钮 -->
    <view class="bottom-bar safe-bottom">
      <template v-if="paid">
        <view class="btn-plain half" @click="goOrderList">查看订单</view>
        <view class="btn-primary half" @click="goOrderDetail">查看取餐码</view>
      </template>

      <template v-else>
        <view class="btn-plain half" @click="refreshOrder">刷新状态</view>

        <view v-if="payConfig.wxPayEnabled" class="btn-primary half" @click="wxPay">
          微信支付
        </view>
        <view v-else-if="payConfig.mockPayEnabled" class="btn-primary half" @click="doMockPay">
          模拟支付
        </view>
        <view v-else class="btn-primary half" @click="goOrderDetail">
          我已完成付款
        </view>
      </template>
    </view>
  </view>
</template>

<script>
import { getOrderDetail, preparePay, mockPay, getPayType } from '@/api/order'
import { formatMoney, copyText } from '@/utils'

export default {
  data() {
    return {
      orderId: null,
      order: {},
      payConfig: {
        wxPayEnabled: false,
        mockPayEnabled: false
      },
      paying: false,
      pollTimer: null
    }
  },

  computed: {
    paid() {
      return this.order.payStatus === '1'
    },

    totalQuantity() {
      return (this.order.items || []).reduce((sum, item) => sum + (item.quantity || 0), 0)
    },

    payTypeText() {
      if (this.order.payType === '2') return '微信支付'
      return '到店付款'
    }
  },

  onLoad(options) {
    this.orderId = options.orderId
    this.loadOrder()
    this.loadPayConfig()
  },

  onShow() {
    // 从后台返回时刷新一次订单状态
    if (this.orderId) this.refreshOrder(true)
  },

  onUnload() {
    this.clearPoll()
  },

  methods: {
    formatMoney,

    async loadOrder() {
      if (!this.orderId) return
      try {
        this.order = (await getOrderDetail(this.orderId)) || {}
        // 未支付时轮询，便于到店付款被店员确认后自动刷新
        if (this.order.payStatus !== '1' && this.order.orderStatus !== '5') {
          this.startPoll()
        } else {
          this.clearPoll()
        }
      } catch (e) {
        // 统一提示
      }
    },

    async loadPayConfig() {
      try {
        // 只查询支付通道开关，真正预下单留到用户点击支付时，避免提前占用预支付单
        const config = await getPayType()
        this.payConfig = {
          wxPayEnabled: !!config.wxPayEnabled,
          mockPayEnabled: !!config.mockPayEnabled
        }
      } catch (e) {
        // 查询失败时按到店付款展示，不影响查看取餐码
      }
    },

    startPoll() {
      this.clearPoll()
      this.pollTimer = setInterval(() => {
        this.refreshOrder(true)
      }, 6000)
    },

    clearPoll() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },

    async refreshOrder(silent) {
      try {
        const data = await getOrderDetail(this.orderId)
        this.order = data || {}
        if (this.paid) {
          this.clearPoll()
          if (!silent) uni.showToast({ title: '支付成功', icon: 'success' })
        } else if (!silent) {
          uni.showToast({ title: '暂未查询到支付结果', icon: 'none' })
        }
      } catch (e) {
        // 静默
      }
    },

    async wxPay() {
      if (this.paying) return
      this.paying = true
      uni.showLoading({ title: '发起支付...' })
      try {
        const payInfo = await preparePay(this.orderId)
        if (!payInfo || payInfo.payType !== '2' || !payInfo.params) {
          throw new Error('微信支付未启用')
        }
        uni.hideLoading()

        const params = payInfo.params
        uni.requestPayment({
          provider: 'wxpay',
          timeStamp: params.timeStamp,
          nonceStr: params.nonceStr,
          package: params.package,
          signType: params.signType || 'RSA',
          paySign: params.paySign,
          success: () => {
            uni.showToast({ title: '支付成功', icon: 'success' })
            // 回调可能有延迟，轮询几次直到后端确认
            this.pollPaidTimes(8)
          },
          fail: err => {
            const cancelled = err && /cancel/i.test(err.errMsg || '')
            if (!cancelled) {
              uni.showToast({ title: '支付未完成，请重试', icon: 'none' })
            }
          }
        })
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '发起支付失败', icon: 'none' })
      } finally {
        this.paying = false
      }
    },

    async pollPaidTimes(times) {
      for (let i = 0; i < times; i++) {
        await new Promise(resolve => setTimeout(resolve, 1500))
        try {
          const data = await getOrderDetail(this.orderId)
          this.order = data || {}
          if (this.order.payStatus === '1') {
            this.clearPoll()
            uni.showToast({ title: '支付成功', icon: 'success' })
            return
          }
        } catch (e) {
          // 继续重试
        }
      }
      uni.showToast({ title: '支付结果确认中，请稍后刷新', icon: 'none' })
    },

    doMockPay() {
      uni.showModal({
        title: '模拟支付',
        content: '将直接把该订单标记为已支付，仅用于开发测试，确认继续？',
        confirmText: '确认支付',
        success: async res => {
          if (!res.confirm) return
          uni.showLoading({ title: '支付中...' })
          try {
            await mockPay(this.orderId)
            uni.hideLoading()
            uni.showToast({ title: '模拟支付成功', icon: 'success' })
            await this.refreshOrder(true)
            setTimeout(() => {
              uni.redirectTo({ url: `/pages/order-detail/index?orderId=${this.orderId}` })
            }, 800)
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    },

    copyPickupCode() {
      if (!this.order.pickupCode) return
      copyText(this.order.pickupCode, '取餐码已复制')
    },

    goOrderDetail() {
      uni.redirectTo({ url: `/pages/order-detail/index?orderId=${this.orderId}` })
    },

    goOrderList() {
      uni.switchTab({ url: '/pages/order-list/index' })
    }
  }
}
</script>

<style scoped>
.pay-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 150rpx;
}

/* ===== 顶部 ===== */
.hero {
  background: linear-gradient(160deg, #ff8c5a 0%, #ff6b35 100%);
  padding: 50rpx 28rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero-success {
  background: linear-gradient(160deg, #52c41a 0%, #39a30e 100%);
}

.hero-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  font-size: 46rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18rpx;
}

.hero-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
}

.hero-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 8rpx;
}

.amount-box {
  display: flex;
  align-items: baseline;
  margin-top: 28rpx;
  color: #fff;
}

.amount-symbol {
  font-size: 30rpx;
  font-weight: 600;
}

.amount-value {
  font-size: 68rpx;
  font-weight: 700;
  margin-left: 4rpx;
}

.pickup-box {
  margin-top: 30rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 18rpx;
  padding: 20rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 300rpx;
}

.pickup-label {
  font-size: 22rpx;
  color: #999;
}

.pickup-code {
  font-size: 64rpx;
  font-weight: 700;
  color: #ff6b35;
  letter-spacing: 8rpx;
  line-height: 1.2;
}

.pickup-copy {
  font-size: 20rpx;
  color: #bbb;
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

/* ===== 支付选项 ===== */
.pay-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx;
  border-radius: 14rpx;
  border: 2rpx solid #f0f0f0;
}

.pay-option.active {
  border-color: #ff6b35;
  background: #fff8f4;
}

.pay-option-left {
  display: flex;
  align-items: center;
}

.pay-option-icon {
  font-size: 44rpx;
  margin-right: 18rpx;
}

.pay-option-text {
  display: flex;
  flex-direction: column;
}

.pay-option-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #222;
}

.pay-option-desc {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}

.pay-check {
  width: 38rpx;
  height: 38rpx;
  border-radius: 50%;
  background: #ff6b35;
  color: #fff;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mock-tip {
  margin-top: 20rpx;
  background: #f7f8fa;
  border-radius: 14rpx;
  padding: 18rpx 20rpx;
  display: flex;
  flex-direction: column;
}

.mock-tip-title {
  font-size: 24rpx;
  font-weight: 600;
  color: #666;
}

.mock-tip-text {
  font-size: 22rpx;
  color: #999;
  line-height: 1.6;
  margin-top: 6rpx;
}

/* ===== 订单信息 ===== */
.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 8rpx 0;
}

.info-label {
  font-size: 24rpx;
  color: #999;
  flex-shrink: 0;
}

.info-value {
  font-size: 24rpx;
  color: #333;
  margin-left: 20rpx;
  text-align: right;
  word-break: break-all;
}

.info-value.remark {
  color: #ff6b35;
}

.order-items {
  margin-top: 16rpx;
  border-top: 1rpx solid #f5f5f5;
  padding-top: 16rpx;
}

.order-item {
  display: flex;
  align-items: center;
  padding: 8rpx 0;
}

.order-item-name {
  flex: 1;
  font-size: 24rpx;
  color: #444;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.order-item-qty {
  font-size: 22rpx;
  color: #999;
  margin: 0 20rpx;
}

.order-item-price {
  font-size: 24rpx;
  color: #333;
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

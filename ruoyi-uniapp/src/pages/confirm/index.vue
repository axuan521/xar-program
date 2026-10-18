<template>
  <view class="confirm-page">
    <!-- 桌号 / 就餐方式 -->
    <view class="card table-card">
      <view class="table-row">
        <view class="table-left">
          <text class="table-icon">🍽️</text>
          <view class="table-text">
            <text class="table-title">堂食</text>
            <text class="table-sub">{{ cartStore.tableNo ? cartStore.tableNo + ' 号桌' : '未选择桌号' }}</text>
          </view>
        </view>
        <text class="link" @click="editTableNo">修改</text>
      </view>
    </view>

    <!-- 商品明细 -->
    <view class="card">
      <view class="card-head">
        <text class="card-title">商品明细</text>
        <text class="card-count">共 {{ cartStore.totalCount }} 件</text>
      </view>

      <view v-for="item in cartStore.items" :key="item.key" class="order-item">
        <image :src="itemImage(item)" mode="aspectFill" class="item-image" @error="onImageError(item)" />
        <view class="item-main">
          <text class="item-name">{{ item.productName }}</text>
          <text v-if="item.flavorText" class="item-flavor">{{ item.flavorText }}</text>
          <view class="item-foot">
            <text class="item-price">¥{{ formatMoney(itemUnitPrice(item)) }}</text>
            <text class="item-qty">×{{ item.quantity }}</text>
          </view>
        </view>
        <text class="item-subtotal">¥{{ formatMoney(itemSubtotal(item)) }}</text>
      </view>
    </view>

    <!-- 备注 -->
    <view class="card">
      <view class="card-head">
        <text class="card-title">订单备注</text>
        <text class="card-count">{{ remark.length }}/100</text>
      </view>
      <textarea
        v-model="remark"
        class="remark-input"
        placeholder="口味偏好、少冰、打包等特殊要求（选填）"
        placeholder-class="placeholder"
        maxlength="100"
        :auto-height="true"
      />
      <view class="quick-remarks">
        <text
          v-for="tag in quickRemarks"
          :key="tag"
          :class="['quick-tag', { active: remark.includes(tag) }]"
          @click="toggleQuickRemark(tag)"
        >
          {{ tag }}
        </text>
      </view>
    </view>

    <!-- 支付方式 -->
    <view class="card">
      <view class="card-head">
        <text class="card-title">支付方式</text>
      </view>
      <view class="pay-row">
        <view class="pay-left">
          <text class="pay-icon">{{ payIcon }}</text>
          <view class="pay-text">
            <text class="pay-name">{{ payName }}</text>
            <text class="pay-desc">{{ payDesc }}</text>
          </view>
        </view>
        <view class="pay-check">✓</view>
      </view>
    </view>

    <!-- 金额 -->
    <view class="card">
      <view class="amount-row">
        <text class="amount-label">商品金额</text>
        <text class="amount-value">¥{{ formatMoney(cartStore.totalPrice) }}</text>
      </view>
      <view class="amount-row">
        <text class="amount-label">打包费</text>
        <text class="amount-value">¥0.00</text>
      </view>
      <view class="amount-row total">
        <text class="amount-label">应付金额</text>
        <text class="amount-total">¥{{ formatMoney(cartStore.totalPrice) }}</text>
      </view>
    </view>

    <view class="bottom-placeholder-lg"></view>

    <!-- 提交栏 -->
    <view class="submit-bar safe-bottom">
      <view class="submit-total">
        <text class="submit-label">合计</text>
        <text class="submit-symbol">¥</text>
        <text class="submit-value">{{ formatMoney(cartStore.totalPrice) }}</text>
      </view>
      <view :class="['submit-btn', { disabled: submitting }]" @click="submitOrder">
        {{ submitting ? '提交中...' : '提交订单' }}
      </view>
    </view>
  </view>
</template>

<script>
import { createOrder, getPayType } from '@/api/order'
import { useCartStore, useUserStore } from '@/store'
import { formatMoney, resolveImage, sumMoney } from '@/utils'

export default {
  data() {
    return {
      remark: '',
      submitting: false,
      payConfig: {},
      quickRemarks: ['少冰', '去冰', '少糖', '打包', '不要吸管']
    }
  },

  computed: {
    cartStore() {
      return useCartStore()
    },

    userStore() {
      return useUserStore()
    },

    payName() {
      if (this.payConfig.wxPayEnabled) return '微信支付'
      if (this.payConfig.mockPayEnabled) return '模拟支付（开发测试）'
      return '到店付款'
    },

    payIcon() {
      if (this.payConfig.wxPayEnabled) return '💚'
      if (this.payConfig.mockPayEnabled) return '🧪'
      return '🏪'
    },

    payDesc() {
      if (this.payConfig.wxPayEnabled) return '提交订单后调起微信收银台'
      if (this.payConfig.mockPayEnabled) return '未配置商户号，可先体验完整下单流程'
      return '提交订单后到收银台出示取餐码付款'
    }
  },

  onLoad() {
    this.loadPayConfig()
  },

  methods: {
    formatMoney,

    async loadPayConfig() {
      try {
        this.payConfig = (await getPayType()) || {}
      } catch (e) {
        this.payConfig = {}
      }
    },

    itemUnitPrice(item) {
      return sumMoney([Number(item.price), Number(item.flavorAmount || 0)])
    },

    itemSubtotal(item) {
      return sumMoney([this.itemUnitPrice(item) * item.quantity])
    },

    itemImage(item) {
      if (item.imageError || !item.productImage) return '/static/default-product.png'
      return resolveImage(item.productImage)
    },

    onImageError(item) {
      item.imageError = true
    },

    toggleQuickRemark(tag) {
      if (this.remark.includes(tag)) {
        this.remark = this.remark.replace(tag, '').replace(/^、|、$/g, '').replace(/、、/g, '、')
      } else {
        this.remark = this.remark ? `${this.remark}、${tag}` : tag
      }
    },

    editTableNo() {
      uni.showModal({
        title: '选择桌号',
        editable: true,
        placeholderText: '请输入桌号，如 8',
        content: this.cartStore.tableNo || '',
        success: res => {
          if (res.confirm) this.cartStore.setTableNo((res.content || '').trim())
        }
      })
    },

    buildRemark() {
      const table = this.cartStore.tableNo ? `[桌号${this.cartStore.tableNo}]` : ''
      return `${table}${this.remark}`.slice(0, 200)
    },

    async submitOrder() {
      if (this.submitting) return
      if (this.cartStore.isEmpty) {
        uni.showToast({ title: '购物车是空的', icon: 'none' })
        return
      }
      // 未登录先去登录，登录后回到本页继续提交
      if (!this.userStore.isLogin) {
        uni.navigateTo({
          url: `/pages/login/index?redirect=${encodeURIComponent('/pages/confirm/index')}`
        })
        return
      }

      this.submitting = true
      try {
        const result = await createOrder(this.cartStore.toOrderItems(), this.buildRemark())
        this.cartStore.clear()
        uni.redirectTo({
          url: `/pages/pay/index?orderId=${result.orderId}`
        })
      } catch (e) {
        // 错误已统一提示
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.confirm-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding: 20rpx;
}

.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
}

.card-count {
  font-size: 22rpx;
  color: #bbb;
}

/* ===== 桌号 ===== */
.table-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.table-left {
  display: flex;
  align-items: center;
}

.table-icon {
  font-size: 40rpx;
  margin-right: 16rpx;
}

.table-text {
  display: flex;
  flex-direction: column;
}

.table-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
}

.table-sub {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}

.link {
  font-size: 26rpx;
  color: #ff6b35;
}

/* ===== 商品 ===== */
.order-item {
  display: flex;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f7f7f7;
}

.order-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.item-image {
  width: 120rpx;
  height: 120rpx;
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

/* ===== 备注 ===== */
.remark-input {
  width: 100%;
  min-height: 120rpx;
  background: #f7f8fa;
  border-radius: 14rpx;
  padding: 20rpx;
  font-size: 26rpx;
  color: #333;
  line-height: 1.6;
}

.placeholder {
  color: #bbb;
}

.quick-remarks {
  display: flex;
  flex-wrap: wrap;
  margin-top: 16rpx;
}

.quick-tag {
  font-size: 22rpx;
  color: #666;
  background: #f5f6f8;
  border-radius: 999rpx;
  padding: 10rpx 24rpx;
  margin: 0 14rpx 12rpx 0;
}

.quick-tag.active {
  background: #fff4ee;
  color: #ff6b35;
}

/* ===== 支付方式 ===== */
.pay-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pay-left {
  display: flex;
  align-items: center;
}

.pay-icon {
  font-size: 40rpx;
  margin-right: 16rpx;
}

.pay-text {
  display: flex;
  flex-direction: column;
}

.pay-name {
  font-size: 28rpx;
  font-weight: 500;
  color: #222;
}

.pay-desc {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}

.pay-check {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #ff6b35;
  color: #fff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== 金额 ===== */
.amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 0;
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
  margin-top: 12rpx;
  padding-top: 22rpx;
}

.amount-total {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff6b35;
}

/* ===== 提交栏 ===== */
.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 128rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28rpx;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
}

.submit-total {
  display: flex;
  align-items: baseline;
}

.submit-label {
  font-size: 24rpx;
  color: #999;
  margin-right: 8rpx;
}

.submit-symbol {
  font-size: 24rpx;
  color: #ff6b35;
  font-weight: 600;
}

.submit-value {
  font-size: 44rpx;
  font-weight: 700;
  color: #ff6b35;
}

.submit-btn {
  width: 260rpx;
  height: 84rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-btn.disabled {
  background: #d9d9d9;
}

.submit-btn:active {
  opacity: 0.85;
}
</style>

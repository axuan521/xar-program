<template>
  <view class="cart-page">
    <!-- 空购物车 -->
    <view v-if="cartStore.isEmpty" class="empty-wrap">
      <image src="/static/empty-cart.png" mode="aspectFit" class="empty-image" />
      <text class="empty-title">购物车还是空的</text>
      <text class="empty-desc">去菜单挑一杯喜欢的饮品吧</text>
      <view class="empty-btn" @click="goMenu">去点单</view>
    </view>

    <template v-else>
      <!-- 桌号 -->
      <view class="table-bar">
        <view class="table-left">
          <text class="table-icon">🍽️</text>
          <text class="table-label">就餐方式</text>
          <text class="table-value">{{ cartStore.tableNo ? '堂食 · ' + cartStore.tableNo + ' 号桌' : '未选择桌号' }}</text>
        </view>
        <view class="table-edit" @click="editTableNo">修改</view>
      </view>

      <!-- 商品列表 -->
      <view class="item-list">
        <view v-for="(item, index) in cartStore.items" :key="item.key" class="item-card">
          <image
            :src="itemImage(item)"
            mode="aspectFill"
            class="item-image"
            @error="onImageError(item)"
          />

          <view class="item-main">
            <view class="item-head">
              <text class="item-name">{{ item.productName }}</text>
              <text class="item-delete" @click="removeItem(index)">删除</text>
            </view>

            <text v-if="item.flavorText" class="item-flavor">{{ item.flavorText }}</text>

            <view class="item-foot">
              <view class="item-price">
                <text class="price-symbol">¥</text>
                <text class="price-value">{{ formatMoney(itemUnitPrice(item)) }}</text>
              </view>

              <view class="stepper">
                <view class="step-btn" @click="decrease(index)">−</view>
                <text class="step-value">{{ item.quantity }}</text>
                <view class="step-btn plus" @click="increase(index)">+</view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 清空 -->
      <view class="clear-row">
        <text class="clear-btn" @click="clearCart">清空购物车</text>
      </view>

      <view class="tip-card">
        <text class="tip-title">温馨提示</text>
        <text class="tip-text">· 下单后请留意取餐码，制作完成后到取餐台叫号取餐</text>
        <text class="tip-text">· 饮品口味以门店实际供应为准，售罄商品会自动下架</text>
      </view>

      <view class="bottom-placeholder"></view>

      <!-- 结算栏 -->
      <view class="checkout-bar safe-bottom">
        <view class="total-box">
          <text class="total-label">合计</text>
          <view class="total-price">
            <text class="price-symbol">¥</text>
            <text class="price-value">{{ formatMoney(cartStore.totalPrice) }}</text>
          </view>
          <text class="total-count">共 {{ cartStore.totalCount }} 件</text>
        </view>
        <view class="checkout-btn" @click="goConfirm">去结算</view>
      </view>
    </template>
  </view>
</template>

<script>
import { useCartStore } from '@/store'
import { formatMoney, resolveImage, sumMoney } from '@/utils'

export default {
  data() {
    return {}
  },

  computed: {
    cartStore() {
      return useCartStore()
    }
  },

  methods: {
    formatMoney,

    itemUnitPrice(item) {
      return sumMoney([Number(item.price), Number(item.flavorAmount || 0)])
    },

    itemImage(item) {
      if (item.imageError || !item.productImage) return '/static/default-product.png'
      return resolveImage(item.productImage)
    },

    onImageError(item) {
      item.imageError = true
    },

    increase(index) {
      this.cartStore.increaseItem(index)
    },

    decrease(index) {
      this.cartStore.decreaseItem(index)
    },

    removeItem(index) {
      const item = this.cartStore.items[index]
      uni.showModal({
        title: '删除商品',
        content: `确定删除「${item.productName}」吗？`,
        success: res => {
          if (res.confirm) this.cartStore.removeItem(index)
        }
      })
    },

    clearCart() {
      uni.showModal({
        title: '清空购物车',
        content: '确定清空购物车中的全部商品吗？',
        success: res => {
          if (res.confirm) this.cartStore.clear()
        }
      })
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

    goMenu() {
      uni.switchTab({ url: '/pages/menu/index' })
    },

    goConfirm() {
      if (this.cartStore.isEmpty) {
        uni.showToast({ title: '购物车是空的', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/confirm/index' })
    }
  }
}
</script>

<style scoped>
.cart-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding: 20rpx;
}

/* ===== 空态 ===== */
.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}

.empty-image {
  width: 260rpx;
  height: 260rpx;
  margin-bottom: 30rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #444;
}

.empty-desc {
  font-size: 24rpx;
  color: #aaa;
  margin-top: 12rpx;
}

.empty-btn {
  margin-top: 44rpx;
  width: 260rpx;
  height: 80rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== 桌号 ===== */
.table-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 20rpx;
}

.table-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.table-icon {
  font-size: 30rpx;
  margin-right: 12rpx;
}

.table-label {
  font-size: 26rpx;
  color: #999;
  margin-right: 12rpx;
}

.table-value {
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}

.table-edit {
  font-size: 24rpx;
  color: #ff6b35;
  padding: 6rpx 18rpx;
  background: #fff4ee;
  border-radius: 999rpx;
}

/* ===== 商品 ===== */
.item-list {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
}

.item-card {
  display: flex;
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.item-card:last-child {
  border-bottom: none;
}

.item-image {
  width: 150rpx;
  height: 150rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
  background: #f5f6f8;
}

.item-main {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.item-name {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.item-delete {
  font-size: 22rpx;
  color: #bbb;
  padding-left: 20rpx;
  flex-shrink: 0;
}

.item-flavor {
  font-size: 22rpx;
  color: #999;
  margin-top: 10rpx;
  line-height: 1.4;
}

.item-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-price {
  display: flex;
  align-items: baseline;
  color: #ff6b35;
}

.price-symbol {
  font-size: 22rpx;
  font-weight: 600;
}

.price-value {
  font-size: 34rpx;
  font-weight: 700;
}

.stepper {
  display: flex;
  align-items: center;
}

.step-btn {
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  background: #f5f6f8;
  color: #666;
  font-size: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-btn.plus {
  background: #fff4ee;
  color: #ff6b35;
}

.step-btn:active {
  opacity: 0.7;
}

.step-value {
  min-width: 64rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  color: #222;
}

/* ===== 清空 / 提示 ===== */
.clear-row {
  display: flex;
  justify-content: flex-end;
  padding: 24rpx 4rpx;
}

.clear-btn {
  font-size: 24rpx;
  color: #999;
  text-decoration: underline;
}

.tip-card {
  background: #fff7ef;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  display: flex;
  flex-direction: column;
}

.tip-title {
  font-size: 24rpx;
  color: #c96a2a;
  font-weight: 600;
  margin-bottom: 10rpx;
}

.tip-text {
  font-size: 22rpx;
  color: #b98a68;
  line-height: 1.7;
}

/* ===== 结算栏 ===== */
.checkout-bar {
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

.total-box {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
}

.total-label {
  font-size: 24rpx;
  color: #999;
  margin-right: 10rpx;
}

.total-price {
  display: flex;
  align-items: baseline;
  color: #ff6b35;
}

.total-count {
  font-size: 20rpx;
  color: #bbb;
  margin-left: 12rpx;
}

.checkout-btn {
  width: 240rpx;
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

.checkout-btn:active {
  opacity: 0.85;
}
</style>

<template>
  <view class="mine-page">
    <!-- 用户卡片 -->
    <view class="user-card">
      <image :src="avatar" mode="aspectFill" class="avatar" @error="avatarError = true" />
      <view class="user-info">
        <text class="nickname">{{ userStore.isLogin ? userStore.nickName : '未登录' }}</text>
        <text class="user-sub">
          {{ userStore.isLogin ? '欢迎回来，今天想喝点什么？' : '登录后可查看订单与取餐码' }}
        </text>
      </view>
      <view v-if="!userStore.isLogin" class="login-btn" @click="goLogin">登录</view>
      <view v-else class="logout-btn" @click="logout">退出</view>
    </view>

    <!-- 订单快捷入口 -->
    <view class="card order-card">
      <view class="card-head">
        <text class="card-title">我的订单</text>
        <text class="card-more" @click="goOrders('')">全部订单 ›</text>
      </view>
      <view class="order-grid">
        <view class="order-grid-item" @click="goOrders('0')">
          <text class="grid-icon">💰</text>
          <text class="grid-label">待付款</text>
        </view>
        <view class="order-grid-item" @click="goOrders('1')">
          <text class="grid-icon">📝</text>
          <text class="grid-label">待制作</text>
        </view>
        <view class="order-grid-item" @click="goOrders('3')">
          <text class="grid-icon">🎫</text>
          <text class="grid-label">待取餐</text>
        </view>
        <view class="order-grid-item" @click="goOrders('4')">
          <text class="grid-icon">✅</text>
          <text class="grid-label">已完成</text>
        </view>
      </view>
    </view>

    <!-- 当前桌号 -->
    <view class="card">
      <view class="row" @click="editTableNo">
        <view class="row-left">
          <text class="row-icon">🍽️</text>
          <text class="row-label">当前桌号</text>
        </view>
        <view class="row-right">
          <text class="row-value">{{ cartStore.tableNo ? cartStore.tableNo + ' 号桌' : '未选择' }}</text>
          <text class="row-arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="card">
      <view class="row" @click="goMenu">
        <view class="row-left">
          <text class="row-icon">🥤</text>
          <text class="row-label">扫码点单</text>
        </view>
        <view class="row-right">
          <text class="row-arrow">›</text>
        </view>
      </view>

      <view class="row" @click="goStaff">
        <view class="row-left">
          <text class="row-icon">🧑‍🍳</text>
          <text class="row-label">店员工作台</text>
        </view>
        <view class="row-right">
          <text class="row-value">{{ staffLogged ? '已登录' : '' }}</text>
          <text class="row-arrow">›</text>
        </view>
      </view>

      <view class="row" @click="clearCache">
        <view class="row-left">
          <text class="row-icon">🧹</text>
          <text class="row-label">清除缓存</text>
        </view>
        <view class="row-right">
          <text class="row-arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 门店信息 -->
    <view class="card shop-card">
      <view class="card-title">门店信息</view>
      <view class="shop-row">
        <text class="shop-label">门店名称</text>
        <text class="shop-value">{{ shop.name }}</text>
      </view>
      <view class="shop-row">
        <text class="shop-label">营业时间</text>
        <text class="shop-value">{{ shop.businessHours }}</text>
      </view>
      <view class="shop-row">
        <text class="shop-label">门店地址</text>
        <text class="shop-value">{{ shop.address }}</text>
      </view>
    </view>

    <view class="footer">
      <text class="footer-text">若依扫码点单 · v1.0.0</text>
    </view>
  </view>
</template>

<script>
import { customerLogout } from '@/api/auth'
import { useCartStore, useUserStore } from '@/store'
import { SHOP_INFO, STORAGE_KEYS } from '@/config'
import { isStaffLogin, clearStaffLogin } from '@/utils/auth'
import { resolveImage } from '@/utils'

export default {
  data() {
    return {
      shop: SHOP_INFO,
      avatarError: false,
      staffLogged: false
    }
  },

  computed: {
    userStore() {
      return useUserStore()
    },

    cartStore() {
      return useCartStore()
    },

    avatar() {
      if (this.avatarError || !this.userStore.avatarUrl) return '/static/default-avatar.png'
      return resolveImage(this.userStore.avatarUrl)
    }
  },

  onShow() {
    this.staffLogged = isStaffLogin()
  },

  methods: {
    goLogin() {
      uni.navigateTo({
        url: `/pages/login/index?redirect=${encodeURIComponent('/pages/mine/index')}`
      })
    },

    goOrders(status) {
      // tabBar 页面不能带参数，通过本地缓存把目标筛选页签传过去
      // 'all' 作为哨兵值，避免空字符串被当成“未设置”
      uni.setStorageSync('order_list_tab', status || 'all')
      uni.switchTab({ url: '/pages/order-list/index' })
    },

    goMenu() {
      uni.switchTab({ url: '/pages/menu/index' })
    },

    goStaff() {
      if (isStaffLogin()) {
        uni.navigateTo({ url: '/pages/staff/index' })
      } else {
        uni.navigateTo({ url: '/pages/staff/login' })
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

    clearCache() {
      uni.showModal({
        title: '清除缓存',
        content: '将清除登录状态与购物车，确定继续？',
        success: res => {
          if (!res.confirm) return
          this.cartStore.clear()
          this.cartStore.setTableNo('')
          this.userStore.logout()
          clearStaffLogin()
          this.staffLogged = false
          uni.showToast({ title: '已清除', icon: 'none' })
        }
      })
    },

    logout() {
      uni.showModal({
        title: '退出登录',
        content: '确定退出当前账号吗？',
        success: async res => {
          if (!res.confirm) return
          try {
            await customerLogout()
          } catch (e) {
            // 即使后端退出失败也清理本地登录态
          }
          this.userStore.logout()
          uni.removeStorageSync(STORAGE_KEYS.CUSTOMER_TOKEN)
          uni.showToast({ title: '已退出', icon: 'none' })
        }
      })
    }
  }
}
</script>

<style scoped>
.mine-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 40rpx;
}

/* ===== 用户卡片 ===== */
.user-card {
  display: flex;
  align-items: center;
  padding: 48rpx 32rpx 60rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.6);
  background: #fff;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.nickname {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
}

.user-sub {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 8rpx;
}

.login-btn,
.logout-btn {
  flex-shrink: 0;
  padding: 12rpx 32rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.login-btn {
  background: #fff;
  color: #ff6b35;
  font-weight: 600;
}

.logout-btn {
  border: 2rpx solid rgba(255, 255, 255, 0.7);
  color: #fff;
}

/* ===== 卡片 ===== */
.card {
  background: #fff;
  border-radius: 20rpx;
  margin: 20rpx;
  padding: 28rpx;
}

.card:first-of-type {
  margin-top: -30rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
}

.card-more {
  font-size: 22rpx;
  color: #999;
}

/* ===== 订单宫格 ===== */
.order-grid {
  display: flex;
}

.order-grid-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.grid-icon {
  font-size: 44rpx;
  line-height: 1.4;
}

.grid-label {
  font-size: 22rpx;
  color: #666;
  margin-top: 8rpx;
}

/* ===== 列表行 ===== */
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f7f7f7;
}

.row:last-child {
  border-bottom: none;
}

.row-left {
  display: flex;
  align-items: center;
}

.row-icon {
  font-size: 34rpx;
  margin-right: 18rpx;
}

.row-label {
  font-size: 28rpx;
  color: #333;
}

.row-right {
  display: flex;
  align-items: center;
}

.row-value {
  font-size: 24rpx;
  color: #999;
  margin-right: 8rpx;
}

.row-arrow {
  font-size: 32rpx;
  color: #ccc;
}

/* ===== 门店信息 ===== */
.shop-card .card-title {
  margin-bottom: 18rpx;
}

.shop-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 10rpx 0;
}

.shop-label {
  font-size: 24rpx;
  color: #999;
  flex-shrink: 0;
}

.shop-value {
  font-size: 24rpx;
  color: #333;
  margin-left: 20rpx;
  text-align: right;
}

/* ===== 底部 ===== */
.footer {
  text-align: center;
  padding: 20rpx 0;
}

.footer-text {
  font-size: 20rpx;
  color: #c5c8cc;
}
</style>

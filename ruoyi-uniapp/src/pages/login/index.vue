<template>
  <view class="login-page">
    <view class="logo-wrap">
      <image src="/static/logo.png" mode="aspectFit" class="logo" />
    </view>

    <text class="title">若依饮品</text>
    <text class="subtitle">扫码点单 · 现做现取</text>

    <view class="feature-row">
      <view class="feature-item">
        <text class="feature-icon">🧋</text>
        <text class="feature-text">口味随心配</text>
      </view>
      <view class="feature-item">
        <text class="feature-icon">🎫</text>
        <text class="feature-text">取餐码叫号</text>
      </view>
      <view class="feature-item">
        <text class="feature-icon">⚡</text>
        <text class="feature-text">下单即制作</text>
      </view>
    </view>

    <view class="login-box">
      <view class="login-btn" :class="{ disabled: loading }" @click="handleLogin">
        <text class="wx-icon">💬</text>
        <text>{{ loading ? '登录中...' : '微信一键登录' }}</text>
      </view>

      <view class="skip-btn" @click="skipLogin">先随便逛逛</view>

      <view class="agreement" @click="agreed = !agreed">
        <view :class="['checkbox', { checked: agreed }]">
          <text v-if="agreed" class="check-icon">✓</text>
        </view>
        <text class="agreement-text">
          我已阅读并同意
          <text class="agreement-link" @click.stop="showAgreement('user')">《用户协议》</text>
          和
          <text class="agreement-link" @click.stop="showAgreement('privacy')">《隐私政策》</text>
        </text>
      </view>
    </view>

    <view class="staff-entry" @click="goStaffLogin">
      <text class="staff-text">我是店员，进入工作台 ›</text>
    </view>
  </view>
</template>

<script>
import { wxLogin } from '@/api/auth'
import { useUserStore } from '@/store'
import { STORAGE_KEYS } from '@/config'

export default {
  data() {
    return {
      loading: false,
      agreed: true,
      redirect: ''
    }
  },

  onLoad(options) {
    this.redirect = options.redirect ? decodeURIComponent(options.redirect) : ''
  },

  methods: {
    /**
     * 获取微信登录 code
     * 微信小程序走 uni.login；H5/App 调试时用一个稳定的本地 mock code，
     * 配合后端 wx.miniapp.mock-enabled=true 即可跑通全流程。
     */
    getWxCode() {
      return new Promise((resolve, reject) => {
        // #ifdef MP-WEIXIN
        uni.login({
          provider: 'weixin',
          success: res => {
            if (res.code) resolve(res.code)
            else reject(new Error('未获取到微信登录凭证'))
          },
          fail: err => reject(err)
        })
        // #endif

        // #ifndef MP-WEIXIN
        let code = uni.getStorageSync(STORAGE_KEYS.MOCK_CODE)
        if (!code) {
          code = `h5_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`
          uni.setStorageSync(STORAGE_KEYS.MOCK_CODE, code)
        }
        resolve(code)
        // #endif
      })
    },

    async handleLogin() {
      if (this.loading) return
      if (!this.agreed) {
        uni.showToast({ title: '请先阅读并同意用户协议', icon: 'none' })
        return
      }

      this.loading = true
      try {
        const code = await this.getWxCode()
        const res = await wxLogin(code, '', '')
        const token = res.token || (res.data && res.data.token)
        const userInfo = {
          wxUserId: res.wxUserId,
          nickName: res.nickName || '微信用户',
          avatarUrl: res.avatarUrl || ''
        }
        if (!token) throw new Error('登录返回缺少 token')

        useUserStore().login(token, userInfo)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => this.afterLogin(), 500)
      } catch (e) {
        uni.showToast({ title: e.message || e.msg || '登录失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    afterLogin() {
      const redirect = this.redirect
      if (!redirect) {
        uni.switchTab({ url: '/pages/menu/index' })
        return
      }
      const tabPages = ['/pages/menu/index', '/pages/order-list/index', '/pages/mine/index']
      if (tabPages.includes(redirect)) {
        uni.switchTab({ url: redirect })
      } else {
        // 用 redirectTo 替换登录页，返回时可回到上一页
        uni.redirectTo({
          url: redirect,
          fail: () => uni.switchTab({ url: '/pages/menu/index' })
        })
      }
    },

    skipLogin() {
      const pages = getCurrentPages()
      if (pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.switchTab({ url: '/pages/menu/index' })
      }
    },

    goStaffLogin() {
      uni.navigateTo({ url: '/pages/staff/login' })
    },

    showAgreement(type) {
      const isUser = type === 'user'
      uni.showModal({
        title: isUser ? '用户协议' : '隐私政策',
        content: isUser
          ? '本小程序用于门店扫码点单。下单后请按取餐码取餐；如需取消订单，请在制作前操作，已支付订单请联系店员处理。'
          : '我们仅收集完成点单所必需的微信标识与订单信息，不向第三方出售或共享。支付由微信支付或门店线下完成。',
        showCancel: false,
        confirmText: '我知道了'
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff3ec 0%, #ffffff 55%, #ffffff 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 48rpx 60rpx;
}

.logo-wrap {
  width: 180rpx;
  height: 180rpx;
  border-radius: 48rpx;
  background: #fff;
  box-shadow: 0 12rpx 40rpx rgba(255, 107, 53, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo {
  width: 140rpx;
  height: 140rpx;
}

.title {
  font-size: 48rpx;
  font-weight: 700;
  color: #222;
  margin-top: 36rpx;
}

.subtitle {
  font-size: 26rpx;
  color: #999;
  margin-top: 12rpx;
}

.feature-row {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-top: 70rpx;
}

.feature-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.feature-icon {
  font-size: 48rpx;
  line-height: 1.4;
}

.feature-text {
  font-size: 22rpx;
  color: #888;
  margin-top: 8rpx;
}

.login-box {
  width: 100%;
  margin-top: 80rpx;
}

.login-btn {
  width: 100%;
  height: 96rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 26rpx rgba(255, 107, 53, 0.3);
}

.login-btn.disabled {
  opacity: 0.7;
}

.wx-icon {
  font-size: 36rpx;
  margin-right: 12rpx;
}

.skip-btn {
  text-align: center;
  font-size: 26rpx;
  color: #999;
  margin-top: 30rpx;
  padding: 12rpx;
}

.agreement {
  display: flex;
  align-items: flex-start;
  margin-top: 36rpx;
}

.checkbox {
  width: 30rpx;
  height: 30rpx;
  border-radius: 50%;
  border: 2rpx solid #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2rpx;
  flex-shrink: 0;
}

.checkbox.checked {
  background: #ff6b35;
  border-color: #ff6b35;
}

.check-icon {
  font-size: 20rpx;
  color: #fff;
  line-height: 1;
}

.agreement-text {
  font-size: 22rpx;
  color: #999;
  margin-left: 12rpx;
  line-height: 1.6;
}

.agreement-link {
  color: #ff6b35;
}

.staff-entry {
  margin-top: auto;
  padding: 40rpx 0;
}

.staff-text {
  font-size: 24rpx;
  color: #aaa;
}
</style>

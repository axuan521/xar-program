<template>
  <view class="staff-login-page">
    <view class="header">
      <image src="/static/logo.png" mode="aspectFit" class="logo" />
      <text class="title">店员工作台</text>
      <text class="subtitle">使用若依后台账号登录</text>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="field-label">账号</text>
        <input
          v-model="form.username"
          class="field-input"
          placeholder="请输入后台账号"
          placeholder-class="placeholder"
          :adjust-position="true"
        />
      </view>

      <view class="field">
        <text class="field-label">密码</text>
        <input
          v-model="form.password"
          class="field-input"
          password
          placeholder="请输入密码"
          placeholder-class="placeholder"
          :adjust-position="true"
          @confirm="handleLogin"
        />
      </view>

      <view v-if="captchaEnabled" class="field captcha-field">
        <text class="field-label">验证码</text>
        <input
          v-model="form.code"
          class="field-input captcha-input"
          placeholder="请输入验证码"
          placeholder-class="placeholder"
        />
        <image
          v-if="captchaImg"
          :src="captchaImg"
          mode="aspectFit"
          class="captcha-img"
          @click="loadCaptcha"
        />
      </view>

      <view class="login-btn" :class="{ disabled: loading }" @click="handleLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </view>

      <view class="tips">
        <text class="tips-text">店员账号由管理员在「系统管理 - 用户管理」中创建</text>
        <text class="tips-text">并分配「点单管理」相关菜单权限</text>
      </view>
    </view>

    <view class="back-entry" @click="backToCustomer">
      <text class="back-text">‹ 返回顾客点单</text>
    </view>
  </view>
</template>

<script>
import { staffLogin, staffGetInfo, getCaptchaImage } from '@/api/auth'
import { setStaffLogin } from '@/utils/auth'

export default {
  data() {
    return {
      form: {
        username: '',
        password: '',
        code: '',
        uuid: ''
      },
      captchaEnabled: false,
      captchaImg: '',
      loading: false
    }
  },

  onLoad() {
    this.loadCaptcha()
  },

  methods: {
    async loadCaptcha() {
      try {
        const res = await getCaptchaImage()
        this.captchaEnabled = !!res.captchaEnabled
        this.form.uuid = res.uuid || ''
        this.captchaImg = res.img ? `data:image/gif;base64,${res.img}` : ''
      } catch (e) {
        // 后端未启动或接口异常时不阻塞登录表单展示
        this.captchaEnabled = false
      }
    },

    async handleLogin() {
      if (this.loading) return
      if (!this.form.username.trim()) {
        uni.showToast({ title: '请输入账号', icon: 'none' })
        return
      }
      if (!this.form.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }
      if (this.captchaEnabled && !this.form.code.trim()) {
        uni.showToast({ title: '请输入验证码', icon: 'none' })
        return
      }

      this.loading = true
      try {
        const res = await staffLogin({
          username: this.form.username.trim(),
          password: this.form.password,
          code: this.form.code.trim(),
          uuid: this.form.uuid
        })
        const token = res.token || (res.data && res.data.token)
        if (!token) throw new Error('登录失败，未获取到令牌')

        // 先保存 token，/getInfo 需要携带
        setStaffLogin(token, {})

        let info = {}
        try {
          info = (await staffGetInfo()) || {}
        } catch (e) {
          // 取不到权限信息时仍允许进入，工作台接口会做二次校验
        }

        const permissions = info.permissions || []
        const user = info.user || {}
        const hasShopPermission =
          permissions.includes('*:*:*') ||
          permissions.includes('shop:order:list') ||
          permissions.includes('shop:order:verify')

        if (permissions.length > 0 && !hasShopPermission) {
          setStaffLogin('', {})
          throw new Error('该账号没有点单管理权限，请联系管理员')
        }

        setStaffLogin(token, {
          userName: user.userName || this.form.username,
          nickName: user.nickName || this.form.username,
          avatar: user.avatar || '',
          permissions,
          roles: info.roles || []
        })

        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.redirectTo({ url: '/pages/staff/index' })
        }, 400)
      } catch (e) {
        uni.showToast({ title: e.message || e.msg || '登录失败', icon: 'none' })
        if (this.captchaEnabled) this.loadCaptcha()
      } finally {
        this.loading = false
      }
    },

    backToCustomer() {
      uni.switchTab({ url: '/pages/menu/index' })
    }
  }
}
</script>

<style scoped>
.staff-login-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding: 70rpx 40rpx;
}

.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 50rpx;
}

.logo {
  width: 130rpx;
  height: 130rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 700;
  color: #222;
  margin-top: 20rpx;
}

.subtitle {
  font-size: 24rpx;
  color: #999;
  margin-top: 10rpx;
}

.form-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.05);
}

.field {
  display: flex;
  align-items: center;
  border-bottom: 1rpx solid #f0f0f0;
  padding: 24rpx 0;
}

.field-label {
  width: 120rpx;
  font-size: 28rpx;
  color: #333;
  flex-shrink: 0;
}

.field-input {
  flex: 1;
  font-size: 28rpx;
  color: #222;
  height: 50rpx;
}

.placeholder {
  color: #bbb;
}

.captcha-field {
  border-bottom: none;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 200rpx;
  height: 72rpx;
  margin-left: 16rpx;
  border-radius: 8rpx;
  background: #f5f6f8;
}

.login-btn {
  margin-top: 40rpx;
  height: 92rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-btn.disabled {
  opacity: 0.7;
}

.tips {
  margin-top: 30rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.tips-text {
  font-size: 20rpx;
  color: #bbb;
  line-height: 1.8;
}

.back-entry {
  margin-top: 50rpx;
  text-align: center;
}

.back-text {
  font-size: 26rpx;
  color: #999;
}
</style>

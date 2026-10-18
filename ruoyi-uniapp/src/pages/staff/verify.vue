<template>
  <view class="verify-page">
    <!-- 扫码区 -->
    <view class="scan-card">
      <view class="scan-icon-wrap">
        <text class="scan-icon">📷</text>
      </view>
      <text class="scan-title">扫描顾客取餐码</text>
      <text class="scan-desc">对准顾客出示的取餐码 / 订单二维码</text>
      <view class="scan-btn" @click="scanCode">开始扫码</view>
    </view>

    <!-- 手动输入 -->
    <view class="manual-card">
      <text class="card-title">手动输入取餐码</text>
      <view class="input-row">
        <input
          v-model="pickupCode"
          class="code-input"
          placeholder="例如 A001"
          placeholder-class="placeholder"
          maxlength="10"
          @confirm="queryOrder"
        />
        <view class="query-btn" @click="queryOrder">查询</view>
      </view>
    </view>

    <!-- 查询结果 -->
    <view v-if="order" class="result-card">
      <view class="result-head">
        <view class="pickup-box">
          <text class="pickup-label">取餐码</text>
          <text class="pickup-code">{{ order.pickupCode }}</text>
        </view>
        <view class="result-status">
          <text :class="['pay-tag', order.payStatus === '1' ? 'paid' : 'unpaid']">
            {{ order.payStatus === '1' ? '已支付' : '待收款' }}
          </text>
          <text class="status-text">{{ statusText(order.orderStatus) }}</text>
        </view>
      </view>

      <view class="result-items">
        <view v-for="item in order.items" :key="item.itemId" class="result-item">
          <view class="item-line">
            <text class="item-name">{{ item.productName }}</text>
            <text class="item-qty">×{{ item.quantity }}</text>
          </view>
          <text v-if="item.flavorText" class="item-flavor">{{ item.flavorText }}</text>
        </view>
      </view>

      <view v-if="order.customerRemark" class="remark-box">
        <text class="remark-label">备注</text>
        <text class="remark-text">{{ order.customerRemark }}</text>
      </view>

      <view class="result-amount">
        <text class="amount-label">订单金额</text>
        <text class="amount-value">¥{{ formatMoney(order.totalAmount) }}</text>
      </view>

      <view class="result-actions">
        <view v-if="order.payStatus !== '1'" class="action primary" @click="collect">
          确认收款
        </view>
        <template v-else>
          <view v-if="order.orderStatus === '1'" class="action primary" @click="changeStatus('2', '已开始制作')">
            开始制作
          </view>
          <view v-else-if="order.orderStatus === '2'" class="action success" @click="changeStatus('3', '制作完成')">
            制作完成
          </view>
          <view v-else-if="order.orderStatus === '3'" class="action success" @click="finishPickup">
            确认取餐
          </view>
          <view v-else class="action disabled">该订单无需处理</view>
        </template>
      </view>
    </view>

    <view v-else-if="searched" class="no-result">
      <text class="no-result-icon">🔍</text>
      <text class="no-result-text">未找到取餐码 {{ searchedCode }} 对应的订单</text>
      <text class="no-result-desc">请确认取餐码是否为今日订单，或让顾客在小程序订单详情中查看</text>
    </view>

    <!-- 最近核销 -->
    <view v-if="recent.length > 0" class="recent-card">
      <view class="recent-head">
        <text class="card-title no-margin">最近核销</text>
        <text class="recent-clear" @click="clearRecent">清空</text>
      </view>
      <view v-for="(item, index) in recent" :key="index" class="recent-item">
        <text class="recent-code">{{ item.pickupCode }}</text>
        <text class="recent-action">{{ item.action }}</text>
        <text class="recent-time">{{ item.time }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getOrderByPickupCode, verifyOrder, updateOrderStatus } from '@/api/staff'
import { isStaffLogin } from '@/utils/auth'
import { formatMoney, normalizePickupCode, formatTimeHM } from '@/utils'

export default {
  data() {
    return {
      pickupCode: '',
      order: null,
      searched: false,
      searchedCode: '',
      recent: []
    }
  },

  onLoad() {
    if (!isStaffLogin()) {
      uni.redirectTo({ url: '/pages/staff/login' })
      return
    }
    this.recent = uni.getStorageSync('shop_recent_verify') || []
  },

  methods: {
    formatMoney,

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

    scanCode() {
      uni.scanCode({
        onlyFromCamera: false,
        scanType: ['qrCode', 'barCode'],
        success: res => {
          const code = this.extractCode(res.result || res.code || '')
          if (!code) {
            uni.showToast({ title: '未识别到取餐码', icon: 'none' })
            return
          }
          this.pickupCode = code
          this.queryOrder()
        },
        fail: () => {
          // 用户取消扫码无需提示
        }
      })
    },

    /** 兼容纯取餐码、pickupCode=A001、URL 中带 code 的二维码 */
    extractCode(raw) {
      if (!raw) return ''
      let text = String(raw).trim()
      try {
        text = decodeURIComponent(text)
      } catch (e) {
        // 保持原样
      }

      const patterns = [
        /pickupCode=([A-Za-z0-9]+)/i,
        /[?&]code=([A-Za-z0-9]+)/i,
        /pickup\/([A-Za-z0-9]+)/i,
        /^([A-Za-z]\d{1,6})$/,
        /^(\d{1,6})$/
      ]
      for (const pattern of patterns) {
        const match = text.match(pattern)
        if (match) {
          let code = match[1].toUpperCase()
          // 纯数字补上 A 前缀，与后端 A001 规则一致
          if (/^\d+$/.test(code)) code = 'A' + code.padStart(3, '0')
          return code
        }
      }
      return text.toUpperCase().slice(0, 10)
    },

    async queryOrder() {
      const code = normalizePickupCode(this.pickupCode)
      if (!code) {
        uni.showToast({ title: '请输入取餐码', icon: 'none' })
        return
      }
      this.pickupCode = code
      uni.showLoading({ title: '查询中...' })
      try {
        this.order = await getOrderByPickupCode(code)
        this.searched = false
      } catch (e) {
        this.order = null
        this.searched = true
        this.searchedCode = code
        uni.showToast({ title: e.msg || '未找到订单', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },

    async collect() {
      uni.showModal({
        title: '确认收款',
        content: `确认已收取 ¥${formatMoney(this.order.totalAmount)}？`,
        confirmText: '已收款',
        success: async res => {
          if (!res.confirm) return
          uni.showLoading({ title: '处理中...' })
          try {
            await verifyOrder(this.order.orderId)
            uni.hideLoading()
            this.record('确认收款')
            uni.showToast({ title: '收款成功', icon: 'success' })
            await this.queryOrder()
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    },

    async changeStatus(status, tip) {
      uni.showLoading({ title: '处理中...' })
      try {
        await updateOrderStatus(this.order.orderId, status)
        uni.hideLoading()
        this.record(tip)
        uni.showToast({ title: tip, icon: 'none' })
        await this.queryOrder()
      } catch (e) {
        uni.hideLoading()
      }
    },

    finishPickup() {
      uni.showModal({
        title: '确认取餐',
        content: `确认顾客已取走「${this.order.pickupCode}」的订单？`,
        confirmText: '已取餐',
        success: async res => {
          if (!res.confirm) return
          uni.showLoading({ title: '处理中...' })
          try {
            await verifyOrder(this.order.orderId)
            uni.hideLoading()
            this.record('取餐完成')
            uni.showToast({ title: '取餐完成', icon: 'success' })
            this.order = null
            this.pickupCode = ''
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    },

    record(action) {
      this.recent.unshift({
        pickupCode: this.order.pickupCode,
        action,
        time: formatTimeHM(new Date())
      })
      this.recent = this.recent.slice(0, 10)
      uni.setStorageSync('shop_recent_verify', this.recent)
    },

    clearRecent() {
      this.recent = []
      uni.removeStorageSync('shop_recent_verify')
    }
  }
}
</script>

<style scoped>
.verify-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding: 20rpx;
}

/* ===== 扫码卡 ===== */
.scan-card {
  background: linear-gradient(160deg, #2b2b2b 0%, #454545 100%);
  border-radius: 24rpx;
  padding: 44rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.scan-icon-wrap {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
}

.scan-icon {
  font-size: 54rpx;
}

.scan-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #fff;
  margin-top: 22rpx;
}

.scan-desc {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 10rpx;
}

.scan-btn {
  margin-top: 30rpx;
  width: 320rpx;
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

/* ===== 手动输入 ===== */
.manual-card,
.result-card,
.recent-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-top: 20rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
  margin-bottom: 20rpx;
  display: block;
}

.card-title.no-margin {
  margin-bottom: 0;
}

.input-row {
  display: flex;
  align-items: center;
}

.code-input {
  flex: 1;
  height: 84rpx;
  background: #f5f6f8;
  border-radius: 14rpx;
  padding: 0 24rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #222;
  letter-spacing: 4rpx;
}

.placeholder {
  color: #bbb;
  font-weight: 400;
  letter-spacing: 0;
}

.query-btn {
  width: 150rpx;
  height: 84rpx;
  margin-left: 16rpx;
  border-radius: 14rpx;
  background: #ff6b35;
  color: #fff;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== 查询结果 ===== */
.result-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.pickup-box {
  display: flex;
  flex-direction: column;
  background: #fff4ee;
  border-radius: 16rpx;
  padding: 16rpx 28rpx;
}

.pickup-label {
  font-size: 20rpx;
  color: #c98a6a;
}

.pickup-code {
  font-size: 56rpx;
  font-weight: 700;
  color: #ff6b35;
  letter-spacing: 6rpx;
  line-height: 1.1;
}

.result-status {
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

.status-text {
  font-size: 24rpx;
  color: #666;
  margin-top: 10rpx;
}

.result-items {
  margin-top: 20rpx;
  border-top: 1rpx dashed #f0f0f0;
  padding-top: 16rpx;
}

.result-item {
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
}

.remark-text {
  font-size: 22rpx;
  color: #ad6800;
  flex: 1;
}

.result-amount {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f5f5f5;
}

.amount-label {
  font-size: 24rpx;
  color: #999;
}

.amount-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff6b35;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20rpx;
}

.action {
  height: 74rpx;
  padding: 0 44rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
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

.action.disabled {
  background: #f0f0f0;
  color: #999;
}

/* ===== 无结果 ===== */
.no-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 90rpx 40rpx;
}

.no-result-icon {
  font-size: 80rpx;
}

.no-result-text {
  font-size: 28rpx;
  color: #555;
  margin-top: 16rpx;
}

.no-result-desc {
  font-size: 22rpx;
  color: #aaa;
  text-align: center;
  margin-top: 10rpx;
  line-height: 1.6;
}

/* ===== 最近核销 ===== */
.recent-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.recent-clear {
  font-size: 22rpx;
  color: #999;
}

.recent-item {
  display: flex;
  align-items: center;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #f7f7f7;
}

.recent-item:last-child {
  border-bottom: none;
}

.recent-code {
  font-size: 30rpx;
  font-weight: 700;
  color: #ff6b35;
  width: 140rpx;
}

.recent-action {
  flex: 1;
  font-size: 24rpx;
  color: #666;
}

.recent-time {
  font-size: 22rpx;
  color: #bbb;
}
</style>

<template>
  <view class="detail-page">
    <!-- 商品大图 -->
    <view class="hero">
      <image
        :src="heroImage"
        mode="aspectFill"
        class="hero-image"
        @error="imageError = true"
      />
      <view v-if="product.isHot === '1'" class="hero-tag">热销推荐</view>
    </view>

    <!-- 基础信息 -->
    <view class="info-card">
      <text class="product-name">{{ product.productName }}</text>
      <text v-if="product.description" class="product-desc">{{ product.description }}</text>

      <view class="price-row">
        <view class="price-box">
          <text class="price-symbol">¥</text>
          <text class="price-value">{{ formatMoney(unitPrice) }}</text>
        </view>
        <text v-if="flavorAmount > 0" class="price-hint">含口味加价 ¥{{ formatMoney(flavorAmount) }}</text>
      </view>
    </view>

    <!-- 口味规格 -->
    <view v-if="flavorGroups.length > 0" class="flavor-card">
      <view class="card-title">选择规格</view>

      <view v-for="group in flavorGroups" :key="group.groupName" class="flavor-group">
        <view class="group-head">
          <text class="group-name">{{ group.groupName }}</text>
          <text class="group-tip">{{ isMultiGroup(group.groupName) ? '可多选' : '单选' }}</text>
        </view>

        <view class="option-list">
          <view
            v-for="flavor in group.items"
            :key="flavor.flavorId"
            :class="['option', { active: isSelected(group.groupName, flavor.flavorId) }]"
            @click="toggleFlavor(group.groupName, flavor)"
          >
            <text class="option-name">{{ flavor.flavorName }}</text>
            <text v-if="Number(flavor.extraPrice) > 0" class="option-extra">
              +¥{{ formatMoney(flavor.extraPrice) }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 数量 -->
    <view class="quantity-card">
      <text class="card-title no-margin">购买数量</text>
      <view class="stepper">
        <view :class="['step-btn', { disabled: quantity <= 1 }]" @click="decrease">−</view>
        <text class="step-value">{{ quantity }}</text>
        <view class="step-btn plus" @click="increase">+</view>
      </view>
    </view>

    <!-- 已选摘要 -->
    <view v-if="selectedText" class="summary-card">
      <text class="summary-label">已选</text>
      <text class="summary-value">{{ selectedText }}</text>
    </view>

    <view class="bottom-placeholder-lg"></view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar safe-bottom">
      <view class="bottom-price">
        <text class="bottom-price-label">合计</text>
        <view class="price-box">
          <text class="price-symbol">¥</text>
          <text class="price-value lg">{{ formatMoney(totalPrice) }}</text>
        </view>
      </view>
      <view class="add-cart-btn" @click="addToCart">
        <text>加入购物车</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getProductDetail } from '@/api/product'
import { useCartStore } from '@/store'
import { formatMoney, resolveImage, sumMoney } from '@/utils'

export default {
  data() {
    return {
      productId: null,
      product: {},
      flavorGroups: [],
      /** { 组名: [flavorId, ...] } */
      selected: {},
      quantity: 1,
      imageError: false,
      loading: false
    }
  },

  computed: {
    heroImage() {
      if (this.imageError || !this.product.productImage) return '/static/default-product.png'
      return resolveImage(this.product.productImage)
    },

    /** 选中口味加价合计 */
    flavorAmount() {
      let total = 0
      this.flavorGroups.forEach(group => {
        const ids = this.selected[group.groupName] || []
        group.items.forEach(item => {
          if (ids.includes(item.flavorId)) total += Number(item.extraPrice || 0)
        })
      })
      return sumMoney([total])
    },

    unitPrice() {
      return sumMoney([Number(this.product.price || 0), this.flavorAmount])
    },

    totalPrice() {
      return sumMoney([this.unitPrice * this.quantity])
    },

    selectedText() {
      const texts = []
      this.flavorGroups.forEach(group => {
        const ids = this.selected[group.groupName] || []
        group.items.forEach(item => {
          if (ids.includes(item.flavorId)) texts.push(item.flavorName)
        })
      })
      return texts.join(' / ')
    }
  },

  onLoad(options) {
    this.productId = options.productId
    this.loadProduct()
  },

  methods: {
    formatMoney,

    async loadProduct() {
      if (!this.productId) return
      this.loading = true
      try {
        const data = await getProductDetail(this.productId)
        this.product = data || {}
        this.buildGroups(this.product.flavors || [])
      } catch (e) {
        uni.showToast({ title: '商品加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    /** 按 groupName 聚合口味，保持后端给的组顺序与组内顺序 */
    buildGroups(flavors) {
      const map = new Map()
      flavors.forEach(flavor => {
        const key = flavor.groupName || '规格'
        if (!map.has(key)) {
          map.set(key, {
            groupName: key,
            groupOrderNum: flavor.groupOrderNum || 0,
            items: []
          })
        }
        map.get(key).items.push(flavor)
      })

      this.flavorGroups = Array.from(map.values())
        .sort((a, b) => a.groupOrderNum - b.groupOrderNum)
        .map(group => {
          group.items.sort((a, b) => (a.orderNum || 0) - (b.orderNum || 0))
          return group
        })

      // 初始化默认选中：优先 isDefault=1，否则选第一个
      const selected = {}
      this.flavorGroups.forEach(group => {
        if (this.isMultiGroup(group.groupName)) {
          const defaults = group.items.filter(i => i.isDefault === '1').map(i => i.flavorId)
          selected[group.groupName] = defaults
        } else {
          const def = group.items.find(i => i.isDefault === '1') || group.items[0]
          selected[group.groupName] = def ? [def.flavorId] : []
        }
      })
      this.selected = selected
    },

    /** 加料类分组允许多选 */
    isMultiGroup(groupName) {
      return /加料|小料|配料|加购|topping/i.test(groupName || '')
    },

    isSelected(groupName, flavorId) {
      return (this.selected[groupName] || []).includes(flavorId)
    },

    toggleFlavor(groupName, flavor) {
      const current = this.selected[groupName] || []
      if (this.isMultiGroup(groupName)) {
        const next = current.includes(flavor.flavorId)
          ? current.filter(id => id !== flavor.flavorId)
          : [...current, flavor.flavorId]
        this.selected = { ...this.selected, [groupName]: next }
        return
      }
      // 单选组：再次点击已选项不做取消，避免出现“未选规格”的异常订单
      this.selected = { ...this.selected, [groupName]: [flavor.flavorId] }
    },

    decrease() {
      if (this.quantity > 1) this.quantity--
    },

    increase() {
      if (this.quantity >= 99) return
      this.quantity++
    },

    addToCart() {
      if (!this.product.productId) return
      const flavorIds = []
      const flavorTexts = []

      this.flavorGroups.forEach(group => {
        const ids = this.selected[group.groupName] || []
        group.items.forEach(item => {
          if (ids.includes(item.flavorId)) {
            flavorIds.push(item.flavorId)
            const extra = Number(item.extraPrice || 0)
            flavorTexts.push(extra > 0 ? `${item.flavorName}(+${formatMoney(extra)})` : item.flavorName)
          }
        })
      })

      useCartStore().addItem({
        productId: this.product.productId,
        productName: this.product.productName,
        productImage: this.product.productImage,
        price: Number(this.product.price || 0),
        flavorIds,
        flavorText: flavorTexts.join(' / '),
        flavorAmount: this.flavorAmount,
        quantity: this.quantity
      })

      uni.showToast({ title: '已加入购物车', icon: 'none' })
      setTimeout(() => {
        uni.navigateBack()
      }, 500)
    }
  }
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f6f8;
}

.hero {
  position: relative;
  width: 100%;
  height: 520rpx;
  background: #fff;
}

.hero-image {
  width: 100%;
  height: 100%;
}

.hero-tag {
  position: absolute;
  left: 24rpx;
  top: 24rpx;
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7a45 100%);
  color: #fff;
  font-size: 20rpx;
  padding: 6rpx 18rpx;
  border-radius: 999rpx;
}

.info-card,
.flavor-card,
.quantity-card,
.summary-card {
  background: #fff;
  border-radius: 20rpx;
  margin: 20rpx;
  padding: 28rpx;
}

.info-card {
  margin-top: -36rpx;
  position: relative;
  z-index: 1;
}

.product-name {
  font-size: 38rpx;
  font-weight: 600;
  color: #222;
}

.product-desc {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 12rpx;
  line-height: 1.6;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-top: 20rpx;
}

.price-box {
  display: flex;
  align-items: baseline;
  color: #ff6b35;
}

.price-symbol {
  font-size: 24rpx;
  font-weight: 600;
}

.price-value {
  font-size: 44rpx;
  font-weight: 700;
  margin-left: 2rpx;
}

.price-value.lg {
  font-size: 46rpx;
}

.price-hint {
  font-size: 22rpx;
  color: #bbb;
  margin-left: 16rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
  margin-bottom: 20rpx;
}

.card-title.no-margin {
  margin-bottom: 0;
}

.flavor-group {
  margin-bottom: 28rpx;
}

.flavor-group:last-child {
  margin-bottom: 0;
}

.group-head {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.group-name {
  font-size: 26rpx;
  color: #666;
  font-weight: 500;
}

.group-tip {
  font-size: 20rpx;
  color: #bbb;
  margin-left: 12rpx;
}

.option-list {
  display: flex;
  flex-wrap: wrap;
}

.option {
  display: flex;
  align-items: baseline;
  padding: 14rpx 28rpx;
  margin: 0 16rpx 16rpx 0;
  background: #f5f6f8;
  border: 2rpx solid transparent;
  border-radius: 999rpx;
}

.option.active {
  background: #fff4ee;
  border-color: #ff6b35;
}

.option-name {
  font-size: 26rpx;
  color: #444;
}

.option.active .option-name {
  color: #ff6b35;
  font-weight: 500;
}

.option-extra {
  font-size: 20rpx;
  color: #ff8c5a;
  margin-left: 6rpx;
}

.quantity-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stepper {
  display: flex;
  align-items: center;
}

.step-btn {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #f5f6f8;
  color: #666;
  font-size: 34rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-btn.plus {
  background: #fff4ee;
  color: #ff6b35;
}

.step-btn.disabled {
  color: #ccc;
}

.step-btn:active {
  opacity: 0.7;
}

.step-value {
  min-width: 80rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
}

.summary-card {
  display: flex;
  align-items: flex-start;
}

.summary-label {
  font-size: 24rpx;
  color: #999;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.summary-value {
  font-size: 24rpx;
  color: #ff6b35;
  flex: 1;
  line-height: 1.5;
}

.bottom-bar {
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
  z-index: 10;
}

.bottom-price {
  display: flex;
  align-items: baseline;
}

.bottom-price-label {
  font-size: 24rpx;
  color: #999;
  margin-right: 10rpx;
}

.add-cart-btn {
  width: 280rpx;
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

.add-cart-btn:active {
  opacity: 0.85;
}
</style>

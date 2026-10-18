<template>
  <view class="menu-page">
    <!-- 门店信息 -->
    <view class="shop-header">
      <view class="shop-main">
        <view class="shop-title-row">
          <text class="shop-name">{{ shop.name }}</text>
          <text class="shop-tag">营业中</text>
        </view>
        <text class="shop-slogan">{{ shop.slogan }}</text>
      </view>
      <view class="header-right">
        <view class="scan-btn" @click="scanTableCode">
          <text class="scan-btn-icon">⌗</text>
          <text class="scan-btn-text">扫码</text>
        </view>
        <view class="table-chip" @click="editTableNo">
          <text class="table-chip-label">{{ cartStore.tableNo ? '桌号' : '未选桌号' }}</text>
          <text v-if="cartStore.tableNo" class="table-chip-value">{{ cartStore.tableNo }}</text>
        </view>
      </view>
    </view>

    <view class="notice-bar">
      <text class="notice-icon">📢</text>
      <text class="notice-text">{{ shop.notice }}</text>
    </view>

    <!-- 分类 + 商品 -->
    <view class="content">
      <scroll-view class="category-sidebar" scroll-y :scroll-into-view="'cat-' + currentCategory">
        <view
          v-for="cat in categories"
          :key="cat.categoryId"
          :id="'cat-' + cat.categoryId"
          :class="['category-item', { active: currentCategory === cat.categoryId }]"
          @click="selectCategory(cat.categoryId)"
        >
          <text class="category-name">{{ cat.categoryName }}</text>
          <view v-if="categoryCount(cat.categoryId) > 0" class="category-badge">
            {{ categoryCount(cat.categoryId) }}
          </view>
        </view>
      </scroll-view>

      <scroll-view class="product-scroll" scroll-y :scroll-top="scrollTop">
        <view class="product-list">
          <view class="category-title">{{ currentCategoryName }}</view>

          <view
            v-for="product in products"
            :key="product.productId"
            class="product-card"
            @click="openProduct(product)"
          >
            <view class="product-image-wrap">
              <image
                :src="productImage(product)"
                mode="aspectFill"
                class="product-image"
                @error="onImageError(product)"
              />
              <view v-if="product.isHot === '1'" class="hot-tag">热销</view>
            </view>

            <view class="product-info">
              <text class="product-name">{{ product.productName }}</text>
              <text v-if="product.description" class="product-desc">{{ product.description }}</text>

              <view class="product-meta">
                <text v-if="flavorGroupCount(product) > 0" class="meta-text">
                  {{ flavorGroupCount(product) }} 种规格可选
                </text>
                <text v-else class="meta-text">默认规格</text>
              </view>

              <view class="product-footer">
                <view class="price-box">
                  <text class="price-symbol">¥</text>
                  <text class="price-value">{{ formatMoney(product.price) }}</text>
                  <text v-if="minExtraPrice(product) > 0" class="price-from">
                    起
                  </text>
                </view>

                <view class="product-actions">
                  <view v-if="productCount(product.productId) > 0" class="count-chip">
                    {{ productCount(product.productId) }}
                  </view>
                  <view class="add-btn" @click.stop="quickAdd(product)">
                    <text class="add-icon">+</text>
                  </view>
                </view>
              </view>
            </view>
          </view>

          <view v-if="!loading && products.length === 0" class="empty-block">
            <text class="empty-icon">🍵</text>
            <text class="empty-text">该分类暂无在售商品</text>
          </view>

          <view class="list-bottom-tip">
            <text>— 已经到底啦 —</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 购物车悬浮栏 -->
    <view class="cart-bar" @click="goCart">
      <view class="cart-left">
        <view :class="['cart-icon-wrap', { empty: cartStore.totalCount === 0 }]">
          <text class="cart-icon">🛒</text>
          <view v-if="cartStore.totalCount > 0" class="cart-badge">{{ cartStore.totalCount }}</view>
        </view>
        <view class="cart-price-box">
          <view v-if="cartStore.totalCount > 0" class="cart-total">
            <text class="cart-total-symbol">¥</text>
            <text class="cart-total-value">{{ formatMoney(cartStore.totalPrice) }}</text>
          </view>
          <text v-else class="cart-empty-text">购物车是空的</text>
          <text class="cart-tip">{{ cartStore.tableNo ? '堂食 · ' + cartStore.tableNo + ' 号桌' : '请先选择桌号' }}</text>
        </view>
      </view>

      <view :class="['checkout-btn', { disabled: cartStore.totalCount === 0 }]" @click.stop="goCart">
        去结算
      </view>
    </view>
  </view>
</template>

<script>
import { getCategoryList, getProductList } from '@/api/product'
import { useCartStore } from '@/store'
import { SHOP_INFO } from '@/config'
import { formatMoney, resolveImage } from '@/utils'

export default {
  data() {
    return {
      shop: SHOP_INFO,
      categories: [],
      allProducts: [],
      currentCategory: null,
      loading: false,
      scrollTop: 0
    }
  },

  computed: {
    cartStore() {
      return useCartStore()
    },

    /** 当前分类下的商品 */
    products() {
      return this.allProducts.filter(p => p.categoryId === this.currentCategory)
    },

    currentCategoryName() {
      const cat = this.categories.find(c => c.categoryId === this.currentCategory)
      return cat ? cat.categoryName : ''
    },

    /** 分类 → 商品ID集合，用于统计各分类已选件数 */
    productIdsByCategory() {
      const map = {}
      this.allProducts.forEach(p => {
        if (!map[p.categoryId]) map[p.categoryId] = []
        map[p.categoryId].push(p.productId)
      })
      return map
    }
  },

  onLoad(options) {
    this.handleScene(options)
    this.loadMenu()
  },

  onPullDownRefresh() {
    this.loadMenu(true).finally(() => uni.stopPullDownRefresh())
  },

  methods: {
    formatMoney,

    /** 解析扫码进入时的桌号：支持 ?tableNo=8、?scene=8、scene 中带 tableNo=8 */
    handleScene(options) {
      if (!options) return
      const raw = options.tableNo || options.scene || options.table
      if (raw) {
        this.cartStore.setTableNo(this.parseTableNo(raw))
      }
    },

    /** 桌号可能被编码成 tableNo%3D8 或 table:8，这里统一清洗 */
    parseTableNo(raw) {
      let text = String(raw)
      try {
        text = decodeURIComponent(text)
      } catch (e) {
        // 保持原样
      }
      const matched = text.match(/(?:tableNo|table)[=:]([^&]+)/i)
      if (matched) text = matched[1]
      return text.replace(/^table[=:]/i, '').trim()
    },

    /** 在小程序内扫桌码，解析出桌号后直接点单 */
    scanTableCode() {
      uni.scanCode({
        onlyFromCamera: false,
        scanType: ['qrCode', 'barCode'],
        success: res => {
          const raw = res.result || res.path || ''
          const tableNo = this.parseTableNo(raw)
          if (tableNo) {
            this.cartStore.setTableNo(tableNo)
            uni.showToast({ title: `已切换到 ${tableNo} 号桌`, icon: 'none' })
          }
        },
        fail: () => {
          // 用户取消扫码，无需提示
        }
      })
    },

    /**
     * 一次性拉取分类与全部可售商品，切换分类时本地过滤，
     * 避免每次点分类都请求后端，也便于统计分类角标。
     */
    async loadMenu(refresh) {
      if (this.loading) return
      this.loading = true
      try {
        const [categories, products] = await Promise.all([getCategoryList(), getProductList()])
        this.categories = categories || []
        this.allProducts = (products || []).map(p => ({ ...p, imageError: false }))

        if (this.categories.length === 0) {
          this.currentCategory = null
          return
        }
        const keep = this.categories.find(c => c.categoryId === this.currentCategory)
        this.currentCategory = (keep || this.categories[0]).categoryId
        this.scrollTop = this.scrollTop === 0 ? 1 : 0
      } catch (e) {
        if (!refresh) {
          this.categories = []
          this.allProducts = []
        }
      } finally {
        this.loading = false
      }
    },

    selectCategory(categoryId) {
      if (this.currentCategory === categoryId) return
      this.currentCategory = categoryId
      this.scrollTop = this.scrollTop === 0 ? 1 : 0
    },

    /** 该分类在购物车中的件数 */
    categoryCount(categoryId) {
      const ids = this.productIdsByCategory[categoryId] || []
      const counts = this.cartStore.countByProduct
      return ids.reduce((sum, id) => sum + (counts[id] || 0), 0)
    },

    productCount(productId) {
      return this.cartStore.countByProduct[productId] || 0
    },

    /** 口味分组数量（同组多个选项只算一组） */
    flavorGroupCount(product) {
      const flavors = product.flavors || []
      return new Set(flavors.map(f => f.groupName)).size
    },

    /** 最低加价，用于展示“起” */
    minExtraPrice(product) {
      const flavors = product.flavors || []
      if (flavors.length === 0) return 0
      return Math.min(...flavors.map(f => Number(f.extraPrice || 0)))
    },

    productImage(product) {
      if (product.imageError || !product.productImage) {
        return '/static/default-product.png'
      }
      return resolveImage(product.productImage)
    },

    onImageError(product) {
      product.imageError = true
    },

    openProduct(product) {
      uni.navigateTo({
        url: `/pages/product-detail/index?productId=${product.productId}`
      })
    },

    /**
     * 快捷加购：
     * 有口味规格 → 进详情页选规格；无规格 → 直接加购
     */
    quickAdd(product) {
      const flavors = product.flavors || []
      if (flavors.length > 0) {
        this.openProduct(product)
        return
      }
      this.cartStore.addItem({
        productId: product.productId,
        productName: product.productName,
        productImage: product.productImage,
        price: Number(product.price),
        flavorIds: [],
        flavorText: '',
        flavorAmount: 0,
        quantity: 1
      })
      uni.showToast({ title: '已加入购物车', icon: 'none' })
      if (uni.vibrateShort) uni.vibrateShort({ type: 'light' })
    },

    editTableNo() {
      uni.showModal({
        title: '选择桌号',
        editable: true,
        placeholderText: '请输入桌号，如 8',
        content: this.cartStore.tableNo || '',
        success: res => {
          if (res.confirm) {
            this.cartStore.setTableNo((res.content || '').trim())
          }
        }
      })
    },

    goCart() {
      if (this.cartStore.totalCount === 0) {
        uni.showToast({ title: '请先选择商品', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/cart/index' })
    }
  }
}
</script>

<style scoped>
.menu-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6f8;
  overflow: hidden;
}

/* ===== 门店头部 ===== */
.shop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 28rpx 20rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
}

.shop-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.shop-title-row {
  display: flex;
  align-items: center;
}

.shop-name {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
  margin-right: 12rpx;
}

.shop-tag {
  font-size: 20rpx;
  color: #ff6b35;
  background: #fff;
  border-radius: 999rpx;
  padding: 2rpx 12rpx;
}

.shop-slogan {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 8rpx;
}

.header-right {
  display: flex;
  align-items: center;
}

.scan-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-right: 14rpx;
  padding: 6rpx 10rpx;
}

.scan-btn-icon {
  font-size: 34rpx;
  color: #fff;
  line-height: 1.1;
}

.scan-btn-text {
  font-size: 18rpx;
  color: rgba(255, 255, 255, 0.85);
}

.table-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.22);
  border-radius: 16rpx;
  padding: 10rpx 20rpx;
  min-width: 130rpx;
}

.table-chip-label {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.85);
}

.table-chip-value {
  font-size: 30rpx;
  font-weight: 600;
  color: #fff;
}

/* ===== 公告 ===== */
.notice-bar {
  display: flex;
  align-items: center;
  padding: 14rpx 28rpx;
  background: #fff7ef;
}

.notice-icon {
  font-size: 24rpx;
  margin-right: 10rpx;
}

.notice-text {
  font-size: 22rpx;
  color: #c96a2a;
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* ===== 内容区 ===== */
.content {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding-bottom: 120rpx;
}

.category-sidebar {
  width: 190rpx;
  height: 100%;
  background: #f0f1f3;
}

.category-item {
  position: relative;
  padding: 30rpx 16rpx 30rpx 24rpx;
  font-size: 26rpx;
  color: #666;
  line-height: 1.4;
}

.category-item.active {
  background: #fff;
  color: #ff6b35;
  font-weight: 600;
}

.category-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6rpx;
  height: 36rpx;
  background: #ff6b35;
  border-radius: 0 6rpx 6rpx 0;
}

.category-name {
  display: block;
}

.category-badge {
  position: absolute;
  top: 12rpx;
  right: 10rpx;
  min-width: 30rpx;
  height: 30rpx;
  line-height: 30rpx;
  padding: 0 8rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 18rpx;
  border-radius: 999rpx;
  text-align: center;
}

.product-scroll {
  flex: 1;
  height: 100%;
  background: #fff;
}

.product-list {
  padding: 20rpx 24rpx 40rpx;
}

.category-title {
  font-size: 26rpx;
  color: #999;
  margin: 8rpx 0 20rpx;
}

.product-card {
  display: flex;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.product-image-wrap {
  position: relative;
  width: 168rpx;
  height: 168rpx;
  border-radius: 16rpx;
  overflow: hidden;
  flex-shrink: 0;
  background: #f5f6f8;
}

.product-image {
  width: 168rpx;
  height: 168rpx;
}

.hot-tag {
  position: absolute;
  left: 0;
  top: 0;
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7a45 100%);
  color: #fff;
  font-size: 18rpx;
  padding: 2rpx 10rpx;
  border-radius: 0 0 12rpx 0;
}

.product-info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.product-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
  line-height: 1.3;
}

.product-desc {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.product-meta {
  margin-top: 8rpx;
}

.meta-text {
  font-size: 20rpx;
  color: #b0b0b0;
}

.product-footer {
  margin-top: auto;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.price-box {
  display: flex;
  align-items: baseline;
  color: #ff6b35;
}

.price-symbol {
  font-size: 22rpx;
  font-weight: 600;
}

.price-value {
  font-size: 36rpx;
  font-weight: 700;
  margin-left: 2rpx;
}

.price-from {
  font-size: 20rpx;
  color: #b0b0b0;
  margin-left: 6rpx;
}

.product-actions {
  display: flex;
  align-items: center;
}

.count-chip {
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  padding: 0 8rpx;
  margin-right: 12rpx;
  border-radius: 999rpx;
  background: #fff2eb;
  color: #ff6b35;
  font-size: 20rpx;
  text-align: center;
}

.add-btn {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-btn:active {
  transform: scale(0.92);
}

.add-icon {
  color: #fff;
  font-size: 34rpx;
  line-height: 1;
  font-weight: 500;
}

.empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 140rpx 0;
}

.empty-icon {
  font-size: 90rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 26rpx;
  color: #bbb;
}

.list-bottom-tip {
  text-align: center;
  font-size: 20rpx;
  color: #ccc;
  padding: 30rpx 0 10rpx;
}

/* ===== 购物车栏 ===== */
.cart-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 24rpx;
  height: 104rpx;
  background: #222;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  padding: 0 12rpx 0 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.18);
  z-index: 20;
}

.cart-left {
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
}

.cart-icon-wrap {
  position: relative;
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: #ff6b35;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 18rpx;
}

.cart-icon-wrap.empty {
  background: #4a4a4a;
}

.cart-icon {
  font-size: 36rpx;
  line-height: 1;
}

.cart-badge {
  position: absolute;
  top: -6rpx;
  right: -6rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  padding: 0 6rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 18rpx;
  border-radius: 999rpx;
  text-align: center;
  border: 2rpx solid #222;
}

.cart-price-box {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.cart-total {
  display: flex;
  align-items: baseline;
  color: #fff;
}

.cart-total-symbol {
  font-size: 22rpx;
}

.cart-total-value {
  font-size: 38rpx;
  font-weight: 700;
}

.cart-empty-text {
  font-size: 26rpx;
  color: #aaa;
}

.cart-tip {
  font-size: 20rpx;
  color: #999;
  margin-top: 2rpx;
}

.checkout-btn {
  width: 190rpx;
  height: 80rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkout-btn.disabled {
  background: #4a4a4a;
  color: #888;
}
</style>

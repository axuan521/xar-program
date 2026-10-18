<template>
  <view class="empty-state">
    <image 
      v-if="image" 
      :src="image" 
      mode="aspectFit" 
      class="empty-image" 
    />
    <text v-else class="empty-icon">{{ icon }}</text>
    <text class="empty-text">{{ text }}</text>
    <slot></slot>
  </view>
</template>

<script>
export default {
  name: 'EmptyState',
  props: {
    text: {
      type: String,
      default: '暂无数据'
    },
    image: {
      type: String,
      default: ''
    },
    icon: {
      type: String,
      default: '📭'
    }
  }
}
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
}

.empty-image {
  width: 240rpx;
  height: 240rpx;
  margin-bottom: 32rpx;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 32rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999999;
  text-align: center;
}
</style>

<template>
  <view v-if="loading" class="data-state" aria-live="polite"
    ><view class="skeleton" /><view class="skeleton short" /><text
      >正在加载，请稍候</text
    ></view
  >
  <view v-else-if="error" class="data-state error" aria-live="polite"
    ><view class="state-icon"><app-icon name="circle-alert" :size="40" /></view
    ><view class="state-title">加载未完成</view
    ><text class="state-description">{{ error }}</text
    ><button class="state-button" @click="$emit('retry')">
      重新加载
    </button></view
  >
  <view v-else-if="empty" class="data-state"
    ><view class="state-icon">≡</view
    ><view class="state-title">{{ title }}</view
    ><text class="state-description">{{ description }}</text
    ><button v-if="action" class="state-button" @click="$emit('action')">
      {{ action }}
    </button></view
  >
</template>
<script>
export default {
  props: {
    loading: Boolean,
    error: String,
    empty: Boolean,
    title: { type: String, default: "暂无记录" },
    description: { type: String, default: "有新记录时会显示在这里" },
    action: String,
  },
  emits: ["retry", "action"],
};
</script>
<style scoped>
.data-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 28rpx;
  background: #fff;
  border-radius: 20rpx;
  text-align: center;
  color: #526277;
  font-size: 26rpx;
  gap: 16rpx;
}
.state-icon {
  width: 72rpx;
  height: 72rpx;
  line-height: 72rpx;
  background: #edf3fa;
  color: #235a91;
  border-radius: 22rpx;
  font-size: 38rpx;
}
.error .state-icon {
  background: #fff1e8;
  color: #9c4819;
}
.state-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #23364d;
}
.state-description {
  line-height: 1.7;
  max-width: 100%;
  word-break: break-word;
}
.state-button {
  font-size: 28rpx;
  color: #235a91;
  background: #edf3fa;
  padding: 0 32rpx;
  margin: 8rpx 0 0;
  min-height: 80rpx;
  line-height: 80rpx;
  border-radius: 12rpx;
}
.state-button::after {
  border: none;
}
.skeleton {
  width: 100%;
  height: 28rpx;
  background: #e8eef5;
  border-radius: 8rpx;
}
.short {
  width: 65%;
  margin-bottom: 8rpx;
}
</style>

<template>
  <view class="loading-block">
    <view class="dots">
      <view class="d"></view><view class="d"></view><view class="d"></view>
    </view>
    <view class="t">{{ text }}</view>
  </view>
</template>

<script>
export default {
  name: "LoadingBlock",
  props: { text: { type: String, default: "加载中…" } },
};
</script>

<style lang="scss" scoped>
.loading-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx;
  color: #64748b;
  font-size: 24rpx;
}
.dots {
  display: flex;
  gap: 12rpx;
  margin-bottom: 16rpx;
}
.d {
  width: 12rpx;
  height: 12rpx;
  background: #1e5a96;
  border-radius: 50%;
  animation: bounce 1.2s infinite;
}
.d:nth-child(2) {
  animation-delay: 0.2s;
}
.d:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0.5);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>

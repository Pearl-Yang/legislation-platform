<template>
  <view class="page-heading">
    <view class="heading-copy"
      ><text class="heading-eyebrow">智立法 · 移动工作台</text
      ><view class="heading-title">{{ title }}</view
      ><view class="heading-description">{{ description }}</view></view
    >
    <view class="heading-mark" aria-hidden="true"
      ><app-icon :name="icon" :size="48"
    /></view>
  </view>
</template>
<script>
export default {
  props: {
    title: String,
    description: String,
    icon: { type: String, default: "scale" },
  },
};
</script>
<style scoped>
.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 36rpx 32rpx;
  background: #edf3fa;
  border-bottom: 1rpx solid #dce5ef;
}
.heading-copy {
  flex: 1;
  min-width: 0;
}
.heading-eyebrow {
  font-size: 24rpx;
  color: #536b87;
  letter-spacing: 2rpx;
}
.heading-title {
  margin: 12rpx 0 8rpx;
  font-size: 42rpx;
  font-weight: 700;
  color: #163b64;
}
.heading-description {
  font-size: 26rpx;
  line-height: 1.6;
  color: #536278;
}
.heading-mark {
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #dce8f5;
  color: #235a91;
  font-size: 42rpx;
  border-radius: 24rpx;
  flex-shrink: 0;
}
</style>

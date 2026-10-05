<template>
  <view class="empty">
    <view class="empty-icon">{{ icon }}</view>
    <view class="empty-text">{{ text }}</view>
    <view v-if="desc" class="empty-desc">{{ desc }}</view>
    <slot />
  </view>
</template>

<script>
export default {
  name: 'Empty',
  props: {
    icon:  { type: String, default: '📭' },
    text:  { type: String, default: '暂无数据' },
    desc:  { type: String, default: '' }
  }
}
</script>

<style lang="scss" scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 96rpx 32rpx;
  color: #909399;
}
.empty-icon { font-size: 96rpx; margin-bottom: 24rpx; opacity: 0.7; }
.empty-text { font-size: 30rpx; color: #6b7280; }
.empty-desc { font-size: 24rpx; color: #909399; margin-top: 8rpx; }
</style>
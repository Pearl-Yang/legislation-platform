<template>
  <view class="seg-tabs">
    <button
      v-for="t in tabs"
      :key="t.value"
      :class="['seg-tab', modelValue === t.value ? 'seg-tab-active' : '']"
      @click="$emit('update:modelValue', t.value)"
    >
      <text>{{ t.label }}</text>
      <view v-if="t.badge" class="seg-badge">{{ t.badge }}</view>
    </button>
  </view>
</template>

<script>
export default {
  name: "SegTabs",
  props: {
    tabs: { type: Array, required: true },
    modelValue: { type: [String, Number], default: "" },
  },
  emits: ["update:modelValue"],
};
</script>

<style lang="scss" scoped>
.seg-tabs {
  display: flex;
  background: #f0f4f8;
  border-radius: 16rpx;
  padding: 4rpx;
  gap: 4rpx;
}
.seg-tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 8rpx;
  font-size: 26rpx;
  color: #6b7280;
  border-radius: 12rpx;
  position: relative;
}
.seg-tab-active {
  background: #fff;
  color: #1e5a96;
  font-weight: 600;
  box-shadow: 0 2rpx 8rpx rgba(30, 90, 150, 0.08);
}
.seg-badge {
  position: absolute;
  top: 4rpx;
  right: 16rpx;
  background: #a44342;
  color: #fff;
  font-size: 18rpx;
  padding: 0 8rpx;
  border-radius: 16rpx;
  min-width: 24rpx;
  line-height: 1.4;
}
button {
  min-height: 88rpx;
  line-height: 1.5;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
button::after {
  border: none;
}
</style>

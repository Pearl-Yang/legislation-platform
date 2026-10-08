<template>
  <button class="stat-card" @click="$emit('click')">
    <view class="stat-label"
      >{{ label
      }}<app-icon
        class="stat-arrow"
        name="arrow-up-right"
        :size="28"
        tone="muted" /></view
    ><view class="stat-value">{{ value == null ? "—" : value }}</view
    ><view class="stat-hint">{{ hint }}</view>
  </button>
</template>
<script>
export default {
  props: { label: String, value: [Number, String], hint: String },
  emits: ["click"],
};
</script>
<style scoped>
.stat-card {
  padding: 24rpx;
  margin: 0;
  background: #fff;
  border: 1rpx solid #e2e9f1;
  border-radius: 20rpx;
  text-align: left;
  line-height: 1.5;
  min-width: 0;
}
.stat-card::after {
  border: none;
}
.stat-label {
  font-size: 26rpx;
  color: #4f6278;
  display: flex;
  justify-content: space-between;
}
.stat-arrow {
  color: #70839a;
}
.stat-value {
  font-size: 52rpx;
  font-weight: 700;
  color: #163b64;
  margin: 8rpx 0;
}
.stat-hint {
  font-size: 24rpx;
  color: #617085;
}
</style>

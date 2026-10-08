<template>
  <view class="filter-pills">
    <button
      v-for="o in options"
      :key="o.value"
      :class="['pill', modelValue === o.value ? 'pill-active' : '']"
      @click="$emit('update:modelValue', o.value)"
    >
      {{ o.label }}
    </button>
  </view>
</template>

<script>
export default {
  name: "FilterPills",
  props: {
    options: { type: Array, required: true }, // [{label, value}]
    modelValue: { type: [String, Number], default: "" },
  },
  emits: ["update:modelValue"],
};
</script>

<style lang="scss" scoped>
.filter-pills {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #fff;
  overflow-x: auto;
  white-space: nowrap;
  border-bottom: 1rpx solid #f3f4f6;
}
.pill {
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
  font-size: 24rpx;
  color: #6b7280;
  background: #f5f7fa;
  flex-shrink: 0;
}
.pill-active {
  background: linear-gradient(135deg, #1e5a96, #4a86c5);
  color: #fff;
  font-weight: 600;
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

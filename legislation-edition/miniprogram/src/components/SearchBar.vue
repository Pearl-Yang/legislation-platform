<template>
  <view class="filter-bar"
    ><view class="search-wrap"
      ><app-icon
        class="search-icon"
        name="search"
        :size="32"
        tone="muted"
      /><input
        class="search-input"
        :value="modelValue"
        :placeholder="placeholder"
        confirm-type="search"
        @input="onInput"
        @confirm="search"
      /><button
        v-if="modelValue"
        class="search-clear"
        aria-label="清空搜索"
        @click="clear"
      >
        ×
      </button></view
    ><slot
  /></view>
</template>
<script>
export default {
  props: {
    placeholder: { type: String, default: "搜索关键词" },
    modelValue: { type: String, default: "" },
  },
  emits: ["update:modelValue", "search"],
  data: () => ({ timer: null }),
  beforeUnmount() {
    clearTimeout(this.timer);
  },
  methods: {
    onInput(e) {
      const value = e.detail.value;
      this.$emit("update:modelValue", value);
      clearTimeout(this.timer);
      this.timer = setTimeout(() => this.$emit("search", value), 400);
    },
    search() {
      clearTimeout(this.timer);
      this.$emit("search", this.modelValue);
    },
    clear() {
      clearTimeout(this.timer);
      this.$emit("update:modelValue", "");
      this.$emit("search", "");
    },
  },
};
</script>
<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  background: #fff;
}
.search-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
  background: #f0f4f8;
  border: 1rpx solid #e1e8f0;
  border-radius: 14rpx;
  padding: 0 20rpx;
  min-height: 88rpx;
}
.search-icon {
  font-size: 40rpx;
  color: #566d87;
  margin-right: 12rpx;
}
.search-input {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  color: #23364d;
  height: 84rpx;
}
.search-clear {
  width: 76rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 34rpx;
  background: transparent;
  color: #526277;
  padding: 0;
  margin: 0;
}
.search-clear::after {
  border: none;
}
</style>

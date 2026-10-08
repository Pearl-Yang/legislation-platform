<template>
  <view
    v-if="modelValue"
    class="sheet-mask"
    @click="$emit('update:modelValue', false)"
    ><view class="sheet-panel" @click.stop
      ><view class="sheet-heading"
        ><view class="sheet-title">{{ title }}</view
        ><button
          class="sheet-close"
          aria-label="关闭"
          @click="$emit('update:modelValue', false)"
        >
          ×
        </button></view
      ><scroll-view scroll-y class="sheet-content"><slot /></scroll-view></view
  ></view>
</template>
<script>
export default {
  props: { modelValue: Boolean, title: String },
  emits: ["update:modelValue"],
};
</script>
<style scoped>
.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(15, 35, 60, 0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.sheet-panel {
  width: 100%;
  max-width: 640px;
  background: #fff;
  border-radius: 28rpx 28rpx 0 0;
  padding: 28rpx 32rpx calc(28rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}
.sheet-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}
.sheet-title {
  font-size: 34rpx;
  font-weight: 650;
  color: #23364d;
}
.sheet-close {
  width: 80rpx;
  height: 80rpx;
  margin: 0;
  padding: 0;
  line-height: 80rpx;
  background: #edf3fa;
  color: #526277;
  font-size: 40rpx;
  border-radius: 50%;
}
.sheet-close::after {
  border: none;
}
.sheet-content {
  max-height: 65vh;
}
</style>

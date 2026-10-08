<template>
  <view class="section-header"
    ><view
      ><view class="section-title">{{ title }}</view
      ><text v-if="description" class="section-description">{{
        description
      }}</text></view
    ><button v-if="action" class="section-action" @click="$emit('action')">
      {{ action }} ›
    </button></view
  >
</template>
<script>
export default {
  props: { title: String, description: String, action: String },
  emits: ["action"],
};
</script>
<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 650;
  color: #23364d;
}
.section-description {
  font-size: 24rpx;
  line-height: 1.6;
  color: #617085;
}
.section-action {
  font-size: 26rpx;
  line-height: 76rpx;
  min-height: 76rpx;
  padding: 0 12rpx;
  margin: 0;
  background: transparent;
  color: #235a91;
  flex-shrink: 0;
}
.section-action::after {
  border: none;
}
</style>

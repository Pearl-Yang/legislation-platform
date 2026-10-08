<template>
  <view class="module-grid"
    ><button
      v-for="item in items"
      :key="item.path"
      class="module-item"
      @click="$emit('select', item.path)"
    >
      <view class="module-icon"><app-icon :name="item.icon" :size="42" /></view
      ><view class="module-title">{{ item.title }}</view
      ><view class="module-description">{{ item.description }}</view>
    </button></view
  >
</template>
<script>
export default {
  props: { items: { type: Array, default: () => [] } },
  emits: ["select"],
};
</script>
<style scoped>
.module-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24rpx 12rpx;
}
.module-item {
  min-width: 0;
  padding: 0;
  margin: 0;
  background: transparent;
  text-align: center;
  line-height: 1.4;
}
.module-item::after {
  border: none;
}
.module-icon {
  width: 76rpx;
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 22rpx;
  margin: 0 auto 12rpx;
  font-weight: 650;
  font-size: 32rpx;
  background: #edf3fa;
}
.module-title {
  font-size: 26rpx;
  color: #23364d;
  white-space: nowrap;
}
.module-description {
  font-size: 22rpx;
  color: #607087;
  margin-top: 6rpx;
}
@media (max-width: 340px) {
  .module-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (min-width: 960px) {
  .module-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }
  .module-item {
    padding: 20px 12px;
    border: 1px solid #e5ebf2;
    border-radius: 16px;
    background: #fff;
  }
  .module-item:hover {
    background: #f5f8fc;
    border-color: #c7d7e8;
  }
  .module-icon {
    width: 52px;
    height: 52px;
    margin-bottom: 12px;
  }
}
</style>

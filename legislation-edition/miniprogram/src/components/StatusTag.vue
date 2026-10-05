<template>
  <view :class="['status-tag', `tag-${type}`]" :style="{ color: color, background: bg }">{{ label }}</view>
</template>

<script>
export default {
  name: 'StatusTag',
  props: {
    type: { type: String, default: 'info' },           // preset 名字
    label:{ type: String, required: true },           // 显示文字
    color: { type: String, default: '' },             // 自定义前景
    bg:    { type: String, default: '' }              // 自定义背景
  }
}
</script>

<style lang="scss" scoped>
.status-tag {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  line-height: 1.6;
}
.tag-info     { background: #f0f4f8; color: #1e5a96; }
.tag-success  { background: #e8f7e6; color: #67c23a; }
.tag-warning  { background: #fdf6ec; color: #e6a23c; }
.tag-danger   { background: #fef0f0; color: #f56c6c; }
.tag-primary  { background: #ecf5fc; color: #1e5a96; }
.tag-grey     { background: #f4f4f5; color: #909399; }
</style>
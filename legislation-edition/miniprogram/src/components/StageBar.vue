<template>
  <view class="stage-bar">
    <view
      v-for="(s, i) in stages"
      :key="i"
      class="stage-cell"
      @click="$emit('tap', s)"
    >
      <view :class="['stage-dot', dotCls(s)]">
        <text v-if="s.status === 'DONE'" class="dot-ok">✓</text>
      </view>
      <view class="stage-line" v-if="i < stages.length - 1"></view>
      <view class="stage-name">{{ s.stageName }}</view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'StageBar',
  props: {
    stages: { type: Array, required: true } // [{ stageName, status, ... }]
  },
  emits: ['tap'],
  methods: {
    dotCls(s) {
      switch (s.status) {
        case 'DONE':        return 'dot-done'
        case 'IN_PROGRESS': return 'dot-doing'
        case 'RETURNED':    return 'dot-returned'
        case 'SKIPPED':     return 'dot-skipped'
        default:            return 'dot-pending'
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.stage-bar {
  display: flex;
  align-items: flex-start;
  padding: 24rpx 0;
}
.stage-cell {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  font-size: 22rpx;
  color: #6b7280;
}
.stage-dot {
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20rpx;
  font-weight: 700;
  z-index: 1;
  background: #d1d5db;
}
.dot-pending { background: #d1d5db; }
.dot-done    { background: #67c23a; }
.dot-doing   {
  background: #1e5a96;
  box-shadow: 0 0 0 6rpx rgba(30, 90, 150, 0.15);
}
.dot-returned{ background: #f56c6c; }
.dot-skipped { background: #909399; }
.dot-ok { line-height: 1; }
.stage-line {
  position: absolute;
  top: 14rpx;
  left: 50%;
  width: 100%;
  height: 4rpx;
  background: #e5e7eb;
  z-index: 0;
}
.stage-name {
  margin-top: 12rpx;
  word-break: break-all;
  line-height: 1.4;
  padding: 0 4rpx;
}
</style>
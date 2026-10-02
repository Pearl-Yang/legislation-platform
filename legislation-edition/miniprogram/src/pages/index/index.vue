<template>
  <view class="splash">
    <view class="bg-circle bg-circle-1"></view>
    <view class="bg-circle bg-circle-2"></view>

    <view class="content">
      <view class="brand-logo">智</view>
      <view class="brand-name">智立法</view>
      <view class="brand-subname">行政立法智能辅助平台</view>

      <view class="feature-list">
        <view class="feature-item">立项 · 起草 · 审查 · 公布 · 清理 · 评估</view>
        <view class="feature-item">AI 辅助立法 · 智慧审查 · 公众参与</view>
      </view>

      <view class="version">v0.2 · 演示版</view>
    </view>

    <view class="footer">
      <button class="enter-btn" @click="enterApp">进入工作台</button>
    </view>
  </view>
</template>

<script>
export default {
  data() { return {} },
  onLoad() {},
  methods: {
    enterApp() {
      uni.switchTab({ url: '/pages/dashboard/index' })
    }
  }
}
</script>

<style lang="scss" scoped>
.splash {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f2c4f 0%, #1e5a96 50%, #4a86c5 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 240rpx 48rpx 96rpx;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80rpx);
  opacity: 0.3;
  z-index: 0;
}
.bg-circle-1 { width: 480rpx; height: 480rpx; background: #4a86c5; top: -120rpx; left: -120rpx; }
.bg-circle-2 { width: 600rpx; height: 600rpx; background: #17a2b8; bottom: -200rpx; right: -150rpx; }

.content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand-logo {
  width: 160rpx;
  height: 160rpx;
  border-radius: 32rpx;
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(20rpx);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 80rpx;
  font-weight: 700;
  margin-bottom: 32rpx;
  box-shadow: 0 16rpx 40rpx rgba(0,0,0,0.2);
}

.brand-name {
  font-size: 56rpx;
  font-weight: 700;
  letter-spacing: 8rpx;
}
.brand-subname {
  font-size: 28rpx;
  opacity: 0.8;
  margin-top: 16rpx;
}

.feature-list {
  margin-top: 64rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.feature-item {
  font-size: 26rpx;
  background: rgba(255,255,255,0.12);
  padding: 12rpx 24rpx;
  border-radius: 24rpx;
  text-align: center;
}

.version {
  margin-top: 64rpx;
  font-size: 24rpx;
  opacity: 0.6;
}

.footer {
  position: relative;
  z-index: 1;
  width: 100%;
}
.enter-btn {
  background: #fff;
  color: #1e5a96;
  border-radius: 48rpx;
  font-size: 32rpx;
  font-weight: 600;
  height: 96rpx;
  line-height: 96rpx;
}
.enter-btn::after { border: none; }
</style>
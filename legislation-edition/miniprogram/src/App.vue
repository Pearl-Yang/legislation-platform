<template>
  <view class="app-container">
    <!-- 立法版 uni-app 根组件 -->
  </view>
</template>

<script>
export default {
  onLaunch() {
    console.log('App Launch - 智立法 v0.2')
    // 演示版：写入默认登录态
    if (!uni.getStorageSync('userName')) {
      uni.setStorageSync('userName', '立法管理员')
      uni.setStorageSync('userRole', '系统管理员')
      uni.setStorageSync('token', 'demo-token')
    }
  },
  onShow() { console.log('App Show') },
  onHide()  { console.log('App Hide') }
}
</script>

<style lang="scss">
/* 全局主题色 */
page {
  background-color: #f5f7fa;
  color: #1f2937;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 28rpx;
}

view { box-sizing: border-box; }

/* 通用工具类 */
.flex { display: flex; }
.flex-1 { flex: 1; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-center { display: flex; align-items: center; justify-content: center; }
.flex-col { display: flex; flex-direction: column; }
.gap-8  { gap: 16rpx; }
.gap-12 { gap: 24rpx; }
.gap-16 { gap: 32rpx; }
.mt-8  { margin-top: 16rpx; }
.mt-12 { margin-top: 24rpx; }
.mt-16 { margin-top: 32rpx; }
.mt-24 { margin-top: 48rpx; }
.mb-8  { margin-bottom: 16rpx; }
.mb-12 { margin-bottom: 24rpx; }
.mb-16 { margin-bottom: 32rpx; }
.mb-24 { margin-bottom: 48rpx; }

.text-primary   { color: #1e5a96; }
.text-secondary { color: #6b7280; }
.text-warning   { color: #e6a23c; }
.text-danger    { color: #f56c6c; }
.text-success   { color: #67c23a; }

.fz-12 { font-size: 24rpx; }
.fz-14 { font-size: 28rpx; }
.fz-16 { font-size: 32rpx; }
.fz-18 { font-size: 36rpx; }
.fz-20 { font-size: 40rpx; }
.fz-24 { font-size: 48rpx; }
.fw-500 { font-weight: 500; }
.fw-600 { font-weight: 600; }
.fw-700 { font-weight: 700; }

/* 卡片 */
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(15, 35, 60, 0.04);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid #f3f4f6;
  margin-bottom: 24rpx;
}
.card-title {
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
}
.card-title::before {
  content: '';
  display: inline-block;
  width: 8rpx;
  height: 32rpx;
  background: #1e5a96;
  border-radius: 4rpx;
  margin-right: 16rpx;
}

/* 流程节点圆点 */
.stage-dot {
  display: inline-block;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 12rpx;
}
.stage-dot.done       { background: #67c23a; }
.stage-dot.in-progress { background: #1e5a96; box-shadow: 0 0 0 8rpx rgba(30,90,150,0.15); }
.stage-dot.pending    { background: #d1d5db; }
.stage-dot.returned   { background: #f56c6c; }
.stage-dot.skipped    { background: #909399; }

/* 类型徽标 */
.type-badge {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  color: #fff;
}
.type-badge.admin { background: #1e5a96; }
.type-badge.dept  { background: #4a86c5; }
.type-badge.local { background: #67c23a; }

/* 严重度标签 */
.sev-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40rpx;
  height: 40rpx;
  border-radius: 8rpx;
  color: #fff;
  font-size: 22rpx;
  font-weight: 600;
}
.sev-badge.red    { background: #f56c6c; }
.sev-badge.yellow { background: #e6a23c; }
.sev-badge.blue   { background: #409eff; }
.sev-badge.grey   { background: #909399; }

/* 页面通用容器 */
.page-container {
  padding: 24rpx;
  min-height: calc(100vh - 200rpx);
}

.page-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}
.page-subtitle {
  font-size: 24rpx;
  color: #6b7280;
  margin-bottom: 32rpx;
}
</style>
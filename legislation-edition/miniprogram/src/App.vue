<template>
  <view class="app-container">
    <!-- 立法版 uni-app 根组件 -->
  </view>
</template>

<script>
export default {
  onLaunch() {
    console.log("App Launch - 智立法 v0.2");
    if (uni.getStorageSync("token") === "demo-token")
      uni.removeStorageSync("token");
  },
  onShow() {
    console.log("App Show");
  },
  onHide() {
    console.log("App Hide");
  },
};
</script>

<style lang="scss">
/* 全局主题色 */
page {
  background-color: #f5f7fa;
  color: #1f2937;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC",
    "Microsoft YaHei", sans-serif;
  font-size: 28rpx;
}

view {
  box-sizing: border-box;
}

/* 通用工具类 */
.flex {
  display: flex;
}
.flex-1 {
  flex: 1;
}
.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}
.flex-col {
  display: flex;
  flex-direction: column;
}
.gap-8 {
  gap: 16rpx;
}
.gap-12 {
  gap: 24rpx;
}
.gap-16 {
  gap: 32rpx;
}
.mt-8 {
  margin-top: 16rpx;
}
.mt-12 {
  margin-top: 24rpx;
}
.mt-16 {
  margin-top: 32rpx;
}
.mt-24 {
  margin-top: 48rpx;
}
.mb-8 {
  margin-bottom: 16rpx;
}
.mb-12 {
  margin-bottom: 24rpx;
}
.mb-16 {
  margin-bottom: 32rpx;
}
.mb-24 {
  margin-bottom: 48rpx;
}

.text-primary {
  color: #1e5a96;
}
.text-secondary {
  color: #6b7280;
}
.text-warning {
  color: #9b621f;
}
.text-danger {
  color: #a44342;
}
.text-success {
  color: #39734c;
}

.fz-12 {
  font-size: 24rpx;
}
.fz-14 {
  font-size: 28rpx;
}
.fz-16 {
  font-size: 32rpx;
}
.fz-18 {
  font-size: 36rpx;
}
.fz-20 {
  font-size: 40rpx;
}
.fz-24 {
  font-size: 48rpx;
}
.fw-500 {
  font-weight: 500;
}
.fw-600 {
  font-weight: 600;
}
.fw-700 {
  font-weight: 700;
}

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
  content: "";
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
.stage-dot.done {
  background: #39734c;
}
.stage-dot.in-progress {
  background: #1e5a96;
  box-shadow: 0 0 0 8rpx rgba(30, 90, 150, 0.15);
}
.stage-dot.pending {
  background: #d1d5db;
}
.stage-dot.returned {
  background: #a44342;
}
.stage-dot.skipped {
  background: #64748b;
}

/* 类型徽标 */
.type-badge {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  color: #fff;
}
.type-badge.admin {
  background: #1e5a96;
}
.type-badge.dept {
  background: #4a86c5;
}
.type-badge.local {
  background: #39734c;
}

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
.sev-badge.red {
  background: #a44342;
}
.sev-badge.yellow {
  background: #9b621f;
}
.sev-badge.blue {
  background: #2b66a0;
}
.sev-badge.grey {
  background: #64748b;
}

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

/* Shared mobile design tokens and accessible form controls. */
page {
  --color-accent: #235a91;
  --color-text: #23364d;
  --color-muted: #526277;
  --color-surface: #fff;
  --color-border: #dce5ef;
  background: #f2f5f9;
}
.mobile-content {
  max-width: 640px;
  margin: 0 auto;
  box-sizing: border-box;
}
.field-label {
  display: block;
  margin: 24rpx 0 12rpx;
  font-size: 28rpx;
  font-weight: 500;
  color: #23364d;
}
.field-input {
  min-height: 92rpx;
  height: 92rpx;
  padding: 0 24rpx;
  background: #f5f7fa;
  border: 1rpx solid #dce5ef;
  border-radius: 14rpx;
  color: #23364d;
  font-size: 28rpx;
  box-sizing: border-box;
  width: 100%;
}
.field-hint {
  font-size: 26rpx;
  line-height: 1.7;
  margin: 16rpx 0;
  color: #526277;
}
.primary-button {
  min-height: 92rpx;
  line-height: 92rpx;
  font-size: 30rpx;
  font-weight: 600;
  background: #235a91;
  color: #fff;
  border-radius: 14rpx;
  margin: 28rpx 0 0;
}
.primary-button::after,
.text-button::after {
  border: none;
}
.primary-button[disabled] {
  color: #fff;
  background: #6c86a1;
}
.text-button {
  min-height: 84rpx;
  line-height: 84rpx;
  padding: 0;
  margin: 8rpx 0 0;
  background: transparent;
  color: #235a91;
  font-size: 28rpx;
}
.inline-error {
  display: block;
  color: #a44342;
  margin-top: 16rpx;
  font-size: 26rpx;
  line-height: 1.6;
}
.login-note {
  color: #526277;
  margin-top: 24rpx;
  line-height: 1.6;
  font-size: 24rpx;
}
.card {
  border: 1rpx solid #e2e9f1;
  box-shadow: none;
  border-radius: 20rpx;
}
button:focus-visible,
input:focus-visible {
  outline: 3px solid #235a91;
  outline-offset: 2px;
}
@media (min-width: 750px) {
  uni-page-body {
    max-width: 1120px;
    margin: auto;
  }
}

/* H5 uses uni custom elements; inherit the same mobile typeface. */
body,
uni-page,
uni-page-body,
uni-view,
uni-text,
uni-button,
uni-input,
uni-textarea {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC",
    "Microsoft YaHei", sans-serif;
}
/* #ifdef H5 */
@media (min-width: 960px) {
  uni-page-body {
    min-height: calc(100vh - 44px);
    border-left: 1px solid #e3eaf2;
    border-right: 1px solid #e3eaf2;
  }
  .mobile-content {
    max-width: 1120px;
  }
  .page-heading {
    padding: 32px;
  }
  .library-detail .action-bar {
    max-width: 1120px;
    margin: auto;
  }
  uni-tabbar .uni-tabbar {
    max-width: 1120px;
    margin: auto;
    left: 0;
    right: 0;
  }
}
.card-title .app-icon,
.modal-title .app-icon {
  margin-right: 12rpx;
}
/* #endif */
</style>

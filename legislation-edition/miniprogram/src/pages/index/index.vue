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
        <view class="feature-item">法规资料库 · 知识图谱 · 多渠道征集</view>
      </view>

      <view class="version">v0.2 · 演示版</view>
    </view>

    <view class="footer">
      <button class="enter-btn" @click="enterApp">进入工作台</button>
      <view class="login-tip" @click="onLogin">
        {{ loggedIn ? '切换账号' : '管理员登录' }}
      </view>
    </view>

    <!-- 登录弹层 -->
    <view v-if="showLogin" class="modal-mask" @click.self="showLogin = false">
      <view class="modal">
        <view class="modal-title">登录</view>
        <view class="form-row">
          <text class="form-key">用户名</text>
          <input v-model="loginForm.username" class="input" placeholder="admin / leader / drafter / reviewer / evaluator" />
        </view>
        <view class="form-row">
          <text class="form-key">密码</text>
          <input v-model="loginForm.password" password class="input" placeholder="默认 123456" />
        </view>
        <view class="hint">种子账号密码均为 123456</view>
        <view class="modal-actions">
          <view class="modal-btn cancel"  @click="showLogin = false">取消</view>
          <view class="modal-btn confirm" @click="doLogin">登录</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { authApi } from '@/api/index.js'

export default {
  data() {
    return {
      loggedIn: false,
      showLogin: false,
      loginForm: { username: 'admin', password: '123456' }
    }
  },
  onLoad() {
    this.loggedIn = !!uni.getStorageSync('token')
  },
  methods: {
    enterApp() {
      uni.switchTab({ url: '/pages/dashboard/index' })
    },

    onLogin() {
      this.showLogin = true
    },

    async doLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        uni.showToast({ title: '请填写用户名密码', icon: 'none' })
        return
      }
      this.showLogin = false
      uni.showLoading({ title: '登录中…' })
      try {
        const r = await authApi.login(this.loginForm.username, this.loginForm.password)
        const token = r?.token || ''
        const info  = r?.userInfo || {}
        if (token) {
          uni.setStorageSync('token', token)
          uni.setStorageSync('userId', info.id || 1)
          uni.setStorageSync('userName', info.displayName || info.username || this.loginForm.username)
          uni.setStorageSync('userRole', info.role || '系统管理员')
          uni.setStorageSync('userDept', info.department || '')
          this.loggedIn = true
          uni.showToast({ title: '登录成功', icon: 'success' })
        } else {
          // 后端不可达：演示模式直接进
          uni.setStorageSync('userName', this.loginForm.username)
          this.loggedIn = true
          uni.showToast({ title: '已进入演示模式', icon: 'none' })
        }
        setTimeout(() => uni.switchTab({ url: '/pages/dashboard/index' }), 600)
      } catch (e) {
        // 后端不可达 → 演示模式登录
        uni.setStorageSync('userName', this.loginForm.username)
        this.loggedIn = true
        uni.showToast({ title: '已进入演示模式', icon: 'none' })
        setTimeout(() => uni.switchTab({ url: '/pages/dashboard/index' }), 600)
      } finally {
        uni.hideLoading()
      }
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
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
}
.enter-btn {
  background: #fff;
  color: #1e5a96;
  border-radius: 48rpx;
  font-size: 32rpx;
  font-weight: 600;
  height: 96rpx;
  line-height: 96rpx;
  width: 100%;
}
.enter-btn::after { border: none; }
.login-tip {
  font-size: 26rpx;
  opacity: 0.85;
  padding: 8rpx 24rpx;
}

// 登录弹层
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99;
}
.modal {
  width: 84%;
  max-width: 640rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx 32rpx 24rpx;
  color: #1f2937;
}
.modal-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 24rpx;
  text-align: center;
}
.form-row { margin-bottom: 24rpx; }
.form-key {
  font-size: 26rpx;
  color: #1f2937;
  display: block;
  margin-bottom: 8rpx;
}
.input {
  width: 100%;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}
.hint {
  font-size: 22rpx;
  color: #909399;
  text-align: center;
  margin-bottom: 16rpx;
}
.modal-actions { display: flex; gap: 16rpx; }
.modal-btn {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  border-radius: 16rpx;
  font-size: 28rpx;
}
.modal-btn.cancel  { background: #f5f7fa; color: #6b7280; }
.modal-btn.confirm { background: linear-gradient(90deg, #1e5a96, #4a86c5); color: #fff; }
</style>
<template>
  <view class="profile-page">
    <!-- 顶部个人卡片 -->
    <view class="hero">
      <view class="hero-avatar">{{ avatar }}</view>
      <view class="hero-info">
        <view class="hero-name">{{ userName }}</view>
        <view class="hero-role">{{ userRole }}</view>
        <view class="hero-dept" v-if="userDept">🏛 {{ userDept }}</view>
      </view>
      <view class="hero-edit" @click="onEdit">编辑</view>
    </view>

    <!-- 统计数据 -->
    <view class="stat-row">
      <view class="stat-cell">
        <view class="stat-value">{{ stat.draftCount }}</view>
        <view class="stat-label">我起草的草案</view>
      </view>
      <view class="stat-cell">
        <view class="stat-value">{{ stat.opinionCount }}</view>
        <view class="stat-label">提交的意见</view>
      </view>
      <view class="stat-cell">
        <view class="stat-value">{{ stat.favCount }}</view>
        <view class="stat-label">收藏的资料</view>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="card">
      <view class="menu-row" v-for="m in menus" :key="m.label" @click="onTap(m)">
        <view class="menu-icon" :style="{ background: m.color }">{{ m.glyph }}</view>
        <view class="menu-label">{{ m.label }}</view>
        <view v-if="m.tag" class="menu-tag">{{ m.tag }}</view>
        <view class="menu-arrow">›</view>
      </view>
    </view>

    <!-- 关于 -->
    <view class="card">
      <view class="about-row">
        <view class="about-key">当前版本</view>
        <view class="about-val">v0.2 演示版</view>
      </view>
      <view class="about-row">
        <view class="about-key">后端地址</view>
        <view class="about-val">http://localhost:8083</view>
      </view>
      <view class="about-row" @click="onCheckHealth">
        <view class="about-key">服务健康检查</view>
        <view class="about-val">
          <text :class="['health-dot', health ? 'ok' : 'fail']"></text>
          {{ healthText }}
        </view>
      </view>
      <view class="about-row">
        <view class="about-key">API 文档</view>
        <view class="about-val">/api/swagger-ui.html</view>
      </view>
    </view>

    <button class="logout-btn" @click="onLogout">退出登录</button>
  </view>
</template>

<script>
import { authApi } from '@/api/index.js'

export default {
  data() {
    return {
      userName: '',
      userRole: '',
      userDept: '',
      stat: { draftCount: 5, opinionCount: 12, favCount: 18 },
      menus: [
        { label: '我的立法项目',     glyph: '法', color: '#1e5a96', path: '/pages/project/index',    tag: '25' },
        { label: '我的草案',         glyph: '稿', color: '#8e44ad', path: '/pages/draft/index',     tag: '5' },
        { label: '提交的意见记录',   glyph: '议', color: '#17a2b8', path: '/pages/consultation/index', tag: '12' },
        { label: '我的收藏',         glyph: '★', color: '#e6a23c', path: '/pages/library/index' },
        { label: '我的批注',         glyph: '注', color: '#67c23a', path: '' },
        { label: '消息通知',         glyph: '铃', color: '#f56c6c', path: '', tag: '3 条未读' },
        { label: '订阅管理',         glyph: '订', color: '#409eff', path: '/pages/info/index' },
        { label: '系统设置',         glyph: '设', color: '#909399', path: '' },
        { label: '帮助中心',         glyph: '?',  color: '#606266', path: '' },
        { label: '意见反馈',         glyph: '回', color: '#8e44ad', path: '' },
        { label: '关于智立法',       glyph: '关', color: '#1e5a96', path: '' }
      ],
      health: false,
      healthText: '点击检测'
    }
  },
  computed: {
    avatar() { return (this.userName || '管').slice(0, 1) }
  },
  onShow() {
    this.userName = uni.getStorageSync('userName') || '立法管理员'
    this.userRole = uni.getStorageSync('userRole') || '系统管理员'
    this.userDept = uni.getStorageSync('userDept') || ''
  },
  methods: {
    onTap(m) {
      if (m.path) {
        uni.navigateTo({ url: m.path })
      } else {
        uni.showToast({ title: '该功能开发中', icon: 'none' })
      }
    },

    onEdit() {
      uni.showModal({
        title: '编辑个人信息',
        editable: true,
        placeholderText: '请输入新的昵称',
        success: ({ confirm, input: v }) => {
          if (!confirm || !v) return
          uni.setStorageSync('userName', v)
          this.userName = v
          uni.showToast({ title: '已更新', icon: 'success' })
        }
      })
    },

    async onCheckHealth() {
      this.healthText = '检测中…'
      try {
        const r = await authApi.health()
        this.health = r?.status === 'UP'
        this.healthText = this.health ? '✓ 服务正常' : '✗ 服务异常'
      } catch (e) {
        this.health = false
        this.healthText = '✗ 无法连接'
      }
    },

    onLogout() {
      uni.showModal({
        title: '确认退出',
        content: '退出后需要重新登录',
        success: ({ confirm }) => {
          if (confirm) {
            uni.removeStorageSync('token')
            uni.removeStorageSync('userName')
            uni.removeStorageSync('userRole')
            uni.removeStorageSync('userId')
            uni.reLaunch({ url: '/pages/index/index' })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-page { padding: 24rpx 24rpx 240rpx; }

// 顶部
.hero {
  display: flex;
  align-items: center;
  gap: 24rpx;
  background: linear-gradient(120deg, #1e5a96, #4a86c5);
  color: #fff;
  padding: 32rpx;
  border-radius: 24rpx;
  margin-bottom: 24rpx;
}
.hero-avatar {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  font-weight: 700;
  flex-shrink: 0;
}
.hero-info { flex: 1; min-width: 0; }
.hero-name { font-size: 36rpx; font-weight: 700; }
.hero-role { font-size: 24rpx; opacity: 0.85; margin-top: 8rpx; }
.hero-dept { font-size: 22rpx; opacity: 0.85; margin-top: 4rpx; }
.hero-edit {
  background: rgba(255,255,255,0.2);
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  font-size: 24rpx;
}

// 统计
.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx 16rpx;
  margin-bottom: 24rpx;
}
.stat-cell { text-align: center; }
.stat-value { font-size: 40rpx; font-weight: 700; color: #1e5a96; }
.stat-label { font-size: 22rpx; color: #6b7280; margin-top: 4rpx; }

// 卡片
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 16rpx 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(15, 35, 60, 0.04);
}
.card-title {
  font-size: 28rpx;
  font-weight: 600;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
  margin-bottom: 8rpx;
}

// 菜单
.menu-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child { border-bottom: none; }
}
.menu-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
  flex-shrink: 0;
}
.menu-label { flex: 1; font-size: 30rpx; color: #1f2937; }
.menu-tag {
  background: #fef0f0;
  color: #f56c6c;
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
  margin-right: 16rpx;
}
.menu-arrow { font-size: 36rpx; color: #d1d5db; }

// 关于
.about-row {
  display: flex;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
  font-size: 28rpx;
  &:last-child { border-bottom: none; }
}
.about-key { color: #6b7280; }
.about-val { color: #1f2937; }

.health-dot {
  display: inline-block;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 8rpx;
  vertical-align: middle;
}
.health-dot.ok   { background: #67c23a; }
.health-dot.fail { background: #f56c6c; }

// 退出
.logout-btn {
  margin-top: 48rpx;
  background: #fff;
  color: #f56c6c;
  border-radius: 16rpx;
  font-size: 30rpx;
  height: 88rpx;
  line-height: 88rpx;
  border: 1rpx solid #f56c6c;
}
.logout-btn::after { border: none; }
</style>
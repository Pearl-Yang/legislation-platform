<template>
  <view class="dashboard">
    <!-- 顶部欢迎 -->
    <view class="hero">
      <view class="hero-left">
        <view class="hero-date">{{ today }}</view>
        <view class="hero-title">{{ greeting }}, {{ userName }}</view>
        <view class="hero-subtitle">当前进行中 <text class="text-strong">{{ stats.projectActive }}</text> 项立法工作</view>
      </view>
      <view class="hero-avatar">{{ avatar }}</view>
    </view>

    <!-- 4 大数据 -->
    <view class="stat-grid">
      <view class="stat-cell stat-blue">
        <view class="stat-label">进行中项目</view>
        <view class="stat-value">{{ stats.projectActive }}</view>
        <view class="stat-extra">本月新增 +{{ stats.projectNew }}</view>
      </view>
      <view class="stat-cell stat-orange">
        <view class="stat-label">即将到期</view>
        <view class="stat-value">{{ stats.upcoming }}</view>
        <view class="stat-extra text-danger">{{ stats.overdue }} 项已逾期</view>
      </view>
      <view class="stat-cell stat-green">
        <view class="stat-label">待处理意见</view>
        <view class="stat-value">{{ stats.opinionNew }}</view>
        <view class="stat-extra">采纳率 {{ stats.adoptionRate }}%</view>
      </view>
      <view class="stat-cell stat-purple">
        <view class="stat-label">评估中法规</view>
        <view class="stat-value">{{ stats.evalRunning }}</view>
        <view class="stat-extra">已发布 {{ stats.published }}</view>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="card">
      <view class="card-header">
        <view class="card-title">模块快捷入口</view>
        <view class="text-primary fz-12">全部</view>
      </view>
      <view class="quick-grid">
        <view v-for="m in modules" :key="m.path" class="quick-item" @click="goPage(m.path)">
          <view class="qi-icon" :style="{ background: m.color }">
            <text class="qi-icon-text">{{ m.glyph }}</text>
          </view>
          <view class="qi-text">
            <view class="qi-title">{{ m.title }}</view>
            <view class="qi-desc">{{ m.desc }}</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 期限预警 -->
    <view class="card">
      <view class="card-header">
        <view class="card-title">⚠️ 即将到期（30 天内）</view>
        <view class="text-primary fz-12" @click="goPage('/pages/project/index')">查看全部</view>
      </view>
      <view class="deadline-list">
        <view v-for="d in upcomingList" :key="d.id" class="deadline-row" :class="{ overdue: d.daysLeft < 0, soon: d.daysLeft >= 0 && d.daysLeft < 7 }">
          <view class="dr-content">
            <view class="dr-title">{{ d.name }}</view>
            <view class="dr-meta">{{ d.nodeName }} · {{ d.deadlineDate }}</view>
          </view>
          <view v-if="d.daysLeft < 0" class="dl-tag tag-danger">逾期 {{ -d.daysLeft }} 天</view>
          <view v-else-if="d.daysLeft < 7" class="dl-tag tag-warning">{{ d.daysLeft }} 天</view>
          <view v-else class="dl-tag tag-info">{{ d.daysLeft }} 天</view>
        </view>
      </view>
    </view>

    <!-- 我的待办 -->
    <view class="card">
      <view class="card-header">
        <view class="card-title">我的待办</view>
      </view>
      <view class="todo-list">
        <view v-for="t in todos" :key="t.id" class="todo-row">
          <view class="todo-icon" :style="{ background: t.color }">{{ t.glyph }}</view>
          <view class="todo-content">
            <view class="todo-title">{{ t.title }}</view>
            <view class="todo-meta">{{ t.time }} · {{ t.from }}</view>
          </view>
          <view class="text-primary fz-12">去处理</view>
        </view>
      </view>
    </view>

    <!-- 底部 banner -->
    <view class="bottom-banner">
      <text class="bb-icon">🎯</text>
      <view class="bb-text">本期目标：完成 5 项草案审查 · 推进 3 项意见征集 · 完成 2 份评估报告</view>
    </view>
  </view>
</template>

<script>
import { formatDate } from '@/utils/index.js'

export default {
  data() {
    return {
      today: '',
      userName: '',
      stats: {
        projectActive: 12, projectNew: 3,
        upcoming: 8, overdue: 1,
        opinionNew: 47, adoptionRate: 22,
        evalRunning: 4, published: 36
      },
      modules: [
        { path: '/pages/project/index',      title: '立法项目',  desc: '全流程管理', glyph: '法', color: 'linear-gradient(135deg,#4a86c5,#1e5a96)' },
        { path: '/pages/draft/index',       title: '草案生成',  desc: 'AI 起草',    glyph: '稿', color: 'linear-gradient(135deg,#b07cc6,#8e44ad)' },
        { path: '/pages/review/index',      title: '智慧审查',  desc: '红黄蓝灰',   glyph: '审', color: 'linear-gradient(135deg,#f78989,#f56c6c)' },
        { path: '/pages/cleanup/index',     title: '智能清理',  desc: '联动检测',   glyph: '清', color: 'linear-gradient(135deg,#f7b977,#e6a23c)' },
        { path: '/pages/evaluation/index',  title: '实施评估',  desc: '三维评分',   glyph: '评', color: 'linear-gradient(135deg,#95d475,#67c23a)' },
        { path: '/pages/consultation/index', title: '意见征集',  desc: '公众参与',   glyph: '议', color: 'linear-gradient(135deg,#5cdbd3,#17a2b8)' },
        { path: '/pages/library/index',      title: '资料库',    desc: '全文检索',   glyph: '库', color: 'linear-gradient(135deg,#95d4e7,#409eff)' },
        { path: '/pages/info/index',         title: '立法动态',  desc: '资讯聚合',   glyph: '讯', color: 'linear-gradient(135deg,#c0c4cc,#606266)' }
      ],
      upcomingList: [
        { id: 1, name: '网络数据安全管理条例', nodeName: '征求意见', deadlineDate: '2026-10-05', daysLeft: 2 },
        { id: 2, name: '某省医疗保障办法',     nodeName: '法制机构审查', deadlineDate: '2026-10-08', daysLeft: 5 },
        { id: 3, name: '某市人才公寓办法',     nodeName: '部门会签', deadlineDate: '2026-10-12', daysLeft: 9 },
        { id: 4, name: '养老服务促进条例',     nodeName: '公布', deadlineDate: '2026-09-30', daysLeft: -3 },
        { id: 5, name: '烟花安全管理规定',     nodeName: '立项审查', deadlineDate: '2026-10-25', daysLeft: 22 }
      ],
      todos: [
        { id: 1, glyph: '审', color: '#f56c6c', title: '《网络数据安全管理条例》草案审查结果待复核', time: '10 分钟前', from: '智慧审查' },
        { id: 2, glyph: '议', color: '#17a2b8', title: '12 条新意见待分类归并', time: '35 分钟前', from: '意见征集' },
        { id: 3, glyph: '清', color: '#e6a23c', title: '2026Q4 规章定期清理任务待发布', time: '1 小时前', from: '智能清理' },
        { id: 4, glyph: '评', color: '#67c23a', title: '《养老服务促进条例》年度评估报告待签发', time: '今天 09:30', from: '实施评估' }
      ]
    }
  },
  computed: {
    avatar() {
      return (this.userName || '管').slice(0, 1)
    },
    greeting() {
      const h = new Date().getHours()
      if (h < 6)  return '夜深了'
      if (h < 11) return '早上好'
      if (h < 14) return '中午好'
      if (h < 18) return '下午好'
      return '晚上好'
    }
  },
  onLoad() {
    this.today = formatDate(new Date(), 'YYYY年M月D日')
    this.userName = uni.getStorageSync('userName') || '立法管理员'
  },
  onShow() {
    this.userName = uni.getStorageSync('userName') || '立法管理员'
  },
  methods: {
    goPage(url) {
      // tabBar 页
      const tabPages = ['/pages/dashboard/index', '/pages/project/index', '/pages/consultation/index', '/pages/profile/index']
      if (tabPages.includes(url)) {
        uni.switchTab({ url })
      } else {
        uni.navigateTo({ url })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard { padding: 24rpx; padding-top: 24rpx; padding-bottom: 240rpx; }

// 欢迎
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(120deg, #1e5a96, #4a86c5);
  color: #fff;
  padding: 32rpx;
  border-radius: 24rpx;
  margin-bottom: 24rpx;
}
.hero-date   { font-size: 24rpx; opacity: 0.85; }
.hero-title { font-size: 36rpx; font-weight: 700; margin-top: 8rpx; }
.hero-subtitle { font-size: 24rpx; opacity: 0.9; margin-top: 8rpx; }
.text-strong { font-weight: 700; }
.hero-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: 700;
}

// 4 大数据
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.stat-cell {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  border-left: 8rpx solid #1e5a96;
  &.stat-blue   { border-left-color: #1e5a96; }
  &.stat-orange { border-left-color: #e6a23c; }
  &.stat-green  { border-left-color: #67c23a; }
  &.stat-purple { border-left-color: #8e44ad; }
  .stat-label { font-size: 24rpx; color: #6b7280; }
  .stat-value { font-size: 48rpx; font-weight: 700; color: #1f2937; margin: 8rpx 0; }
  .stat-extra { font-size: 22rpx; color: #6b7280; }
  .text-danger { color: #f56c6c; }
}

// 快捷入口
.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}
.quick-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx;
  border-radius: 12rpx;
  background: #f5f7fa;
}
.qi-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
  flex-shrink: 0;
}
.qi-title { font-size: 28rpx; font-weight: 500; color: #1f2937; }
.qi-desc  { font-size: 22rpx; color: #6b7280; margin-top: 4rpx; }

// 期限预警
.deadline-list { display: flex; flex-direction: column; gap: 12rpx; }
.deadline-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx;
  border-radius: 12rpx;
  background: #f5f7fa;
  &.overdue { background: #fef0f0; }
  &.soon    { background: #fdf6ec; }
}
.dr-content { flex: 1; min-width: 0; }
.dr-title { font-size: 28rpx; font-weight: 500; color: #1f2937; }
.dr-meta  { font-size: 22rpx; color: #6b7280; margin-top: 4rpx; }
.dl-tag { font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 6rpx; }
.tag-danger  { background: #f56c6c; color: #fff; }
.tag-warning { background: #e6a23c; color: #fff; }
.tag-info    { background: #909399; color: #fff; }

// 待办
.todo-list { display: flex; flex-direction: column; gap: 12rpx; }
.todo-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx;
  border-radius: 12rpx;
  background: #f5f7fa;
}
.todo-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
}
.todo-content { flex: 1; min-width: 0; }
.todo-title { font-size: 26rpx; color: #1f2937; line-height: 1.5; }
.todo-meta  { font-size: 22rpx; color: #6b7280; margin-top: 4rpx; }

// banner
.bottom-banner {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: linear-gradient(90deg, #fff3cd, #fff8e1);
  border: 1rpx solid #ffeaa7;
  font-size: 24rpx;
  color: #856404;
  line-height: 1.6;
}
.bb-icon { font-size: 36rpx; }
.bb-text { flex: 1; }
</style>
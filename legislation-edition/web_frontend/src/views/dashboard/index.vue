<template>
  <div class="dashboard">
    <!-- ========== 欢迎区 ========== -->
    <div class="hero">
      <div class="hero-left">
        <div class="hero-date">{{ today }}</div>
        <h1 class="hero-title">{{ greeting }}，{{ userName }}</h1>
        <p class="hero-subtitle">今天是 {{ dateDesc }}，让我们继续推进 <strong>{{ projectCount }}</strong> 项进行中的立法工作</p>
      </div>
      <div class="hero-right">
        <el-button type="primary" :icon="Plus" size="large" @click="$router.push('/app/project')">新建立法项目</el-button>
        <el-button :icon="Promotion" size="large" plain @click="$router.push('/app/draft/generate')">AI 生成草案</el-button>
      </div>
    </div>

    <!-- ========== 4 大统计卡 ========== -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card stat-blue">
          <div class="icon-wrap"><el-icon><Files /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">进行中项目</div>
            <div class="stat-value">{{ stats.projectActive }}</div>
            <div class="stat-extra">本月新增 <span class="text-primary-color">+{{ stats.projectNew }}</span></div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card stat-orange">
          <div class="icon-wrap"><el-icon><BellFilled /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">即将到期</div>
            <div class="stat-value">{{ stats.upcoming }}</div>
            <div class="stat-extra">含 <span class="text-danger">{{ stats.overdue }}</span> 项已逾期</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card stat-green">
          <div class="icon-wrap"><el-icon><ChatDotRound /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">待处理意见</div>
            <div class="stat-value">{{ stats.opinionNew }}</div>
            <div class="stat-extra">采纳率 {{ stats.adoptionRate }}%</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card stat-purple">
          <div class="icon-wrap"><el-icon><DataAnalysis /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">评估中法规</div>
            <div class="stat-value">{{ stats.evalRunning }}</div>
            <div class="stat-extra">已发布 {{ stats.published }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ========== 中部三栏 ========== -->
    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :md="16">
        <!-- 流程进度分布 -->
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="title">项目状态分布</span>
              <el-radio-group v-model="chartType" size="small">
                <el-radio-button value="status">按状态</el-radio-button>
                <el-radio-button value="type">按类型</el-radio-button>
                <el-radio-button value="month">月度趋势</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <v-chart :option="chartOption" autoresize style="height: 320px" />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card class="quick-actions">
          <template #header>
            <span class="title">模块快捷入口</span>
          </template>
          <div class="quick-grid">
            <div v-for="m in modules" :key="m.path" class="quick-item" @click="$router.push(m.path)">
              <div class="qi-icon" :style="{ background: m.color }">
                <el-icon><component :is="m.icon" /></el-icon>
              </div>
              <div class="qi-text">
                <div class="qi-title">{{ m.title }}</div>
                <div class="qi-desc">{{ m.desc }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 底部：期限预警 + 待办 ========== -->
    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :md="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="title">
                <el-icon class="warn-icon"><BellFilled /></el-icon>
                即将到期（30 天内）
              </span>
              <el-link type="primary" :underline="false" @click="$router.push('/app/project')">查看全部 →</el-link>
            </div>
          </template>
          <el-table :data="upcomingList" stripe size="default">
            <el-table-column label="项目名称" min-width="220">
              <template #default="{ row }">
                <div class="project-cell">
                  <span class="stage-dot" :class="row.stageCls"></span>
                  <a @click="$router.push(`/app/project/detail/${row.id}`)">{{ row.name }}</a>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="当前节点" width="130" prop="nodeName" />
            <el-table-column label="截止日期" width="120">
              <template #default="{ row }">
                <span :class="row.daysLeft < 0 ? 'text-danger' : row.daysLeft < 7 ? 'text-warning' : 'text-secondary'">
                  {{ row.deadlineDate }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="剩余天数" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.daysLeft < 0"  type="danger"  size="small">逾期 {{ -row.daysLeft }} 天</el-tag>
                <el-tag v-else-if="row.daysLeft < 7" type="warning" size="small">{{ row.daysLeft }} 天</el-tag>
                <el-tag v-else type="info" size="small">{{ row.daysLeft }} 天</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default>
                <el-button type="primary" link size="small">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card>
          <template #header>
            <span class="title">我的待办</span>
          </template>
          <div class="todo-list">
            <div v-for="t in todos" :key="t.id" class="todo-row">
              <div class="todo-icon" :class="`todo-${t.type}`">
                <el-icon><component :is="t.icon" /></el-icon>
              </div>
              <div class="todo-content">
                <div class="todo-title">{{ t.title }}</div>
                <div class="todo-meta">{{ t.time }} · {{ t.from }}</div>
              </div>
              <el-button v-if="t.link" link type="primary" size="small" @click="$router.push(t.link)">去处理</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 底部横幅 ========== -->
    <div class="banner mt-16">
      <el-icon class="banner-icon"><Aim /></el-icon>
      <div class="banner-text">
        <strong>本期目标</strong>：完成 5 项立法草案智慧审查 · 推进 3 项意见征集进入分类阶段 · 完成 2 份年度立法后评估报告
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import { Plus, Promotion, Files, BellFilled, ChatDotRound, DataAnalysis, Aim, Document, EditPen, View, Brush, List, DataBoard, Notification } from '@element-plus/icons-vue'
import request from '@/utils/request'

use([CanvasRenderer, PieChart, BarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const today = dayjs().format('YYYY年M月D日 dddd HH:mm')
const dateDesc = dayjs().format('M月D日')
const userName = computed(() => localStorage.getItem('userName') || '立法管理员')

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6)  return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// 真实数据(默认填一组保底值,后端返回时覆盖)
const stats = ref({
  projectActive: 0, projectNew: 0,
  upcoming: 0,     overdue: 0,
  opinionNew: 0,   adoptionRate: 0,
  evalRunning: 0,  published: 0
})
const projectCount = computed(() => stats.value.projectActive)

const typeDistribution = ref({ ADMIN_REGULATION: 0, DEPT_RULE: 0, LOCAL_RULE: 0 })
const recentProjects   = ref([])

async function loadDashboard() {
  try {
    const res = await request.get('/legislative-project/dashboard')
    const data = res.data || {}
    stats.value = {
      projectActive: data.active        || 0,
      projectNew:    data.recentProjects ? data.recentProjects.length : 0,
      upcoming:      data.dueSoonProjects || 0,
      overdue:       data.overdueProjects || 0,
      opinionNew:    0,
      adoptionRate:  0,
      evalRunning:   0,
      published:     data.published     || 0
    }
    typeDistribution.value = data.typeDistribution || typeDistribution.value
    recentProjects.value   = data.recentProjects   || []
  } catch (e) {
    console.warn('dashboard 数据加载失败,使用占位值', e)
  }
}

const chartType = ref('status')
const chartOption = computed(() => {
  if (chartType.value === 'status') {
    return {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, icon: 'circle' },
      color: ['#4f46e5', '#6366f1', '#10b981', '#f43f5e'],
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%' },
        data: [
          { value: 12, name: '进行中' },
          { value: 36, name: '已发布' },
          { value: 5,  name: '草稿' },
          { value: 4,  name: '已废止' }
        ]
      }]
    }
  }
  if (chartType.value === 'type') {
    return {
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0 },
      grid: { top: 20, left: 40, right: 20, bottom: 40 },
      color: ['#4f46e5', '#6366f1', '#10b981'],
      xAxis: { type: 'category', data: ['进行中', '草稿', '已发布', '已废止'] },
      yAxis: { type: 'value' },
      series: [
        { name: '行政法规',   type: 'bar', stack: 't', data: [4, 2, 12, 2] },
        { name: '部门规章',   type: 'bar', stack: 't', data: [5, 2, 14, 1] },
        { name: '地方政府规章', type: 'bar', stack: 't', data: [3, 1, 10, 1] }
      ]
    }
  }
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 20, left: 40, right: 20, bottom: 40 },
    color: ['#4f46e5'],
    xAxis: { type: 'category', data: ['1月','2月','3月','4月','5月','6月','7月','8月','9月'] },
    yAxis: { type: 'value', name: '项目数' },
    series: [{ name: '新增立法项目', type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: [5, 7, 6, 9, 8, 10, 11, 9, 12] }]
  }
})

const modules = [
  { path: '/app/project',           title: '立法项目',   desc: '全流程管理',  icon: 'Files',    color: 'linear-gradient(135deg,#6366f1,#4f46e5)' },
  { path: '/app/draft/generate',    title: '草案生成',   desc: 'AI 辅助起草', icon: 'EditPen',  color: 'linear-gradient(135deg,#a78bfa,#8b5cf6)' },
  { path: '/app/review/records',    title: '智慧审查',   desc: '红黄蓝灰四级', icon: 'View',   color: 'linear-gradient(135deg,#fb7185,#f43f5e)' },
  { path: '/app/cleanup/tasks',     title: '智能清理',   desc: '联动检测',    icon: 'Brush',    color: 'linear-gradient(135deg,#fbbf24,#f59e0b)' },
  { path: '/app/evaluation/tasks',  title: '实施评估',   desc: '三维评分',    icon: 'DataAnalysis', color: 'linear-gradient(135deg,#34d399,#10b981)' },
  { path: '/app/consultation/list', title: '意见征集',   desc: '公众参与',    icon: 'ChatDotRound', color: 'linear-gradient(135deg,#22d3ee,#06b6d4)' },
  { path: '/app/library/list',      title: '资料库',     desc: '全文检索',    icon: 'Document', color: 'linear-gradient(135deg,#60a5fa,#3b82f6)' },
  { path: '/app/info/dashboard',    title: '数据大屏',   desc: '可视化',      icon: 'DataBoard', color: 'linear-gradient(135deg,#94a3b8,#475569)' }
]

const upcomingList = ref([
  { id: 1, name: '网络数据安全管理条例', nodeName: '征求意见', deadlineDate: '2026-10-05', daysLeft: 2, stageCls: 'in-progress' },
  { id: 2, name: '某省医疗保障办法',     nodeName: '法制机构审查', deadlineDate: '2026-10-08', daysLeft: 5, stageCls: 'in-progress' },
  { id: 3, name: '某市人才公寓管理办法', nodeName: '部门会签', deadlineDate: '2026-10-12', daysLeft: 9, stageCls: 'pending' },
  { id: 4, name: '养老服务促进条例',     nodeName: '公布', deadlineDate: '2026-09-30', daysLeft: -3, stageCls: 'returned' },
  { id: 5, name: '烟花爆竹安全管理规定', nodeName: '立项审查', deadlineDate: '2026-10-25', daysLeft: 22, stageCls: 'pending' }
])

const todos = ref([
  { id: 1, type: 'review',   icon: 'View',  title: '《网络数据安全管理条例》草案审查结果待复核', time: '10 分钟前', from: '智慧审查', link: '/app/review/records' },
  { id: 2, type: 'opinion',  icon: 'ChatDotRound', title: '12 条新意见待分类归并', time: '35 分钟前', from: '意见征集', link: '/app/consultation/list' },
  { id: 3, type: 'clean',    icon: 'Brush', title: '2026Q4 规章定期清理任务待发布', time: '1 小时前', from: '智能清理', link: '/app/cleanup/tasks' },
  { id: 4, type: 'evaluate', icon: 'DataAnalysis', title: '《养老服务促进条例》年度评估报告待签发', time: '今天 09:30', from: '实施评估', link: '/app/evaluation/reports' },
  { id: 5, type: 'notify',   icon: 'Notification', title: '上位法《数据安全法》近期修订，请关注下位法', time: '昨天 16:12', from: '系统通知', link: '/app/info/news' }
])

onMounted(() => {
  loadDashboard()
})
</script>

<style lang="scss" scoped>
.dashboard { padding: 0; }

// 欢迎区
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(120deg, #4f46e5 0%, #6366f1 100%);
  color: #fff;
  padding: 22px 28px;
  border-radius: 12px;
  margin-bottom: 16px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -40px; right: -40px;
    width: 200px; height: 200px;
    background: rgba(255,255,255,0.08);
    border-radius: 50%;
  }
  &::after {
    content: '';
    position: absolute;
    bottom: -60px; right: 100px;
    width: 160px; height: 160px;
    background: rgba(255,255,255,0.06);
    border-radius: 50%;
  }

  .hero-date { font-size: 13px; opacity: 0.8; }
  .hero-title { font-size: 24px; margin: 6px 0; font-weight: 600; }
  .hero-subtitle { font-size: 13px; opacity: 0.9; }
  .hero-right :deep(.el-button) { margin-left: 8px; }
  .hero-right :deep(.el-button--primary) {
    background: #fff; color: $primary-color; border-color: #fff;
    &:hover { background: #f5f7fa; border-color: #f5f7fa; }
  }
}

// 图表卡
.chart-card .card-header { display: flex; align-items: center; justify-content: space-between; }
.chart-card .title { font-weight: 600; }

// 快捷入口
.quick-actions .title { font-weight: 600; }
.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.quick-item {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.18s;
  border: 1px solid transparent;

  &:hover {
    background: $bg-page;
    border-color: $primary-lighter;
    transform: translateY(-1px);
  }
  .qi-icon {
    width: 38px; height: 38px;
    border-radius: 8px;
    color: #fff;
    display: flex; align-items: center; justify-content: center;
    font-size: 18px;
    flex-shrink: 0;
  }
  .qi-title { font-size: 14px; font-weight: 500; color: $text-primary; }
  .qi-desc  { font-size: 12px; color: $text-secondary; margin-top: 2px; }
}

.project-cell a { color: $primary-color; &:hover { text-decoration: underline; } }
.warn-icon { color: $accent-amber; margin-right: 4px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-weight: 600; }

// 待办
.todo-list { display: flex; flex-direction: column; gap: 8px; }
.todo-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: $bg-page;
  &:hover { background: $primary-lighter; }
}
.todo-icon {
  width: 32px; height: 32px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  font-size: 16px;
  &.todo-review   { background: $sev-red; }
  &.todo-opinion  { background: $accent-cyan; }
  &.todo-clean    { background: $accent-amber; }
  &.todo-evaluate { background: $status-success; }
  &.todo-notify   { background: $sev-grey; }
}
.todo-content { flex: 1; min-width: 0; }
.todo-title   { font-size: 13px; color: $text-primary; line-height: 1.4; }
.todo-meta    { font-size: 12px; color: $text-secondary; margin-top: 2px; }

// 横幅
.banner {
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(90deg, #fff3cd 0%, #fff8e1 100%);
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 14px 20px;
  color: #856404;
  font-size: 13px;
  line-height: 1.6;
  .banner-icon { font-size: 22px; color: $accent-amber; }
  strong { color: #6c5304; }
}
</style>
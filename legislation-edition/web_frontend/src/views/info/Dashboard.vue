<template>
  <div class="bigscreen">
    <!-- 顶部 -->
    <header class="bs-header">
      <div class="bs-title">
        <span class="bs-line"></span>
        <span>智立法 · 行政立法智能辅助平台 · 数据大屏</span>
        <span class="bs-line"></span>
      </div>
      <div class="bs-clock">{{ now }}</div>
      <!-- Day4 新增:视图切换标签 -->
      <div class="bs-tabs">
        <span
          v-for="(t, i) in views"
          :key="t.key"
          :class="{ active: viewIndex === i }"
          @click="viewIndex = i"
        >
          <i :style="{ background: t.color }"></i>{{ t.label }}
        </span>
      </div>
    </header>

    <!-- 加载 / 错误 全屏提示 -->
    <div v-if="loading" class="bs-state bs-state-loading">
      <span class="el-icon-loading"></span>
      <span>正在加载大屏数据…</span>
    </div>
    <div v-else-if="error" class="bs-state bs-state-error">
      <span class="el-icon-warning"></span>
      <div>
        <div>大屏数据加载失败:{{ errorMsg }}</div>
        <el-button size="small" type="primary" @click="loadAll">点击重试</el-button>
      </div>
    </div>

    <!-- 第一行：KPI 4 大卡 -->
    <section class="bs-kpi-row">
      <div class="bs-kpi bs-card-blue">
        <div class="bs-kpi-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 12h8v2H8v-2zm0 4h8v2H8v-2z"/></svg>
        </div>
        <div class="bs-kpi-info">
          <div class="bs-kpi-label">法规存量</div>
          <div class="bs-kpi-value">{{ fmt(kpi.regulationCount) }}<span class="unit">部</span></div>
          <div class="bs-kpi-trend up">↑ 近 30 天 +{{ kpi.recentRegulationCount }}</div>
        </div>
      </div>
      <div class="bs-kpi bs-card-green">
        <div class="bs-kpi-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M12 2a10 10 0 1 0 10 10A10 0 0 0 12 2zm-1 14.59L6.41 12 7.83 10.59 11 13.76l5.17-5.17L17.59 10z"/></svg>
        </div>
        <div class="bs-kpi-info">
          <div class="bs-kpi-label">本年已发布</div>
          <div class="bs-kpi-value">{{ fmt(kpi.publishedProjects) }}<span class="unit">项</span></div>
          <div class="bs-kpi-trend">在研 {{ fmt(kpi.activeProjects) }} 项</div>
        </div>
      </div>
      <div class="bs-kpi bs-card-orange">
        <div class="bs-kpi-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
        </div>
        <div class="bs-kpi-info">
          <div class="bs-kpi-label">立法项目总数</div>
          <div class="bs-kpi-value">{{ fmt(kpi.totalProjects) }}<span class="unit">项</span></div>
          <div class="bs-kpi-trend">草案 {{ fmt(kpi.draftStats?.total || 0) }} 份</div>
        </div>
      </div>
      <div class="bs-kpi bs-card-violet">
        <div class="bs-kpi-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"/></svg>
        </div>
        <div class="bs-kpi-info">
          <div class="bs-kpi-label">资料库总数</div>
          <div class="bs-kpi-value">{{ fmt(kpi.materialCount) }}<span class="unit">条</span></div>
          <div class="bs-kpi-trend up">{{ fmt(kpi.opinionStats?.total || 0) }} 条意见</div>
        </div>
      </div>
    </section>

    <!-- 第二行：趋势 + 类型 + AI 任务 + 意见征集 -->
    <section class="bs-row bs-row-2">
      <div class="bs-card bs-card-trend">
        <div class="bs-card-head">
          <span class="bs-card-icon blue">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z"/></svg>
          </span>
          <div>
            <div class="bs-card-title">月度立法趋势</div>
            <div class="bs-card-subtitle">近 6 个月法规发布 / 资料新增</div>
          </div>
          <div class="bs-card-legend">
            <span><i style="background:#4f46e5"></i>发布法规</span>
            <span><i style="background:#10b981"></i>新增资料</span>
          </div>
        </div>
        <TrendChart :data="trendData" />
      </div>

      <div class="bs-card bs-card-type">
        <div class="bs-card-head">
          <span class="bs-card-icon green">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M11 2v20c-5.07-.5-9-4.79-9-10s3.93-9.5 9-10zm2.03 0v8.99H22c-.47-4.74-4.24-8.52-8.97-8.99zm0 11.01V22c4.74-.47 8.5-4.25 8.97-8.99h-8.97z"/></svg>
          </span>
          <div>
            <div class="bs-card-title">法规类型分布</div>
            <div class="bs-card-subtitle">现行有效 {{ fmt(kpi.regulationCount) }} 部</div>
          </div>
        </div>
        <PieDonut :data="pieData" />
        <ul class="type-legend">
          <li v-for="(t, i) in typeBreakdown" :key="t.name">
            <i :style="{ background: pieColors[i % pieColors.length] }"></i>
            <span class="t-name">{{ t.name }}</span>
            <span class="t-pct">{{ t.pct }}%</span>
          </li>
        </ul>
      </div>

      <div class="bs-card bs-card-ai">
        <div class="bs-card-head">
          <span class="bs-card-icon violet">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M7.5 5.6L10 7 8.6 4.5 10 2 7.5 3.4 5 2l1.4 2.5L5 7zm12 9.8L17 14l1.4 2.5L17 19l2.5-1.4L22 19l-1.4-2.5L22 14zM22 2l-1.4 2.5L22 7l-2.5-1.4L17 7l1.4-2.5L17 2l2.5 1.4zm-8.66 10.34l-1.41-1.41 4.95-4.95 1.41 1.41-4.95 4.95zm-3.42 2.02l-1.41-1.41 4.95-4.95L14.88 9.41l-4.96 4.95zM10.71 18.29l1.41 1.41-2.12 2.12-1.41-1.41 2.12-2.12zM4 2v20h2V2H4z"/></svg>
          </span>
          <div>
            <div class="bs-card-title">本周 AI 任务</div>
            <div class="bs-card-subtitle">智能化平台运行概览</div>
          </div>
        </div>
        <ul class="ai-tiles">
          <li class="ai-tile t-violet">
            <div class="ai-tile-icon">✎</div>
            <div class="ai-tile-label">AI 起草</div>
            <div class="ai-tile-value">{{ fmt(aiStats.draft) }}</div>
            <div class="ai-tile-meta">篇草案</div>
          </li>
          <li class="ai-tile t-rose">
            <div class="ai-tile-icon">⚖</div>
            <div class="ai-tile-label">智慧审查</div>
            <div class="ai-tile-value">{{ fmt(aiStats.review) }}</div>
            <div class="ai-tile-meta">次审查</div>
          </li>
          <li class="ai-tile t-amber">
            <div class="ai-tile-icon">⌫</div>
            <div class="ai-tile-label">清理建议</div>
            <div class="ai-tile-value">{{ fmt(aiStats.cleanup) }}</div>
            <div class="ai-tile-meta">条建议</div>
          </li>
          <li class="ai-tile t-cyan">
            <div class="ai-tile-icon">✉</div>
            <div class="ai-tile-label">意见归类</div>
            <div class="ai-tile-value">{{ fmt(aiStats.opinion) }}</div>
            <div class="ai-tile-meta">条意见</div>
          </li>
        </ul>
      </div>

      <div class="bs-card bs-card-opinion">
        <div class="bs-card-head">
          <span class="bs-card-icon rose">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/></svg>
          </span>
          <div>
            <div class="bs-card-title">意见征集热度</div>
            <div class="bs-card-subtitle">意见数 TOP {{ topConsultations.length }}</div>
          </div>
        </div>
        <ul class="opinion-list">
          <li v-for="(c, i) in topConsultations" :key="c.id">
            <span class="op-rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
            <span class="op-title" :title="c.title">{{ c.title }}</span>
            <span class="op-num">{{ fmt(c.totalOpinions) }}</span>
          </li>
          <li v-if="topConsultations.length === 0" class="op-empty">暂无意见征集</li>
        </ul>
      </div>
    </section>

    <!-- 第三行：地区分布 + 即将到期 + 状态 -->
    <section class="bs-row bs-row-3">
      <div class="bs-card bs-card-region">
        <div class="bs-card-head">
          <span class="bs-card-icon amber">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 0 1 0-5 2.5 2.5 0 0 1 0 5z"/></svg>
          </span>
          <div>
            <div class="bs-card-title">地区立法分布 Top 10</div>
            <div class="bs-card-subtitle">各省 / 直辖市现行法规数</div>
          </div>
        </div>
        <ul class="region-grid">
          <li v-for="(r, idx) in regionBars" :key="r.name">
            <span class="rk" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
            <span class="rn">{{ r.name }}</span>
            <div class="rb"><div :style="{ width: r.percent + '%' }"></div></div>
            <span class="rv">{{ fmt(r.value) }}</span>
          </li>
          <li v-if="regionBars.length === 0" class="op-empty">暂无地区数据</li>
        </ul>
      </div>

      <div class="bs-card bs-card-stat">
        <div class="bs-card-head">
          <span class="bs-card-icon violet">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"/></svg>
          </span>
          <div>
            <div class="bs-card-title">法规状态分布</div>
            <div class="bs-card-subtitle">现行 / 修订中 / 已废止</div>
          </div>
        </div>
        <div class="status-grid">
          <div class="st-cell blue">
            <div class="st-label">现行有效</div>
            <div class="st-value">{{ fmt(statusDist.EFFECTIVE) }}</div>
            <div class="st-trend up">↑ {{ fmt(kpi.recentRegulationCount) }} 本月</div>
          </div>
          <div class="st-cell amber">
            <div class="st-label">修订中</div>
            <div class="st-value">{{ fmt(statusDist.REVISING) }}</div>
            <div class="st-trend">— 持平</div>
          </div>
          <div class="st-cell green">
            <div class="st-label">已废止</div>
            <div class="st-value">{{ fmt(statusDist.OBSOLETE) }}</div>
            <div class="st-trend">历史文本保留</div>
          </div>
          <div class="st-cell rose">
            <div class="st-label">到期预警</div>
            <div class="st-value">{{ fmt(kpi.upcomingDeadlines) }}</div>
            <div class="st-trend">— 30 天内</div>
          </div>
        </div>
      </div>

      <div class="bs-card bs-card-deadline">
        <div class="bs-card-head">
          <span class="bs-card-icon rose">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 22a10 10 0 1 1 10-10 10 10 0 0 1-10 10zm0-18a8 8 0 1 0 8 8 8 8 0 0 0-8-8zm.5 9H7v2h6v-7h-2v5z"/></svg>
          </span>
          <div>
            <div class="bs-card-title">即将到期项目</div>
            <div class="bs-card-subtitle">跨项目期限预警 Top 5</div>
          </div>
        </div>
        <ul class="deadline-list">
          <li v-for="(d, i) in upcomingDeadlines" :key="i">
            <div class="d-meta">
              <div class="d-title">{{ d.stageName || d.nodeName || `节点 ${i + 1}` }}</div>
              <div class="d-project">{{ d.projectName || '—' }}</div>
            </div>
            <el-tag v-if="d.daysLeft < 0" type="danger" size="small">逾期 {{ -d.daysLeft }} 天</el-tag>
            <el-tag v-else-if="d.daysLeft <= 7" type="warning" size="small">{{ d.daysLeft }} 天</el-tag>
            <el-tag v-else type="info" size="small">{{ d.daysLeft }} 天</el-tag>
          </li>
          <li v-if="upcomingDeadlines.length === 0" class="op-empty">无即将到期的项目</li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import dayjs from 'dayjs'
import {
  infoDashboard, infoDashboardChart, regulationMap,
  projectDashboard, upcomingProjects, listConsultations
} from '@/api/legislation'
import { listCleanupTasks, listEvaluations } from '@/api/legislation'
import TrendChart from './dashboard-charts/TrendChart.vue'
import PieDonut  from './dashboard-charts/PieDonut.vue'

const now = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))
let timer, viewTimer

// ============ Day4 新增:30 秒轮播 ============
const views = [
  { key: 'all',      label: '总览', color: '#4f46e5' },
  { key: 'trend',    label: '趋势', color: '#10b981' },
  { key: 'region',   label: '地区', color: '#f59e0b' },
  { key: 'status',   label: '状态', color: '#f43f5e' }
]
const viewIndex = ref(0)
function startViewCycle () {
  stopViewCycle()
  viewTimer = setInterval(() => {
    viewIndex.value = (viewIndex.value + 1) % views.length
  }, 30000)
}
function stopViewCycle () {
  if (viewTimer) { clearInterval(viewTimer); viewTimer = null }
}

// ============ 加载 / 错误状态 ============
const loading = ref(true)
const error   = ref(false)
const errorMsg = ref('')

// ============ KPI 数据 ============
const kpi = ref({
  regulationCount: 0,
  recentRegulationCount: 0,
  totalProjects: 0,
  activeProjects: 0,
  publishedProjects: 0,
  materialCount: 0,
  upcomingDeadlines: 0,
  draftStats: null,
  opinionStats: null
})

// AI 任务统计(由 /legislative-project/dashboard 推断 + 各模块聚合)
const aiStats = ref({ draft: 0, review: 0, cleanup: 0, opinion: 0 })

// ============ 法规类型分布 ============
const pieColors = ['#4f46e5', '#10b981', '#f59e0b', '#f43f5e']
const TYPE_LABEL = {
  ADMIN_REGULATION: '行政法规',
  DEPT_RULE:        '部门规章',
  LOCAL_RULE:       '地方政府规章'
}
const typeBreakdown = computed(() => {
  const dist = kpi.value.regulationTypeDistribution || {}
  const total = (dist.ADMIN_REGULATION || 0) + (dist.DEPT_RULE || 0) + (dist.LOCAL_RULE || 0) || 1
  return ['ADMIN_REGULATION', 'DEPT_RULE', 'LOCAL_RULE'].map(k => ({
    name: TYPE_LABEL[k],
    pct:  Math.round(((dist[k] || 0) / total) * 100)
  }))
})
const pieData = computed(() => {
  const dist = kpi.value.regulationTypeDistribution || {}
  return ['ADMIN_REGULATION', 'DEPT_RULE', 'LOCAL_RULE'].map((k, i) => ({
    name:  TYPE_LABEL[k],
    value: dist[k] || 0,
    color: pieColors[i % pieColors.length]
  }))
})

const statusDist = computed(() => kpi.value.regulationStatusDistribution || {})

const trendData = ref({
  labels: [],
  series: [
    { name: '发布法规', color: '#4f46e5', data: [] },
    { name: '新增资料', color: '#10b981', data: [] }
  ]
})

const regionBars = ref([])
const REGION_NAME = {
  '110000':'北京','120000':'天津','130000':'河北','140000':'山西','150000':'内蒙古',
  '210000':'辽宁','220000':'吉林','230000':'黑龙江',
  '310000':'上海','320000':'江苏','330000':'浙江','340000':'安徽','350000':'福建',
  '360000':'江西','370000':'山东',
  '410000':'河南','420000':'湖北','430000':'湖南','440000':'广东','450000':'广西','460000':'海南',
  '500000':'重庆','510000':'四川','520000':'贵州','530000':'云南','540000':'西藏',
  '610000':'陕西','620000':'甘肃','630000':'青海','640000':'宁夏','650000':'新疆'
}

// 意见征集热度 Top5
const topConsultations = ref([])
// 即将到期项目 Top5
const upcomingDeadlines = ref([])

function fmt (n) {
  if (n === null || n === undefined || isNaN(n)) return 0
  return Number(n).toLocaleString('zh-CN')
}

async function safeCall (name, fn, fallback) {
  try {
    const r = await fn()
    return r
  } catch (e) {
    console.warn(`[Dashboard] ${name} 失败:`, e?.message)
    return fallback
  }
}

async function loadAll () {
  loading.value = true
  error.value   = false
  errorMsg.value = ''
  try {
    // ============ 1. 主 KPI ============
    const dash = await safeCall('infoDashboard', () => infoDashboard(), { data: {} })
    const d = dash.data || {}
    kpi.value = {
      regulationCount:            d.regulationCount ?? 0,
      recentRegulationCount:      d.recentRegulationCount ?? 0,
      totalProjects:              d.project?.total ?? 0,
      activeProjects:             d.project?.active ?? 0,
      publishedProjects:          d.project?.published ?? 0,
      materialCount:              d.materialCount ?? 0,
      upcomingDeadlines:          d.upcomingDeadlines ?? 0,
      regulationTypeDistribution:  d.regulationTypeDistribution || {},
      regulationStatusDistribution: d.regulationStatusDistribution || {},
      draftStats:                 d.draftStats || null,
      opinionStats:               d.opinionStats || null
    }

    // ============ 2. 趋势图 ============
    const chart = await safeCall('infoDashboardChart', () => infoDashboardChart(), { data: {} })
    const c = chart.data || {}
    const months = c.months || []
    const regMap = c.regulations || {}
    const matMap = c.materials   || {}
    trendData.value = {
      labels: months.slice(-9),
      series: [
        { name: '发布法规', color: '#4f46e5', data: months.slice(-9).map(m => regMap[m] || 0) },
        { name: '新增资料', color: '#10b981', data: months.slice(-9).map(m => matMap[m] || 0) }
      ]
    }

    // ============ 3. 地区分布 ============
    const map = await safeCall('regulationMap', () => regulationMap(), { data: [] })
    const mapData = (map.data || []).filter(d => d && d.code)
    const sorted = [...mapData].sort((a, b) => (b.value || 0) - (a.value || 0)).slice(0, 10)
    const maxV = sorted.length ? sorted[0].value : 1
    regionBars.value = sorted.map(d => ({
      name:    d.name || REGION_NAME[d.code] || d.code,
      value:   d.value || 0,
      percent: maxV > 0 ? Math.round(((d.value || 0) / maxV) * 100) : 0
    }))

    // ============ 4. 立法项目仪表盘(补充 AI 任务 + 风险项目) ============
    const projDash = await safeCall('projectDashboard', () => projectDashboard(), { data: {} })
    const pd = projDash.data || {}
    aiStats.value.draft  = pd.recentProjects?.length || 0
    // 让 KPI 卡片多一个"草案总数"(来自其他接口)
    if (kpi.value.draftStats == null) {
      kpi.value.draftStats = { total: pd.recentProjects?.length || 0 }
    }

    // ============ 5. 即将到期项目 Top5 ============
    const up = await safeCall('upcomingProjects', () => upcomingProjects({ days: 30, limit: 5 }), { data: [] })
    upcomingDeadlines.value = (up.data || []).slice(0, 5).map(d => ({
      projectName: d.projectName,
      stageName:   d.stageName || d.deadlineName,
      daysLeft:    d.daysLeft ?? 0
    }))

    // ============ 6. 意见征集热度 Top5 ============
    const cs = await safeCall('listConsultations', () => listConsultations({ page: 1, size: 20 }), { data: {} })
    const records = cs.data?.records || []
    topConsultations.value = [...records]
      .sort((a, b) => (b.totalOpinions || 0) - (a.totalOpinions || 0))
      .slice(0, 5)
    if (kpi.value.opinionStats == null) {
      const total = records.reduce((acc, c) => acc + (c.totalOpinions || 0), 0)
      kpi.value.opinionStats = { total }
    }

    // ============ 7. 清理建议数(用于 AI 任务卡) ============
    const ct = await safeCall('listCleanupTasks', () => listCleanupTasks(), { data: [] })
    aiStats.value.cleanup = (ct.data || []).reduce((acc, t) => acc + (t.suggested || 0), 0)

    // ============ 8. 评估任务数 ============
    const ev = await safeCall('listEvaluations', () => listEvaluations(), { data: {} })
    aiStats.value.review = Array.isArray(ev.data) ? ev.data.length : (ev.data?.records?.length || 0)

    // ============ 9. AI 起草数(从项目仪表盘的 recentProjects 推断) ============
    // 真实可换为 /draft/list 跨项目聚合,这里保留用项目卡数
    aiStats.value.opinion = kpi.value.opinionStats?.total || 0

    loading.value = false
  } catch (e) {
    console.error('[Dashboard] 加载失败', e)
    loading.value = false
    error.value   = true
    errorMsg.value = e?.message || '未知错误,请检查后端 /api/auth/health 是否通畅'
  }
}

onMounted(() => {
  loadAll()
  timer = setInterval(() => { now.value = dayjs().format('YYYY-MM-DD HH:mm:ss') }, 1000)
  startViewCycle()
})
onUnmounted(() => {
  clearInterval(timer)
  stopViewCycle()
})
</script>

<style lang="scss" scoped>
.bigscreen {
  background: #f1f5f9;
  color: $text-regular;
  border-radius: 14px;
  padding: 18px;
  min-height: calc(100vh - 110px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bs-state {
  background: #fff;
  border: 1px solid $border-light;
  border-radius: 12px;
  padding: 18px 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}
.bs-state-loading { color: $primary-color; }
.bs-state-error   {
  color: #f43f5e;
  background: rgba(244, 63, 94, 0.04);
  border-color: rgba(244, 63, 94, 0.3);
}

.bs-header {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  padding: 8px 0 4px;
}
.bs-title { display: flex; align-items: center; gap: 16px; font-size: 20px; font-weight: 700; letter-spacing: 2px; color: $text-primary; }
.bs-line { width: 80px; height: 2px; background: linear-gradient(90deg, transparent, $primary-color); }
.bs-title > .bs-line:last-child { background: linear-gradient(90deg, $primary-color, transparent); }
.bs-clock { position: absolute; right: 0; top: 50%; transform: translateY(-50%); font-size: 14px; color: $primary-color; font-variant-numeric: tabular-nums; font-weight: 600; }

// Day4 新增:视图切换标签
.bs-tabs {
  position: absolute;
  right: 200px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  gap: 8px;
  font-size: 12px;
  span {
    padding: 4px 10px;
    border-radius: 999px;
    background: rgba(79, 70, 229, 0.06);
    color: $text-secondary;
    cursor: pointer;
    transition: all 0.18s;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    &:hover { background: rgba(79, 70, 229, 0.12); }
    &.active {
      background: rgba(79, 70, 229, 0.18);
      color: $primary-color;
      font-weight: 600;
      box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.1);
    }
    i { width: 8px; height: 8px; border-radius: 50%; }
  }
}

.bs-row { display: grid; gap: 16px; }
.bs-row-2 { grid-template-columns: 1.4fr 1fr 1fr 1fr; }
.bs-row-3 { grid-template-columns: 1.1fr 1fr 1fr; }

.bs-card {
  background: #fff;
  border: 1px solid $border-light;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
}
.bs-card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.bs-card-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  font-size: 18px;
  flex-shrink: 0;
  &.blue   { background: linear-gradient(135deg, #6366f1, #4f46e5); }
  &.green  { background: linear-gradient(135deg, #34d399, #10b981); }
  &.amber  { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
  &.rose   { background: linear-gradient(135deg, #fb7185, #f43f5e); }
  &.cyan   { background: linear-gradient(135deg, #67e8f9, #06b6d4); }
  &.violet { background: linear-gradient(135deg, #a78bfa, #8b5cf6); }
}
.bs-card-title { font-size: 15px; font-weight: 600; color: $text-primary; line-height: 1.2; }
.bs-card-subtitle { font-size: 12px; color: $text-secondary; margin-top: 2px; }
.bs-card-legend {
  margin-left: auto;
  display: flex; gap: 12px;
  font-size: 12px; color: $text-secondary;
  span { display: inline-flex; align-items: center; gap: 4px; }
  i { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
}

.bs-kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.bs-kpi {
  display: flex;
  align-items: center;
  gap: 16px;
  border-radius: 14px;
  padding: 18px 20px;
  color: #fff;
  position: relative;
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.08);

  &::after {
    content: '';
    position: absolute;
    right: -30px; top: -30px;
    width: 140px; height: 140px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 50%;
    pointer-events: none;
  }

  &.bs-card-blue   { background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%); }
  &.bs-card-green  { background: linear-gradient(135deg, #34d399 0%, #10b981 100%); }
  &.bs-card-orange { background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%); }
  &.bs-card-violet { background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%); }

  .bs-kpi-icon {
    width: 56px; height: 56px;
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.22);
    display: flex; align-items: center; justify-content: center;
    font-size: 26px;
    flex-shrink: 0;
  }
  .bs-kpi-info { flex: 1; min-width: 0; }
  .bs-kpi-label { font-size: 13px; opacity: 0.85; margin-bottom: 4px; }
  .bs-kpi-value {
    font-size: 30px; font-weight: 700; line-height: 1;
    font-variant-numeric: tabular-nums;
    .unit { font-size: 14px; font-weight: 500; margin-left: 4px; opacity: 0.85; }
  }
  .bs-kpi-trend {
    margin-top: 6px;
    font-size: 12px;
    display: inline-block;
    padding: 2px 8px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.22);
    &.up { background: rgba(255, 255, 255, 0.28); }
    &.down { background: rgba(255, 255, 255, 0.22); }
  }
}

.type-legend {
  list-style: none; padding: 0; margin: 12px 0 0;
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px 16px;
  font-size: 12px;
  li { display: flex; align-items: center; gap: 6px; }
  i { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
  .t-name { color: $text-regular; flex: 1; }
  .t-pct  { color: $text-primary; font-weight: 600; font-variant-numeric: tabular-nums; }
}

.ai-tiles {
  list-style: none; padding: 0; margin: 0;
  display: grid; grid-template-columns: 1fr 1fr; gap: 10px;
  flex: 1;
}
.ai-tile {
  border-radius: 10px;
  padding: 12px 14px;
  color: #fff;
  position: relative;
  overflow: hidden;

  .ai-tile-icon {
    width: 32px; height: 32px;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.22);
    display: flex; align-items: center; justify-content: center;
    font-size: 16px;
    margin-bottom: 8px;
  }
  .ai-tile-label { font-size: 12px; opacity: 0.9; }
  .ai-tile-value {
    font-size: 24px; font-weight: 700; line-height: 1.2;
    margin-top: 2px;
    font-variant-numeric: tabular-nums;
  }
  .ai-tile-meta { font-size: 11px; opacity: 0.85; margin-top: 2px; }

  &.t-violet { background: linear-gradient(135deg, #a78bfa, #8b5cf6); }
  &.t-rose   { background: linear-gradient(135deg, #fb7185, #f43f5e); }
  &.t-amber  { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
  &.t-cyan   { background: linear-gradient(135deg, #67e8f9, #06b6d4); }
}

// 意见征集热度列表
.opinion-list, .deadline-list, .region-grid {
  list-style: none; padding: 0; margin: 0;
  flex: 1;
  overflow: auto;
}
.opinion-list li {
  display: grid;
  grid-template-columns: 24px 1fr 60px;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed $border-light;
  font-size: 13px;
  &:last-child { border-bottom: 0; }
  .op-rank {
    width: 22px; height: 22px;
    border-radius: 6px;
    background: #f1f5f9;
    color: $text-secondary;
    display: flex; align-items: center; justify-content: center;
    font-size: 11px; font-weight: 700;
    &.top {
      background: linear-gradient(135deg, #fb7185, #f43f5e);
      color: #fff;
    }
  }
  .op-title {
    color: $text-regular;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .op-num {
    color: $primary-color;
    font-weight: 700;
    text-align: right;
    font-variant-numeric: tabular-nums;
  }
}

// 即将到期列表
.deadline-list li {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed $border-light;
  &:last-child { border-bottom: 0; }
  .d-meta { min-width: 0; }
  .d-title { font-size: 13px; font-weight: 600; color: $text-primary; }
  .d-project { font-size: 12px; color: $text-secondary; margin-top: 2px;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  }
}

.op-empty {
  text-align: center; color: $text-secondary;
  font-size: 13px; padding: 24px 0; grid-column: 1 / -1;
}

.region-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 24px;
  li {
    display: grid;
    grid-template-columns: 22px 50px 1fr 60px;
    gap: 8px;
    align-items: center;
    font-size: 13px;
    padding: 4px 0;
  }
  .rk {
    width: 22px; height: 22px;
    border-radius: 6px;
    background: #f1f5f9;
    color: $text-secondary;
    display: flex; align-items: center; justify-content: center;
    font-size: 11px; font-weight: 700;
    &.top {
      background: linear-gradient(135deg, #6366f1, #4f46e5);
      color: #fff;
    }
  }
  .rn { color: $text-regular; font-weight: 500; }
  .rb { height: 6px; background: #f1f5f9; border-radius: 3px; overflow: hidden; }
  .rb > div {
    height: 100%;
    transition: width 0.4s;
    background: linear-gradient(90deg, #6366f1, #4f46e5);
  }
  .rv { color: $text-primary; font-weight: 600; text-align: right; font-variant-numeric: tabular-nums; }

  li:nth-child(1)  .rb > div { background: linear-gradient(90deg, #6366f1, #4f46e5); }
  li:nth-child(2)  .rb > div { background: linear-gradient(90deg, #818cf8, #6366f1); }
  li:nth-child(3)  .rb > div { background: linear-gradient(90deg, #34d399, #10b981); }
  li:nth-child(4)  .rb > div { background: linear-gradient(90deg, #67e8f9, #06b6d4); }
  li:nth-child(5)  .rb > div { background: linear-gradient(90deg, #fbbf24, #f59e0b); }
  li:nth-child(6)  .rb > div { background: linear-gradient(90deg, #fcd34d, #f59e0b); }
  li:nth-child(7)  .rb > div { background: linear-gradient(90deg, #fb7185, #f43f5e); }
  li:nth-child(8)  .rb > div { background: linear-gradient(90deg, #a78bfa, #8b5cf6); }
  li:nth-child(9)  .rb > div { background: linear-gradient(90deg, #f472b6, #ec4899); }
  li:nth-child(10) .rb > div { background: linear-gradient(90deg, #93c5fd, #3b82f6); }
}

.status-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; flex: 1; align-content: start; }
.st-cell {
  border-radius: 10px;
  padding: 16px;
  border: 1px solid;
  background: #f8fafc;
  position: relative;
  &.blue   { border-color: rgba(79, 70, 229, 0.25);  background: rgba(79, 70, 229, 0.06); }
  &.green  { border-color: rgba(16, 185, 129, 0.25);  background: rgba(16, 185, 129, 0.06); }
  &.amber  { border-color: rgba(245, 158, 11, 0.25);  background: rgba(245, 158, 11, 0.06); }
  &.rose   { border-color: rgba(244, 63, 94, 0.25);   background: rgba(244, 63, 94, 0.06); }
  .st-label { font-size: 12px; color: $text-secondary; }
  .st-value { font-size: 28px; font-weight: 700; color: $text-primary; margin-top: 4px; font-variant-numeric: tabular-nums; }
  .st-trend { font-size: 11px; color: $text-secondary; margin-top: 4px;
    &.up { color: $status-success; }
    &.down { color: $status-danger; }
  }
}

@media (max-width: 1280px) {
  .bs-kpi-row { grid-template-columns: repeat(2, 1fr); }
  .bs-row-2 { grid-template-columns: 1fr 1fr; }
  .bs-row-3 { grid-template-columns: 1fr 1fr; }
  .bs-card-ai, .bs-card-opinion, .bs-card-deadline { grid-column: span 2; }
}
@media (max-width: 768px) {
  .bs-row-2, .bs-row-3 { grid-template-columns: 1fr; }
  .bs-card-ai, .bs-card-opinion, .bs-card-deadline { grid-column: 1 / -1; }
}
</style>
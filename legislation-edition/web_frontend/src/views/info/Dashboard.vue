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
    </header>

    <!-- KPI 4 大卡 -->
    <section class="bs-kpi-row">
      <div class="bs-kpi bs-card-blue">
        <div class="bs-kpi-icon"><el-icon><Files /></el-icon></div>
        <div class="bs-kpi-info">
          <div class="bs-kpi-label">法规存量</div>
          <div class="bs-kpi-value">2,148</div>
          <div class="bs-kpi-trend up">↑ 本月新增 12</div>
        </div>
      </div>
      <div class="bs-kpi bs-card-green">
        <div class="bs-kpi-icon"><el-icon><CircleCheck /></el-icon></div>
        <div class="bs-kpi-info">
          <div class="bs-kpi-label">本年已发布</div>
          <div class="bs-kpi-value">36</div>
          <div class="bs-kpi-trend up">发布率 63%</div>
        </div>
      </div>
      <div class="bs-kpi bs-card-orange">
        <div class="bs-kpi-icon"><el-icon><EditPen /></el-icon></div>
        <div class="bs-kpi-info">
          <div class="bs-kpi-label">年度立法项目</div>
          <div class="bs-kpi-value">57</div>
          <div class="bs-kpi-trend up">较去年 +8</div>
        </div>
      </div>
      <div class="bs-kpi bs-card-violet">
        <div class="bs-kpi-icon"><el-icon><DataAnalysis /></el-icon></div>
        <div class="bs-kpi-info">
          <div class="bs-kpi-label">平均立法周期</div>
          <div class="bs-kpi-value">218<span class="unit">天</span></div>
          <div class="bs-kpi-trend">同比 -12 天</div>
        </div>
      </div>
    </section>

    <!-- 第二行：趋势 + 类型 + AI 任务 -->
    <section class="bs-row bs-row-2">
      <div class="bs-card bs-card-trend">
        <div class="bs-card-head">
          <span class="bs-card-icon blue"><el-icon><DataLine /></el-icon></span>
          <div>
            <div class="bs-card-title">月度立法趋势</div>
            <div class="bs-card-subtitle">2026 年 1-9 月立项 / 发布 / 清理数量</div>
          </div>
          <div class="bs-card-legend">
            <span><i style="background:#4f46e5"></i>立项</span>
            <span><i style="background:#10b981"></i>发布</span>
            <span><i style="background:#f59e0b"></i>清理</span>
          </div>
        </div>
        <v-chart :option="trendOption" autoresize style="height: 260px" />
      </div>

      <div class="bs-card bs-card-type">
        <div class="bs-card-head">
          <span class="bs-card-icon green"><el-icon><PieChart /></el-icon></span>
          <div>
            <div class="bs-card-title">法规类型分布</div>
            <div class="bs-card-subtitle">现行有效 2,148 部</div>
          </div>
        </div>
        <v-chart :option="pieTypeOption" autoresize style="height: 220px" />
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
          <span class="bs-card-icon violet"><el-icon><MagicStick /></el-icon></span>
          <div>
            <div class="bs-card-title">本周 AI 任务</div>
            <div class="bs-card-subtitle">智能化平台运行概览</div>
          </div>
        </div>
        <ul class="ai-tiles">
          <li class="ai-tile t-violet">
            <div class="ai-tile-icon"><el-icon><EditPen /></el-icon></div>
            <div class="ai-tile-label">AI 起草</div>
            <div class="ai-tile-value">12</div>
            <div class="ai-tile-meta">篇草案</div>
          </li>
          <li class="ai-tile t-rose">
            <div class="ai-tile-icon"><el-icon><View /></el-icon></div>
            <div class="ai-tile-label">智慧审查</div>
            <div class="ai-tile-value">38</div>
            <div class="ai-tile-meta">次审查</div>
          </li>
          <li class="ai-tile t-amber">
            <div class="ai-tile-icon"><el-icon><Brush /></el-icon></div>
            <div class="ai-tile-label">清理建议</div>
            <div class="ai-tile-value">156</div>
            <div class="ai-tile-meta">条建议</div>
          </li>
          <li class="ai-tile t-cyan">
            <div class="ai-tile-icon"><el-icon><ChatDotRound /></el-icon></div>
            <div class="ai-tile-label">意见归类</div>
            <div class="ai-tile-value">642</div>
            <div class="ai-tile-meta">条意见</div>
          </li>
        </ul>
      </div>
    </section>

    <!-- 第三行：意见征集热度 + 即将到期 + 关系网络 -->
    <section class="bs-row bs-row-3">
      <div class="bs-card bs-card-opinion">
        <div class="bs-card-head">
          <span class="bs-card-icon cyan"><el-icon><ChatDotRound /></el-icon></span>
          <div>
            <div class="bs-card-title">意见征集热度</div>
            <div class="bs-card-subtitle">公众参与渠道月度趋势</div>
          </div>
        </div>
        <v-chart :option="oppOption" autoresize style="height: 240px" />
      </div>

      <div class="bs-card bs-card-deadline">
        <div class="bs-card-head">
          <span class="bs-card-icon rose"><el-icon><AlarmClock /></el-icon></span>
          <div>
            <div class="bs-card-title">即将到期任务</div>
            <div class="bs-card-subtitle">最近 30 天截止</div>
          </div>
          <span class="bs-card-badge">5 项</span>
        </div>
        <ul class="dl-list">
          <li v-for="d in deadlines" :key="d.id">
            <span class="dl-name">{{ d.name }}</span>
            <span class="dl-date">{{ d.date }}</span>
            <span class="dl-tag" :class="d.daysLeft < 0 ? 'tag-red' : d.daysLeft < 7 ? 'tag-amber' : 'tag-blue'">
              {{ d.daysLeft < 0 ? `逾期 ${-d.daysLeft} 天` : `${d.daysLeft} 天` }}
            </span>
          </li>
        </ul>
      </div>

      <div class="bs-card bs-card-graph">
        <div class="bs-card-head">
          <span class="bs-card-icon blue"><el-icon><Share /></el-icon></span>
          <div>
            <div class="bs-card-title">法规关系网络</div>
            <div class="bs-card-subtitle">上位 / 当前 / 下位法引用关系</div>
          </div>
        </div>
        <v-chart :option="graphOption" autoresize style="height: 260px" />
      </div>
    </section>

    <!-- 第四行：地区分布 + 状态 -->
    <section class="bs-row bs-row-4">
      <div class="bs-card bs-card-region">
        <div class="bs-card-head">
          <span class="bs-card-icon amber"><el-icon><Location /></el-icon></span>
          <div>
            <div class="bs-card-title">地区立法 Top 10</div>
            <div class="bs-card-subtitle">截至 2026 年 9 月</div>
          </div>
        </div>
        <ul class="region-grid">
          <li v-for="(r, idx) in regionBars" :key="r.name">
            <span class="rk" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
            <span class="rn">{{ r.name }}</span>
            <div class="rb"><div :style="{ width: r.percent + '%' }"></div></div>
            <span class="rv">{{ r.value }}</span>
          </li>
        </ul>
      </div>

      <div class="bs-card bs-card-stat">
        <div class="bs-card-head">
          <span class="bs-card-icon violet"><el-icon><DataBoard /></el-icon></span>
          <div>
            <div class="bs-card-title">法规状态分布</div>
            <div class="bs-card-subtitle">现行 / 修订 / 废止 / 预警</div>
          </div>
        </div>
        <div class="status-grid">
          <div class="st-cell blue">
            <div class="st-label">现行有效</div>
            <div class="st-value">2,148</div>
            <div class="st-trend up">↑ 12</div>
          </div>
          <div class="st-cell amber">
            <div class="st-label">修订中</div>
            <div class="st-value">26</div>
            <div class="st-trend">— 持平</div>
          </div>
          <div class="st-cell green">
            <div class="st-label">已废止</div>
            <div class="st-value">521</div>
            <div class="st-trend down">↓ 3</div>
          </div>
          <div class="st-cell rose">
            <div class="st-label">到期预警</div>
            <div class="st-value">8</div>
            <div class="st-trend">— 6 个月内</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'

const now = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))
let timer

const pieColors = ['#4f46e5', '#10b981', '#f59e0b', '#f43f5e']

const typeBreakdown = [
  { name: '行政法规',     pct: 13 },
  { name: '部门规章',     pct: 48 },
  { name: '地方政府规章', pct: 39 }
]

const regionBars = ref([
  { name: '北京', value: 248, percent: 100 },
  { name: '上海', value: 198, percent: 80 },
  { name: '广东', value: 186, percent: 75 },
  { name: '江苏', value: 174, percent: 70 },
  { name: '浙江', value: 156, percent: 63 },
  { name: '山东', value: 138, percent: 56 },
  { name: '四川', value: 126, percent: 51 },
  { name: '湖北', value: 112, percent: 45 },
  { name: '河南', value: 108, percent: 44 },
  { name: '福建', value: 96,  percent: 39 }
])

const deadlines = ref([
  { id: 1, name: '网络数据安全管理条例 · 征求意见', date: '2026-10-05', daysLeft: 2 },
  { id: 2, name: '某省医疗保障办法 · 法制审查',     date: '2026-10-08', daysLeft: 5 },
  { id: 3, name: '某市人才公寓办法 · 部门会签',     date: '2026-10-12', daysLeft: 9 },
  { id: 4, name: '养老服务促进条例 · 公布',         date: '2026-09-30', daysLeft: -3 },
  { id: 5, name: '烟花安全管理规定 · 立项审查',     date: '2026-10-25', daysLeft: 22 }
])

onMounted(() => {
  timer = setInterval(() => { now.value = dayjs().format('YYYY-MM-DD HH:mm:ss') }, 1000)
})
onUnmounted(() => clearInterval(timer))
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

/* ============ Header ============ */
.bs-header {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  padding: 8px 0 4px;
}
.bs-title { display: flex; align-items: center; gap: 16px; font-size: 20px; font-weight: 700; letter-spacing: 2px; color: $text-primary; }
.bs-line {
  width: 80px; height: 2px;
  background: linear-gradient(90deg, transparent, $primary-color);
}
.bs-title > .bs-line:last-child { background: linear-gradient(90deg, $primary-color, transparent); }
.bs-clock { position: absolute; right: 0; top: 50%; transform: translateY(-50%); font-size: 14px; color: $primary-color; font-variant-numeric: tabular-nums; font-weight: 600; }

/* ============ Row 公共 ============ */
.bs-row { display: grid; gap: 16px; }
.bs-row-2 { grid-template-columns: 1.5fr 1fr 1fr; }
.bs-row-3 { grid-template-columns: 1fr 1fr 1.3fr; }
.bs-row-4 { grid-template-columns: 1.2fr 1fr; }

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
.bs-card-badge {
  margin-left: auto;
  background: rgba(244, 63, 94, 0.1);
  color: #f43f5e;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
}

/* ============ KPI 4 大卡 ============ */
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

/* ============ 类型分布 legend ============ */
.type-legend {
  list-style: none; padding: 0; margin: 4px 0 0;
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px 16px;
  font-size: 12px;
  li { display: flex; align-items: center; gap: 6px; }
  i { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
  .t-name { color: $text-regular; flex: 1; }
  .t-pct  { color: $text-primary; font-weight: 600; font-variant-numeric: tabular-nums; }
}

/* ============ AI 任务小卡 ============ */
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

/* ============ 到期列表 ============ */
.dl-list { list-style: none; padding: 0; margin: 0; }
.dl-list li {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 0;
  border-bottom: 1px dashed $border-light;
  font-size: 13px;
  &:last-child { border-bottom: none; }
}
.dl-name { flex: 1; color: $text-regular; }
.dl-date { color: $text-secondary; font-size: 12px; font-variant-numeric: tabular-nums; }
.dl-tag {
  font-size: 11px; font-weight: 600;
  padding: 2px 8px; border-radius: 999px;
  &.tag-red   { background: rgba(244, 63, 94, 0.12);  color: #f43f5e; }
  &.tag-amber { background: rgba(245, 158, 11, 0.12); color: #f59e0b; }
  &.tag-blue  { background: rgba(79, 70, 229, 0.12);  color: #4f46e5; }
}

/* ============ 地区 grid（每行一种颜色） ============ */
.region-grid {
  list-style: none; padding: 0; margin: 0;
  display: grid; grid-template-columns: 1fr 1fr; gap: 6px 24px;
  flex: 1;
  li {
    display: grid;
    grid-template-columns: 22px 50px 1fr 50px;
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

/* ============ 状态 4 格 ============ */
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
  .bs-card-ai { grid-column: 1 / -1; }
  .bs-row-3 { grid-template-columns: 1fr 1fr; }
  .bs-card-graph { grid-column: 1 / -1; }
  .bs-row-4 { grid-template-columns: 1fr; }
}
</style>
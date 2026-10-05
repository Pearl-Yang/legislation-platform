<template>
  <view class="evaluation-detail" v-if="task">
    <!-- 综合分 -->
    <view class="card score-card">
      <view class="sc-row">
        <view class="sc-num">{{ num(task.overallScore) }}</view>
        <view class="sc-text">
          <view class="sc-label">综合得分</view>
          <view class="sc-period">{{ task.periodStart }} ~ {{ task.periodEnd }}</view>
        </view>
        <view :class="['tag', `tag-${(task.status||'').toLowerCase()}`]">
          {{ statusLabel(task.status) }}
        </view>
      </view>
    </view>

    <!-- 三维度（条形可视化） -->
    <view class="card">
      <view class="card-title">📊 维度评分</view>
      <view v-for="d in dims" :key="d.key" class="dim-row">
        <view class="dim-key">
          <view class="dim-tag" :style="{ background: d.color }"></view>
          <text>{{ d.label }}</text>
        </view>
        <view class="dim-bar">
          <view class="dim-fill" :style="{ width: dimScore(d.key) + '%', background: d.color }"></view>
        </view>
        <text class="dim-value">{{ dimScore(d.key).toFixed(1) }}</text>
      </view>
    </view>

    <!-- 图表 -->
    <view class="card">
      <view class="card-title">📈 趋势 / 对比</view>
      <view v-if="chart && chart.radar" class="chart-wrap">
        <view class="radar">
          <view class="radar-grid">
            <view class="rg" v-for="n in 4" :key="n" :style="{ transform: `scale(${n/4})` }"></view>
          </view>
          <view
            v-for="(p, i) in chart.radar"
            :key="i"
            class="radar-point"
            :style="{ left: p.x + '%', top: p.y + '%', background: p.color }"
          ></view>
        </view>
        <view class="legend">
          <text v-for="(p, i) in chart.radar" :key="i" class="legend-item">
            <view class="legend-dot" :style="{ background: p.color }"></view>
            {{ p.name }} {{ p.value }}
          </text>
        </view>
      </view>
      <empty v-else text="暂无图表数据" desc="任务完成后将自动生成" />
    </view>

    <!-- 报告 -->
    <view class="card" v-if="task.reportContent">
      <view class="card-title">📋 评估报告</view>
      <view class="report">{{ task.reportContent }}</view>
    </view>

    <view class="action-bar">
      <button class="btn-secondary" @click="exportReport">⬇ 导出报告</button>
      <button class="btn-primary"   @click="syncData">🔄 同步执法数据</button>
    </view>
  </view>
  <empty v-else-if="!loading" text="评估任务不存在" />
  <loading-block v-else text="加载中…" />
</template>

<script>
import Empty from '@/components/Empty.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import { EVALUATION_STATUS, DIMENSION, num, showLoading, hideLoading } from '@/utils/index.js'
import { evaluationApi } from '@/api/index.js'

export default {
  components: { Empty, LoadingBlock },
  data() {
    return {
      id: null, task: null, chart: null, loading: true,
      dims: [
        { key: 'LEGALITY',     label: '合法性',  color: '#1e5a96' },
        { key: 'EXECUTION',    label: '落实性',  color: '#67c23a' },
        { key: 'SATISFACTION', label: '满意度',  color: '#e6a23c' }
      ]
    }
  },
  onLoad(opts) { this.id = Number(opts.id) || null },
  onShow() { this.reload() },
  methods: {
    statusLabel, num,

    dimScore(key) {
      if (!this.chart || !this.chart.radar) return 0
      const item = this.chart.radar.find(r => r.key === key)
      return item ? Number(item.value || 0) : 0
    },

    async reload() {
      this.loading = true
      try {
        const [detail, chart] = await Promise.allSettled([
          evaluationApi.evaluationDetail(this.id),
          evaluationApi.evaluationChart(this.id)
        ])
        if (detail.status === 'fulfilled') this.task = detail.value
        if (chart.status === 'fulfilled')  this.chart = chart.value
      } finally {
        this.loading = false
      }
    },

    async exportReport() {
      showLoading()
      try {
        const r = await evaluationApi.evaluationReport(this.id, 'HTML')
        uni.setClipboardData({ data: r?.content || '', success: () => uni.showToast({ title: '报告已复制', icon: 'none' }) })
      } finally {
        hideLoading()
      }
    },

    async syncData() {
      if (!this.task) return
      showLoading('同步执法数据…')
      try {
        await evaluationApi.syncData(this.task.periodStart, this.task.periodEnd)
        uni.showToast({ title: '同步完成', icon: 'success' })
        this.reload()
      } finally {
        hideLoading()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.evaluation-detail { padding: 24rpx 24rpx 200rpx; }

.score-card {
  background: linear-gradient(120deg, #67c23a, #95d475);
  color: #fff;
  border-radius: 16rpx;
}
.sc-row { display: flex; align-items: center; gap: 16rpx; }
.sc-num {
  font-size: 72rpx;
  font-weight: 700;
  line-height: 1;
  flex-shrink: 0;
}
.sc-text { flex: 1; }
.sc-label { font-size: 24rpx; opacity: 0.85; }
.sc-period { font-size: 24rpx; opacity: 0.85; margin-top: 4rpx; }
.tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  background: rgba(255,255,255,0.2);
  color: #fff;
  flex-shrink: 0;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}
.card-title::before {
  content: '';
  display: inline-block;
  width: 8rpx;
  height: 28rpx;
  background: #67c23a;
  margin-right: 12rpx;
  border-radius: 4rpx;
}

.dim-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 12rpx 0;
}
.dim-key {
  display: flex;
  align-items: center;
  gap: 8rpx;
  width: 120rpx;
  font-size: 26rpx;
  color: #1f2937;
  flex-shrink: 0;
}
.dim-tag {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.dim-bar {
  flex: 1;
  height: 16rpx;
  background: #f0f4f8;
  border-radius: 8rpx;
  overflow: hidden;
}
.dim-fill { height: 100%; }
.dim-value {
  width: 64rpx;
  text-align: right;
  font-size: 26rpx;
  color: #1f2937;
  font-weight: 600;
  flex-shrink: 0;
}

.chart-wrap { padding: 16rpx 0; }
.radar {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  max-height: 360rpx;
  margin: 0 auto;
}
.radar-grid {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.rg {
  width: 80%;
  aspect-ratio: 1;
  border: 2rpx solid #e5e7eb;
  border-radius: 50%;
  position: absolute;
}
.radar-point {
  position: absolute;
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 0 0 6rpx rgba(0,0,0,0.05);
}
.legend {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 16rpx;
}
.legend-item {
  font-size: 22rpx;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.legend-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}

.report {
  background: #f9fafb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 24rpx;
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 600rpx;
  overflow-y: auto;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #f3f4f6;
  display: flex;
  gap: 16rpx;
  z-index: 9;
}
.btn-secondary, .btn-primary {
  flex: 1;
  border-radius: 16rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
}
.btn-secondary { background: #f5f7fa; color: #1e5a96; }
.btn-primary   { background: linear-gradient(90deg, #67c23a, #95d475); color: #fff; }
.btn-secondary::after, .btn-primary::after { border: none; }
</style>
<template>
  <view class="evaluation-page">
    <view class="hero">
      <view class="hero-title">实施评估</view>
      <view class="hero-desc">合法性 · 落实性 · 满意度 三维评分</view>
    </view>

    <!-- 三维概览 -->
    <view class="dim-grid">
      <view class="dim-cell dim-legality">
        <view class="dim-value">{{ avg('LEGALITY') }}</view>
        <view class="dim-label">合法性</view>
      </view>
      <view class="dim-cell dim-execution">
        <view class="dim-value">{{ avg('EXECUTION') }}</view>
        <view class="dim-label">落实性</view>
      </view>
      <view class="dim-cell dim-satisfaction">
        <view class="dim-value">{{ avg('SATISFACTION') }}</view>
        <view class="dim-label">满意度</view>
      </view>
    </view>

    <!-- 状态过滤 -->
    <filter-pills
      :options="statusOpts"
      v-model="filter.status"
    />

    <!-- 任务列表 -->
    <view class="list">
      <view
        v-for="t in tasks"
        :key="t.id"
        class="card"
        @click="goDetail(t.id)"
      >
        <view class="row1">
          <view>
            <view class="title">评估任务 #{{ t.id }}</view>
            <view class="text-secondary fz-12">法规 #{{ t.regulationId }}</view>
          </view>
          <view :class="['tag', `tag-${(t.status || '').toLowerCase()}`]">
            {{ statusLabel(t.status) }}
          </view>
        </view>
        <view class="meta">
          <text class="text-secondary fz-12">{{ t.periodStart }} ~ {{ t.periodEnd }}</text>
          <text class="text-secondary fz-12 ml-12">综合 {{ num(t.overallScore) }}</text>
        </view>
      </view>

      <loading-block v-if="loading" text="加载中…" />
      <empty v-if="!loading && tasks.length === 0" text="暂无评估任务" desc="可在 Web 端创建评估后查看" />
    </view>

    <!-- 指标库入口 -->
    <view class="card" v-if="indicators.length > 0">
      <view class="card-title">📐 指标库</view>
      <view v-for="i in indicators" :key="i.id" class="ind-row">
        <view :class="['ind-dim', `dim-${(i.dimension||'').toLowerCase()}`]">{{ dimLabel(i.dimension) }}</view>
        <view class="ind-body">
          <view class="ind-name">{{ i.indicatorName }}</view>
          <view class="ind-meta">权重 {{ i.weight }} · 来源 {{ i.dataSource || '—' }}</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import FilterPills from '@/components/FilterPills.vue'
import Empty       from '@/components/Empty.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import { EVALUATION_STATUS, DIMENSION, num } from '@/utils/index.js'
import { evaluationApi } from '@/api/index.js'

export default {
  components: { FilterPills, Empty, LoadingBlock },
  data() {
    return {
      filter: { status: '' },
      statusOpts: [
        { label: '全部',   value: '' },
        { label: '待执行', value: 'PENDING' },
        { label: '进行中', value: 'RUNNING' },
        { label: '已完成', value: 'COMPLETED' }
      ],
      tasks: [],
      indicators: [],
      loading: false
    }
  },
  watch: {
    'filter.status': 'reload'
  },
  onShow() { this.reload(); this.loadIndicators() },
  methods: {
    statusLabel,
    dimLabel(d)  { return (DIMENSION[d] || { label: d }).label },
    num,

    avg(dim) {
      const ind = this.indicators.filter(i => i.dimension === dim)
      if (!ind.length) return '—'
      const total = ind.reduce((s, x) => s + Number(x.weight || 0), 0)
      if (total === 0) return '—'
      const weighted = ind.reduce((s, x) => s + Number(x.weight || 0) * Number(x.normalized || 0), 0)
      return (weighted / total).toFixed(1)
    },

    async reload() {
      this.loading = true
      try {
        const list = await evaluationApi.listEvaluations(this.filter.status, null)
        this.tasks = Array.isArray(list) ? list : []
      } catch (e) {
        this.tasks = []
      } finally {
        this.loading = false
      }
    },

    async loadIndicators() {
      try {
        const list = await evaluationApi.listIndicators()
        this.indicators = Array.isArray(list) ? list : []
      } catch (e) { this.indicators = [] }
    },

    goDetail(id) { uni.navigateTo({ url: `/pages/evaluation/detail?id=${id}` }) }
  }
}
</script>

<style lang="scss" scoped>
.evaluation-page { padding-bottom: 240rpx; }

.hero {
  background: linear-gradient(120deg, #67c23a, #95d475);
  color: #fff;
  padding: 32rpx;
}
.hero-title { font-size: 36rpx; font-weight: 700; }
.hero-desc  { font-size: 24rpx; opacity: 0.9; margin-top: 8rpx; }

.dim-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  background: #fff;
  padding: 24rpx 8rpx;
}
.dim-cell {
  text-align: center;
  padding: 16rpx;
  border-radius: 16rpx;
  margin: 0 4rpx;
}
.dim-cell {
  background: #f5f7fa;
}
.dim-legality    { background: #ecf5fc; }
.dim-execution   { background: #e8f7e6; }
.dim-satisfaction{ background: #fdf6ec; }
.dim-value { font-size: 36rpx; font-weight: 700; color: #1f2937; }
.dim-label { font-size: 22rpx; color: #6b7280; margin-top: 4rpx; }

.list { padding: 16rpx 24rpx; }

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(15, 35, 60, 0.04);
}
.row1 { display: flex; align-items: flex-start; justify-content: space-between; }
.title { font-size: 28rpx; font-weight: 600; color: #1f2937; }
.tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}
.tag-pending   { background: #f4f4f5; color: #909399; }
.tag-running   { background: #ecf5fc; color: #1e5a96; }
.tag-completed { background: #e8f7e6; color: #67c23a; }
.text-secondary { color: #909399; }
.fz-12 { font-size: 24rpx; }
.ml-12 { margin-left: 12rpx; }

.meta { margin-top: 8rpx; display: flex; gap: 8rpx; }

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

.ind-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child { border-bottom: none; }
}
.ind-dim {
  width: 80rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8rpx;
  font-size: 22rpx;
  color: #fff;
  flex-shrink: 0;
}
.dim-legality    { background: #1e5a96; }
.dim-execution   { background: #67c23a; }
.dim-satisfaction{ background: #e6a23c; }
.ind-body { flex: 1; min-width: 0; }
.ind-name { font-size: 26rpx; color: #1f2937; }
.ind-meta { font-size: 22rpx; color: #6b7280; margin-top: 4rpx; }
</style>
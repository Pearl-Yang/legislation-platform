<template>
  <view class="cleanup-detail" v-if="task">
    <view class="card">
      <view class="row1">
        <text class="title">{{ task.taskName }}</text>
        <view :class="['tag', `tag-${(task.status || '').toLowerCase()}`]">
          {{ statusLabel(task.status) }}
        </view>
      </view>
      <view class="row2">
        <view class="type-tag" :style="{ background: typeColor(task.taskType) }">
          {{ typeLabel(task.taskType) }}
        </view>
        <text class="text-secondary fz-12" v-if="task.theme">主题：{{ task.theme }}</text>
      </view>
      <view class="meta">
        <text class="meta-key">创建</text>
        <text>{{ relativeTime(task.createdAt) }}</text>
      </view>
      <view class="meta" v-if="task.triggerRegulationId">
        <text class="meta-key">触发上位法</text>
        <text>#{{ task.triggerRegulationId }}</text>
      </view>
    </view>

    <view class="action-bar">
      <button class="btn-secondary" @click="loadAffected">👁 受影响法规</button>
      <button class="btn-primary"   @click="onSuggest">⚡ AI 生成建议</button>
    </view>

    <!-- 受影响 -->
    <view class="card" v-if="affected.length > 0">
      <view class="card-title">受影响法规</view>
      <view v-for="r in affected" :key="r.id" class="aff-row">
        <view class="aff-name">{{ r.regulationName }}</view>
        <view :class="['reg-tag', `reg-${(r.status || '').toLowerCase()}`]">
          {{ regStatusLabel(r.status) }}
        </view>
      </view>
    </view>

    <!-- 报告 -->
    <view class="card" v-if="reportContent">
      <view class="card-title">📋 清理报告</view>
      <view class="report">{{ reportContent }}</view>
    </view>
  </view>
  <empty v-else-if="!loading" text="任务不存在" />
  <loading-block v-else text="加载中…" />
</template>

<script>
import Empty from '@/components/Empty.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import {
  CLEANUP_TYPE, CLEANUP_STATUS, REGULATION_STATUS,
  relativeTime, showLoading, hideLoading
} from '@/utils/index.js'
import { cleanupApi } from '@/api/index.js'

export default {
  components: { Empty, LoadingBlock },
  data() {
    return {
      id: null, task: null, loading: true,
      affected: [], reportContent: ''
    }
  },
  onLoad(opts) { this.id = Number(opts.id) || null },
  onShow() { this.reload() },
  methods: {
    typeLabel(t)  { return (CLEANUP_TYPE[t] || { label: t }).label },
    typeColor(t)  { return (CLEANUP_TYPE[t] || { color: '#909399' }).color },
    statusLabel(s){ return (CLEANUP_STATUS[s] || { label: s }).label },
    regStatusLabel(s) { return (REGULATION_STATUS[s] || { label: s }).label },
    relativeTime,

    async reload() {
      this.loading = true
      try {
        this.task = await cleanupApi.cleanupDetail(this.id)
      } finally {
        this.loading = false
      }
    },

    async loadAffected() {
      showLoading()
      try {
        const list = await cleanupApi.affectedRegulations(this.id)
        this.affected = Array.isArray(list) ? list : []
      } finally {
        hideLoading()
      }
    },

    async onSuggest() {
      showLoading('AI 生成建议中…')
      try {
        await cleanupApi.suggestCleanup(this.id)
        uni.showToast({ title: '已生成建议', icon: 'success' })
        // 顺便加载报告
        const rep = await cleanupApi.cleanupReport(this.id)
        this.reportContent = (rep && (rep.summary || rep.content)) || '已生成清理建议'
      } finally {
        hideLoading()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.cleanup-detail { padding: 24rpx 24rpx 200rpx; }
.row1 { display: flex; align-items: flex-start; justify-content: space-between; gap: 16rpx; }
.title { font-size: 32rpx; font-weight: 700; color: #1f2937; flex: 1; }
.tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}
.tag-pending { background: #f4f4f5; color: #909399; }
.tag-running { background: #ecf5fc; color: #1e5a96; }
.tag-done    { background: #e8f7e6; color: #67c23a; }

.row2 { display: flex; flex-wrap: wrap; gap: 12rpx; align-items: center; margin-top: 12rpx; }
.type-tag {
  font-size: 22rpx;
  color: #fff;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}
.text-secondary { color: #909399; }
.fz-12 { font-size: 24rpx; }
.meta {
  margin-top: 12rpx;
  display: flex;
  gap: 8rpx;
  font-size: 24rpx;
  color: #1f2937;
}
.meta-key { color: #909399; }

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
.btn-primary   { background: linear-gradient(90deg, #e6a23c, #f7b977); color: #fff; }
.btn-secondary::after, .btn-primary::after { border: none; }

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
  background: #e6a23c;
  margin-right: 12rpx;
  border-radius: 4rpx;
}

.aff-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 8rpx;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child { border-bottom: none; }
}
.aff-name { font-size: 26rpx; color: #1f2937; }
.reg-tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}
.reg-effective { background: #e8f7e6; color: #67c23a; }
.reg-revising  { background: #fdf6ec; color: #e6a23c; }
.reg-obsolete  { background: #fef0f0; color: #f56c6c; }

.report {
  background: #f9fafb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 24rpx;
  color: #1f2937;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
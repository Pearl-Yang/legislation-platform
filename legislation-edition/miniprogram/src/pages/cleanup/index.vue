<template>
  <view class="cleanup-page">
    <view class="hero">
      <view class="hero-title">智能清理</view>
      <view class="hero-desc">日常 / 定期 / 专项 三种模式 · AI 自动出建议</view>
    </view>

    <!-- 顶部统计 -->
    <view class="stat-grid">
      <view class="stat-cell">
        <view class="stat-value">{{ summary.total }}</view>
        <view class="stat-label">任务总数</view>
      </view>
      <view class="stat-cell">
        <view class="stat-value" style="color:#1e5a96">{{ summary.running }}</view>
        <view class="stat-label">进行中</view>
      </view>
      <view class="stat-cell">
        <view class="stat-value" style="color:#67c23a">{{ summary.done }}</view>
        <view class="stat-label">已完成</view>
      </view>
      <view class="stat-cell">
        <view class="stat-value" style="color:#909399">{{ summary.pending }}</view>
        <view class="stat-label">待执行</view>
      </view>
    </view>

    <!-- 类型过滤 -->
    <filter-pills
      :options="typeOpts"
      v-model="filter.type"
    />
    <filter-pills
      :options="statusOpts"
      v-model="filter.status"
    />

    <!-- 入口 -->
    <view class="quick-row">
      <view class="quick-btn" @click="goGraph">
        <view class="qi-icon" style="background:linear-gradient(135deg,#1e5a96,#4a86c5)">图</view>
        <view>
            <view class="qi-title">法规关系图谱</view>
            <view class="qi-desc">上下位 · 引用 · 替代</view>
          </view>
      </view>
      <view class="quick-btn" @click="onNew">
        <view class="qi-icon" style="background:linear-gradient(135deg,#e6a23c,#f7b977)">+</view>
        <view>
            <view class="qi-title">新建清理任务</view>
            <view class="qi-desc">支持三种触发模式</view>
          </view>
      </view>
    </view>

    <!-- 任务列表 -->
    <view class="list">
      <view
        v-for="t in tasks"
        :key="t.id"
        class="card"
        @click="goDetail(t.id)"
      >
        <view class="row1">
          <text class="name">{{ t.taskName }}</text>
          <view :class="['tag', `tag-${(t.status || '').toLowerCase()}`]">
            {{ statusLabel(t.status) }}
          </view>
        </view>
        <view class="row2">
          <view class="type-tag" :style="{ background: typeColor(t.taskType) }">
            {{ typeLabel(t.taskType) }}
          </view>
          <text class="text-secondary fz-12" v-if="t.theme">主题：{{ t.theme }}</text>
          <text class="text-secondary fz-12 ml-12" v-if="t.triggerRegulationId">
            上位法 #{{ t.triggerRegulationId }}
          </text>
        </view>
        <view class="meta">
          <text class="text-secondary fz-12">创建 {{ relativeTime(t.createdAt) }}</text>
          <text class="text-secondary fz-12 ml-12" v-if="t.completedAt">
            完成 {{ relativeTime(t.completedAt) }}
          </text>
        </view>
      </view>

      <loading-block v-if="loading" text="加载中…" />
      <empty v-if="!loading && tasks.length === 0" text="暂无清理任务" desc="点击右上角「新建任务」开始" />
    </view>

    <!-- 新建弹层 -->
    <view v-if="showNew" class="modal-mask" @click.self="showNew = false">
      <view class="modal">
        <view class="modal-title">＋ 新建清理任务</view>
        <view class="form-row">
          <text class="form-key">任务名称</text>
          <input v-model="newTask.taskName" class="input" placeholder="例如：2026 Q4 数据安全专项清理" />
        </view>
        <view class="form-row">
          <text class="form-key">任务类型</text>
          <picker mode="selector" :range="typeLabels" @change="onTypeChange">
            <view class="picker">
              {{ newTask.taskType ? typeLabels[newTask.taskType] : '请选择' }}
              <text class="picker-arrow">▾</text>
            </view>
          </picker>
        </view>
        <view class="form-row" v-if="newTask.taskType === 'THEMATIC'">
          <text class="form-key">主题关键词</text>
          <input v-model="newTask.theme" class="input" placeholder="如：数据安全" />
        </view>
        <view class="form-row">
          <text class="form-key">触发上级法 ID（可选）</text>
          <input v-model="newTask.triggerRegulationId" type="number" class="input" placeholder="如：101" />
        </view>
        <view class="modal-actions">
          <view class="modal-btn cancel"  @click="showNew = false">取消</view>
          <view class="modal-btn confirm" @click="confirmNew">创建</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import FilterPills from '@/components/FilterPills.vue'
import Empty       from '@/components/Empty.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import {
  CLEANUP_TYPE, CLEANUP_STATUS, relativeTime,
  showLoading, hideLoading
} from '@/utils/index.js'
import { cleanupApi } from '@/api/index.js'

export default {
  components: { FilterPills, Empty, LoadingBlock },
  data() {
    return {
      filter: { type: '', status: '' },
      typeOpts: [
        { label: '全部类型', value: '' },
        { label: '日常清理', value: 'DAILY' },
        { label: '定期清理', value: 'PERIODIC' },
        { label: '专项清理', value: 'THEMATIC' }
      ],
      statusOpts: [
        { label: '全部状态', value: '' },
        { label: '待执行',   value: 'PENDING' },
        { label: '进行中',   value: 'RUNNING' },
        { label: '已完成',   value: 'DONE' }
      ],
      tasks: [],
      summary: { total: 0, pending: 0, running: 0, done: 0 },
      loading: false,
      showNew: false,
      newTask: { taskName: '', taskType: '', theme: '', triggerRegulationId: null }
    }
  },
  computed: {
    typeLabels() { return this.typeOpts.map(o => o.label) }
  },
  watch: {
    'filter.type':   'reload',
    'filter.status': 'reload'
  },
  onShow() { this.reload() },
  onPullDownRefresh() { this.reload().then(() => uni.stopPullDownRefresh()) },
  methods: {
    typeLabel(t)  { return (CLEANUP_TYPE[t] || { label: t }).label },
    typeColor(t)  { return (CLEANUP_TYPE[t] || { color: '#909399' }).color },
    statusLabel(s){ return (CLEANUP_STATUS[s] || { label: s }).label },
    relativeTime,

    async reload() {
      this.loading = true
      try {
        const list = await cleanupApi.listCleanupTasks(this.filter.status, this.filter.type)
        this.tasks = Array.isArray(list) ? list : []
        this.summary = {
          total:   this.tasks.length,
          pending: this.tasks.filter(t => t.status === 'PENDING').length,
          running: this.tasks.filter(t => t.status === 'RUNNING').length,
          done:    this.tasks.filter(t => t.status === 'DONE').length
        }
      } catch (e) {
        this.tasks = []
      } finally {
        this.loading = false
      }
    },

    goDetail(id) { uni.navigateTo({ url: `/pages/cleanup/detail?id=${id}` }) },
    goGraph()    { uni.navigateTo({ url: '/pages/cleanup/graph' }) },

    onNew()  {
      this.newTask = { taskName: '', taskType: '', theme: '', triggerRegulationId: null }
      this.showNew = true
    },

    onTypeChange(e) {
      const i = Number(e.detail.value)
      const v = this.typeOpts[i]?.value
      this.newTask.taskType = v
    },

    async confirmNew() {
      if (!this.newTask.taskName.trim() || !this.newTask.taskType) {
        uni.showToast({ title: '请填写任务名与类型', icon: 'none' })
        return
      }
      this.showNew = false
      showLoading()
      try {
        const body = {
          taskName: this.newTask.taskName,
          taskType: this.newTask.taskType,
          theme: this.newTask.theme,
          triggerRegulationId: this.newTask.triggerRegulationId || null,
          cleanupMode: 'HYBRID',
          createdBy: uni.getStorageSync('userId') || 1
        }
        await cleanupApi.createCleanupTask(body)
        uni.showToast({ title: '已创建', icon: 'success' })
        this.reload()
      } finally {
        hideLoading()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.cleanup-page { padding-bottom: 240rpx; }

.hero {
  background: linear-gradient(120deg, #e6a23c, #f7b977);
  color: #fff;
  padding: 32rpx;
}
.hero-title { font-size: 36rpx; font-weight: 700; }
.hero-desc  { font-size: 24rpx; opacity: 0.9; margin-top: 8rpx; }

.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  background: #fff;
  padding: 24rpx 8rpx;
}
.stat-cell  { text-align: center; }
.stat-value { font-size: 36rpx; font-weight: 700; color: #1f2937; }
.stat-label { font-size: 22rpx; color: #6b7280; margin-top: 4rpx; }

.quick-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #f3f4f6;
}
.quick-btn {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
}
.qi-icon {
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
.qi-title { font-size: 28rpx; font-weight: 500; color: #1f2937; }
.qi-desc  { font-size: 22rpx; color: #6b7280; margin-top: 4rpx; }

.list { padding: 16rpx 24rpx; }

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(15, 35, 60, 0.04);
}
.row1 { display: flex; justify-content: space-between; gap: 16rpx; align-items: center; }
.name { font-size: 30rpx; font-weight: 600; color: #1f2937; flex: 1; }
.tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
  background: #f0f4f8;
  color: #6b7280;
}
.tag-pending { background: #f4f4f5; color: #909399; }
.tag-running { background: #ecf5fc; color: #1e5a96; }
.tag-done    { background: #e8f7e6; color: #67c23a; }

.row2 {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12rpx;
  margin: 12rpx 0;
}
.type-tag {
  font-size: 22rpx;
  color: #fff;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}
.text-secondary { color: #909399; }
.fz-12 { font-size: 24rpx; }
.ml-12 { margin-left: 12rpx; }

// 弹层
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: flex-end;
  z-index: 99;
}
.modal {
  width: 100%;
  background: #fff;
  border-top-left-radius: 24rpx;
  border-top-right-radius: 24rpx;
  padding: 32rpx 24rpx;
}
.modal-title { font-size: 32rpx; font-weight: 600; margin-bottom: 16rpx; }
.form-row    { margin-bottom: 24rpx; }
.form-key    { font-size: 26rpx; color: #1f2937; margin-bottom: 8rpx; display: block; }
.input {
  width: 100%;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}
.picker {
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx 24rpx;
  font-size: 26rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #1f2937;
}
.picker-arrow { color: #909399; }

.modal-actions { display: flex; gap: 16rpx; }
.modal-btn {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  border-radius: 16rpx;
  font-size: 28rpx;
}
.modal-btn.cancel  { background: #f5f7fa; color: #6b7280; }
.modal-btn.confirm { background: linear-gradient(90deg, #e6a23c, #f7b977); color: #fff; }
</style>
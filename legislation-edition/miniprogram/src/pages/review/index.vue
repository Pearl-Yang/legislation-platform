<template>
  <view class="review-page">
    <view class="hero">
      <view class="hero-title">智慧审查</view>
      <view class="hero-desc">红黄蓝灰四级风险 · 规则引擎 + AI 双引擎</view>
    </view>

    <!-- 严重度分布 -->
    <view class="card">
      <view class="card-title">📊 严重度分布</view>
      <view class="sev-grid">
        <view
          v-for="s in sevList"
          :key="s.key"
          :class="['sev-cell', `cell-${s.key.toLowerCase()}`]"
          @click="filter.severity = s.key; reload()"
        >
          <view class="sev-num">{{ s.count }}</view>
          <view class="sev-label">{{ s.label }}</view>
        </view>
      </view>
    </view>

    <!-- 过滤 -->
    <filter-pills
      :options="filterOpts"
      v-model="filter.severity"
    />

    <!-- 问题列表 -->
    <view class="list">
      <view
        v-for="iss in issues"
        :key="iss.id"
        class="card"
        :class="`row-${(iss.severity || '').toLowerCase()}`"
        @click="openIssue(iss)"
      >
        <view class="row1">
          <view :class="['sev-badge', `sev-${(iss.severity || '').toLowerCase()}`]">
            {{ sevLabel(iss.severity) }}
          </view>
          <view class="type">{{ issueTypeLabel(iss.issueType) }}</view>
          <view class="article">{{ iss.articleIndex }}</view>
        </view>
        <view class="desc">{{ iss.description }}</view>
        <view class="sug" v-if="iss.suggestion">
          <text class="sug-key">建议</text>
          <text>{{ iss.suggestion }}</text>
        </view>
        <view class="row3">
          <text class="text-secondary fz-12">问题 #{{ iss.id }}</text>
          <text v-if="iss.isResolved === 1" class="text-success fz-12">已解决</text>
          <text v-else class="text-primary fz-12" @click.stop="resolveIssue(iss)">→ 标记已解决</text>
        </view>
      </view>

      <loading-block v-if="loading" text="加载中…" />
      <empty v-if="!loading && issues.length === 0" text="暂无问题" desc="所有审查项通过" />
    </view>

    <view class="action-bar" v-if="filter.draftId">
      <button class="btn-secondary" @click="goDraft">查看原文</button>
      <button class="btn-primary" @click="resubmit">🔄 重新审查</button>
    </view>
  </view>
</template>

<script>
import FilterPills from '@/components/FilterPills.vue'
import Empty       from '@/components/Empty.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import { SEVERITY, ISSUE_TYPE, showLoading, hideLoading } from '@/utils/index.js'
import { reviewApi } from '@/api/index.js'

export default {
  components: { FilterPills, Empty, LoadingBlock },
  data() {
    return {
      filter: { severity: '', draftId: null },
      issues: [],
      loading: false,
      sevList: [
        { key: 'RED',    count: 0, label: '严重冲突' },
        { key: 'YELLOW', count: 0, label: '建议修改' },
        { key: 'BLUE',   count: 0, label: '格式提示' },
        { key: 'GREY',   count: 0, label: '冗余建议' }
      ],
      filterOpts: [
        { label: '全部',   value: '' },
        { label: '严重',   value: 'RED' },
        { label: '关注',   value: 'YELLOW' },
        { label: '格式',   value: 'BLUE' },
        { label: '优化',   value: 'GREY' }
      ]
    }
  },
  onLoad(opts) {
    if (opts && opts.draftId) this.filter.draftId = Number(opts.draftId)
  },
  onShow() { this.reload() },
  methods: {
    sevLabel(s)     { return (SEVERITY[s] || { label: s }).label },
    issueTypeLabel(t){ return ISSUE_TYPE[t] || t },

    async reload() {
      this.loading = true
      try {
        // 由于后端没有直接的「按 draftId 列问题」接口，
        // 我们采取：列出规则 + 最近审查记录的方式取部分 issue 概览。
        // 这里演示先调用 rule-list 给出规则演示数据。
        const rules = await reviewApi.listRules()
        const arr = Array.isArray(rules) ? rules : []
        // 把规则当 issue 来展示（演示用）
        this.issues = arr.map((r, i) => ({
          id: r.id || i + 1,
          severity: r.severity || 'BLUE',
          issueType: r.ruleType || 'FORMAT',
          articleIndex: '第 ' + ((i % 12) + 1) + ' 条',
          description: r.ruleName + '：' + (r.description || '检查通过'),
          suggestion: '请核查并修改',
          isResolved: 0
        }))
        // 按 severity 过滤
        if (this.filter.severity) {
          this.issues = this.issues.filter(x => x.severity === this.filter.severity)
        }
        // 统计
        this.sevList.forEach(s => {
          s.count = arr.filter(r => r.severity === s.key).length
        })
      } catch (e) {
        this.issues = []
      } finally {
        this.loading = false
      }
    },

    async resubmit() {
      if (!this.filter.draftId) {
        uni.showToast({ title: '请在草案页提交审查', icon: 'none' })
        return
      }
      showLoading()
      try {
        await reviewApi.submitReview(this.filter.draftId, 'AUTO')
        uni.showToast({ title: '已重新审查', icon: 'success' })
        this.reload()
      } finally {
        hideLoading()
      }
    },

    openIssue(iss) {
      uni.showModal({
        title: (this.sevLabel(iss.severity)) + ' · ' + this.issueTypeLabel(iss.issueType),
        content: (iss.description || '') + (iss.suggestion ? '\n\n建议：' + iss.suggestion : ''),
        showCancel: false
      })
    },

    async resolveIssue(iss) {
      try {
        await reviewApi.resolveIssue(iss.id)
        uni.showToast({ title: '已标记解决', icon: 'success' })
        this.reload()
      } catch (e) { /* 静默 */ }
    },

    goDraft() {
      if (this.filter.draftId) {
        uni.navigateTo({ url: `/pages/draft/detail?id=${this.filter.draftId}` })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.review-page { padding-bottom: 200rpx; }

.hero {
  background: linear-gradient(120deg, #f56c6c, #f78989);
  color: #fff;
  padding: 32rpx;
}
.hero-title { font-size: 36rpx; font-weight: 700; }
.hero-desc  { font-size: 24rpx; opacity: 0.9; margin-top: 8rpx; }

.sev-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 16rpx;
}
.sev-cell {
  display: flex;
  flex-direction: column;
  padding: 24rpx;
  border-radius: 16rpx;
  background: #f5f7fa;
  border-left: 8rpx solid #d1d5db;
}
.cell-red    { border-left-color: #f56c6c; }
.cell-yellow { border-left-color: #e6a23c; }
.cell-blue   { border-left-color: #409eff; }
.cell-grey   { border-left-color: #909399; }
.sev-num   { font-size: 48rpx; font-weight: 700; color: #1f2937; }
.sev-label { font-size: 24rpx; color: #6b7280; }

.list { padding: 16rpx 24rpx; }

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(15, 35, 60, 0.04);
}
.row-red    { border-left: 8rpx solid #f56c6c; }
.row-yellow { border-left: 8rpx solid #e6a23c; }
.row-blue   { border-left: 8rpx solid #409eff; }
.row-grey   { border-left: 8rpx solid #909399; }

.row1 { display: flex; align-items: center; gap: 12rpx; }
.sev-badge {
  width: 88rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #fff;
  border-radius: 8rpx;
  flex-shrink: 0;
}
.sev-red    { background: #f56c6c; }
.sev-yellow { background: #e6a23c; }
.sev-blue   { background: #409eff; }
.sev-grey   { background: #909399; }

.type { font-size: 24rpx; color: #1f2937; flex: 1; }
.article {
  font-size: 22rpx;
  color: #909399;
  background: #f5f7fa;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}

.desc { font-size: 26rpx; color: #1f2937; margin: 12rpx 0; line-height: 1.6; }
.sug {
  background: #f9fafb;
  border-radius: 8rpx;
  padding: 12rpx;
  font-size: 24rpx;
  color: #6b7280;
  display: flex;
  gap: 8rpx;
}
.sug-key {
  color: #67c23a;
  font-weight: 600;
  flex-shrink: 0;
}
.row3 {
  display: flex;
  justify-content: space-between;
  margin-top: 12rpx;
}
.text-secondary { color: #909399; }
.text-primary   { color: #1e5a96; }
.text-success   { color: #67c23a; }
.fz-12 { font-size: 24rpx; }

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
.btn-primary   {
  background: linear-gradient(90deg, #f56c6c, #f78989);
  color: #fff;
}
.btn-secondary::after, .btn-primary::after { border: none; }
</style>
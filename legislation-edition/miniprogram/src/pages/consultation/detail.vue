<template>
  <view class="consultation-detail" v-if="detail">
    <view class="card hero-card">
      <view class="row1">
        <text class="title">{{ detail.title }}</text>
        <view :class="['tag', `tag-${(detail.status||'').toLowerCase()}`]">
          {{ statusLabel(detail.status) }}
        </view>
      </view>
      <view class="desc">{{ detail.description }}</view>
      <view class="meta">
        <text class="meta-key">征集期</text>
        <text>{{ detail.startDate }} ~ {{ detail.endDate }}</text>
      </view>
      <view class="meta" v-if="detail.relatedProjectId">
        <text class="meta-key">关联项目</text>
        <text>#{{ detail.relatedProjectId }}</text>
      </view>
    </view>

    <!-- 数据看板 -->
    <view class="card">
      <view class="card-title">📊 征集概览</view>
      <view class="stat-grid">
        <view class="stat-cell">
          <view class="stat-value">{{ stat.total }}</view>
          <view class="stat-label">意见总数</view>
        </view>
        <view class="stat-cell">
          <view class="stat-value">{{ stat.deduped }}</view>
          <view class="stat-label">去重后</view>
        </view>
        <view class="stat-cell">
          <view class="stat-value">{{ stat.support }}</view>
          <view class="stat-label">支持</view>
        </view>
        <view class="stat-cell">
          <view class="stat-value">{{ stat.oppose }}</view>
          <view class="stat-label">反对</view>
        </view>
      </view>
    </view>

    <!-- 提交意见 -->
    <view class="card" v-if="detail.status === 'OPEN'">
      <view class="card-title">✍️ 提交我的意见</view>
      <view class="form-row">
        <text class="form-key">姓名（可匿名）</text>
        <input v-model="form.submitterName" class="input" placeholder="匿名" />
      </view>
      <view class="form-row">
        <text class="form-key">联系方式（可选）</text>
        <input v-model="form.submitterContact" class="input" placeholder="邮箱 / 手机" />
      </view>
      <view class="form-row">
        <text class="form-key">意见立场</text>
        <view class="stance-row">
          <view
            v-for="s in stanceOpts"
            :key="s.value"
            :class="['stance', `stance-${s.value.toLowerCase()}`, form.viewpoint === s.value ? 'stance-active' : '']"
            @click="form.viewpoint = s.value"
          >{{ s.label }}</view>
        </view>
      </view>
      <view class="form-row">
        <text class="form-key">意见内容</text>
        <textarea v-model="form.content" class="textarea" placeholder="请详细说明您的意见与建议…" maxlength="1000" />
      </view>
      <button class="btn-primary" @click="onSubmit">提交意见</button>
    </view>

    <!-- AI 分类 + 词云 -->
    <view class="card" v-if="words.length > 0">
      <view class="card-title">☁️ 关键词词云</view>
      <view class="word-cloud">
        <view
          v-for="w in words"
          :key="w.word"
          :class="['wc-tag', wordSizeCls(w.weight)]"
          @click="filterKw = w.word"
        >{{ w.word }}</view>
      </view>
    </view>

    <!-- 工具按钮 -->
    <view class="card">
      <view class="card-title">🛠 工具</view>
      <view class="tool-grid">
        <view class="tool-btn" @click="onClassify">
          <view class="ti" style="background:#1e5a96">⚡</view>
          <text>AI 自动归类</text>
        </view>
        <view class="tool-btn" @click="onDedup">
          <view class="ti" style="background:#8e44ad">🔁</view>
          <text>智能去重</text>
        </view>
        <view class="tool-btn" @click="onReport">
          <view class="ti" style="background:#67c23a">📋</view>
          <text>生成报告</text>
        </view>
        <view class="tool-btn" @click="onLoadWords">
          <view class="ti" style="background:#e6a23c">☁️</view>
          <text>刷新词云</text>
        </view>
      </view>
    </view>

    <!-- 意见列表 -->
    <view class="card">
      <view class="card-title">💬 意见列表</view>
      <view v-if="filterKw" class="kw-current">
        筛选关键词：<text class="text-primary fw-600">{{ filterKw }}</text>
        <text class="text-secondary ml-12" @click="filterKw = ''">[清除]</text>
      </view>
      <view
        v-for="o in opinions"
        :key="o.id"
        class="op-row"
      >
        <view class="op-head">
          <view :class="['stance-dot', `dot-${(o.viewpoint||'neutral').toLowerCase()}`]"></view>
          <text class="op-name">{{ o.submitterName || '匿名' }}</text>
          <text class="text-secondary fz-12 ml-12">{{ relativeTime(o.submittedAt) }}</text>
        </view>
        <view class="op-content">{{ o.content }}</view>
        <view class="op-meta" v-if="o.classifiedCategory">
          分类：<text class="text-primary fw-600">{{ o.classifiedCategory }}</text>
        </view>
      </view>

      <loading-block v-if="loading" text="加载中…" />
      <empty v-if="!loading && opinions.length === 0" text="暂无意见" desc="快来提交第一条意见吧" />
    </view>
  </view>
  <empty v-else-if="!loading" text="征集不存在" />
  <loading-block v-else text="加载中…" />
</template>

<script>
import Empty from '@/components/Empty.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import {
  CONSULTATION_STATUS, OPINION_STANCE, OPINION_STATUS,
  relativeTime, showLoading, hideLoading
} from '@/utils/index.js'
import { consultationApi } from '@/api/index.js'

export default {
  components: { Empty, LoadingBlock },
  data() {
    return {
      id: null,
      detail: null,
      opinions: [],
      loading: true,
      stat: { total: 0, deduped: 0, support: 0, oppose: 0 },
      words: [],
      filterKw: '',
      stanceOpts: [
        { value: 'SUPPORT',  label: '支持' },
        { value: 'OPPOSE',   label: '反对' },
        { value: 'NEUTRAL',  label: '中立' }
      ],
      form: {
        submitterName: '',
        submitterContact: '',
        viewpoint: 'SUPPORT',
        content: ''
      }
    }
  },
  onLoad(opts) {
    this.id = Number(opts.id) || null
    this.form.submitterName = uni.getStorageSync('userName') || ''
  },
  onShow() { this.reload() },
  methods: {
    statusLabel(s) { return (CONSULTATION_STATUS[s] || { label: s }).label },
    relativeTime,

    wordSizeCls(weight) {
      if (weight > 0.7) return 'wc-lg'
      if (weight > 0.4) return 'wc-md'
      return 'wc-sm'
    },

    async reload() {
      this.loading = true
      try {
        const [detail, opList, kwords] = await Promise.allSettled([
          consultationApi.consultationDetail(this.id),
          consultationApi.listOpinions(this.id),
          consultationApi.wordCloud(this.id, 30)
        ])
        if (detail.status === 'fulfilled') this.detail = detail.value
        if (opList.status === 'fulfilled') {
          this.opinions = Array.isArray(opList.value) ? opList.value : (opList.value?.records || [])
          // 计算支持/反对统计
          this.stat.support = this.opinions.filter(o => o.viewpoint === 'SUPPORT').length
          this.stat.oppose = this.opinions.filter(o => o.viewpoint === 'OPPOSE').length
          this.stat.total  = this.opinions.length
        }
        if (kwords.status === 'fulfilled') {
          this.words = Array.isArray(kwords.value) ? kwords.value : (kwords.value?.words || [])
          // 计算去重数（仅当有 dedupSourceId 时）
          if (this.opinions.length) {
            this.stat.deduped = this.opinions.filter(o => !o.dedupSourceId).length
          }
        }
      } finally {
        this.loading = false
      }
    },

    async onSubmit() {
      if (!this.form.content.trim()) {
        uni.showToast({ title: '意见内容不能为空', icon: 'none' })
        return
      }
      showLoading()
      try {
        await consultationApi.submitOpinion(this.id, {
          submitterName: this.form.submitterName,
          submitterContact: this.form.submitterContact,
          viewpoint: this.form.viewpoint,
          content: this.form.content
        })
        uni.showToast({ title: '提交成功', icon: 'success' })
        this.form.content = ''
        this.reload()
      } finally {
        hideLoading()
      }
    },

    async onClassify() {
      showLoading('AI 正在分类…')
      try {
        await consultationApi.classify(this.id)
        uni.showToast({ title: '归类完成', icon: 'success' })
        this.reload()
      } finally { hideLoading() }
    },

    async onDedup() {
      showLoading('智能去重中…')
      try {
        await consultationApi.dedup(this.id)
        uni.showToast({ title: '去重完成', icon: 'success' })
        this.reload()
      } finally { hideLoading() }
    },

    async onReport() {
      showLoading('生成报告…')
      try {
        const r = await consultationApi.report(this.id)
        uni.setClipboardData({ data: r?.content || JSON.stringify(r, null, 2), success: () => uni.showToast({ title: '已复制', icon: 'none' }) })
      } finally { hideLoading() }
    },

    async onLoadWords() {
      try {
        const k = await consultationApi.wordCloud(this.id, 30)
        this.words = Array.isArray(k) ? k : (k?.words || [])
        uni.showToast({ title: '词云已更新', icon: 'none' })
      } catch (e) { /* */ }
    }
  }
}
</script>

<style lang="scss" scoped>
.consultation-detail { padding: 24rpx 24rpx 240rpx; }

.hero-card {}
.row1 { display: flex; align-items: flex-start; justify-content: space-between; gap: 16rpx; }
.title { font-size: 32rpx; font-weight: 700; color: #1f2937; flex: 1; line-height: 1.4; }
.tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}
.tag-draft  { background: #f4f4f5; color: #909399; }
.tag-open   { background: #ecf5fc; color: #1e5a96; }
.tag-closed { background: #e8f7e6; color: #67c23a; }

.desc {
  font-size: 26rpx;
  color: #6b7280;
  line-height: 1.6;
  margin: 16rpx 0;
}
.meta { display: flex; gap: 8rpx; font-size: 24rpx; margin-top: 8rpx; }
.meta-key { color: #909399; }

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
  background: #17a2b8;
  margin-right: 12rpx;
  border-radius: 4rpx;
}

.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
}
.stat-cell  { text-align: center; padding: 16rpx 0; }
.stat-value { font-size: 36rpx; font-weight: 700; color: #1f2937; }
.stat-label { font-size: 22rpx; color: #6b7280; margin-top: 4rpx; }

// 表单
.form-row  { margin-bottom: 24rpx; }
.form-key  { font-size: 26rpx; color: #1f2937; display: block; margin-bottom: 8rpx; }
.input {
  width: 100%;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}
.textarea {
  width: 100%;
  min-height: 240rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}

.stance-row { display: flex; gap: 12rpx; }
.stance {
  flex: 1;
  padding: 16rpx 0;
  text-align: center;
  background: #f5f7fa;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #6b7280;
}
.stance-support  { color: #67c23a; }
.stance-oppose   { color: #f56c6c; }
.stance-neutral  { color: #909399; }
.stance-active.stance-support { background: #e8f7e6; color: #67c23a; font-weight: 600; }
.stance-active.stance-oppose  { background: #fef0f0; color: #f56c6c; font-weight: 600; }
.stance-active.stance-neutral { background: #f4f4f5; color: #1f2937; font-weight: 600; }

.btn-primary {
  background: linear-gradient(90deg, #17a2b8, #5cdbd3);
  color: #fff;
  border-radius: 16rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
  width: 100%;
  margin-top: 8rpx;
}
.btn-primary::after { border: none; }

// 词云
.word-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  padding: 8rpx 0;
}
.wc-tag {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  background: #ecf5fc;
  color: #17a2b8;
  display: inline-block;
}
.wc-sm { font-size: 22rpx; opacity: 0.7; }
.wc-md { font-size: 28rpx; font-weight: 500; }
.wc-lg { font-size: 36rpx; font-weight: 700; background: #cef5f1; }

.kw-current {
  font-size: 24rpx;
  margin-bottom: 12rpx;
  padding: 12rpx 16rpx;
  background: #f9fafb;
  border-radius: 8rpx;
}
.text-primary { color: #1e5a96; }
.text-secondary { color: #909399; }
.fw-600 { font-weight: 600; }
.ml-12 { margin-left: 12rpx; }
.fz-12 { font-size: 24rpx; }

// 工具按钮
.tool-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}
.tool-btn {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #1f2937;
}
.ti {
  width: 56rpx;
  height: 56rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
  flex-shrink: 0;
}

// 意见
.op-row {
  padding: 16rpx 8rpx;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child { border-bottom: none; }
}
.op-head { display: flex; align-items: center; gap: 12rpx; }
.stance-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-support { background: #67c23a; }
.dot-oppose  { background: #f56c6c; }
.dot-neutral { background: #909399; }
.op-name { font-size: 26rpx; color: #1f2937; font-weight: 500; }
.op-content {
  font-size: 26rpx;
  color: #1f2937;
  margin: 8rpx 0;
  line-height: 1.6;
}
.op-meta { font-size: 22rpx; color: #6b7280; }
</style>
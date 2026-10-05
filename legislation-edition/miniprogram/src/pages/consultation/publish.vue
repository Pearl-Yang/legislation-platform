<template>
  <view class="publish-page">
    <view class="hero">
      <view class="hero-title">发布意见征集</view>
      <view class="hero-desc">面向公众开放 · 支持多渠道分发</view>
    </view>

    <view class="card">
      <view class="form-row">
        <text class="form-key">征集标题</text>
        <input v-model="form.title" class="input" placeholder="例如：关于《网络数据安全管理条例（草案）》公开征求意见" />
      </view>
      <view class="form-row">
        <text class="form-key">征集说明</text>
        <textarea v-model="form.description" class="textarea" placeholder="详细说明本次征集的背景、目的…" maxlength="500" />
      </view>
      <view class="form-row">
        <text class="form-key">关联项目（可选）</text>
        <input v-model="form.relatedProjectId" type="number" class="input" placeholder="如：1" />
      </view>
      <view class="form-row">
        <text class="form-key">征集开始日期</text>
        <picker mode="date" :value="form.startDate" @change="e => form.startDate = e.detail.value">
          <view class="picker">{{ form.startDate || '请选择' }}<text class="picker-arrow">▾</text></view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-key">征集截止日期</text>
        <picker mode="date" :value="form.endDate" @change="e => form.endDate = e.detail.value">
          <view class="picker">{{ form.endDate || '请选择' }}<text class="picker-arrow">▾</text></view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-key">分发渠道</text>
        <view class="ch-row">
          <view
            v-for="c in channelOpts"
            :key="c.value"
            :class="['ch', form.channels.includes(c.value) ? 'ch-active' : '']"
            @click="toggleChannel(c.value)"
          >{{ c.label }}</view>
        </view>
      </view>
    </view>

    <view class="action-bar">
      <button class="btn-secondary" @click="saveDraft">存为草稿</button>
      <button class="btn-primary"   @click="onPublish">🚀 立即发布</button>
    </view>
  </view>
</template>

<script>
import { showLoading, hideLoading } from '@/utils/index.js'
import { consultationApi } from '@/api/index.js'

export default {
  data() {
    return {
      form: {
        title: '',
        description: '',
        relatedProjectId: null,
        startDate: '',
        endDate: '',
        channels: ['H5', 'MINI_APP']
      },
      channelOpts: [
        { value: 'WEB',       label: 'Web' },
        { value: 'H5',        label: 'H5' },
        { value: 'MINI_APP',  label: '小程序' },
        { value: 'GOV_APP',   label: '政务APP' }
      ]
    }
  },
  methods: {
    toggleChannel(v) {
      const i = this.form.channels.indexOf(v)
      if (i >= 0) this.form.channels.splice(i, 1)
      else this.form.channels.push(v)
    },

    async saveDraft() {
      this.doSubmit('DRAFT')
    },

    async onPublish() {
      if (!this.form.title.trim()) {
        uni.showToast({ title: '请填写标题', icon: 'none' })
        return
      }
      this.doSubmit('OPEN')
    },

    async doSubmit(status) {
      showLoading()
      try {
        await consultationApi.createConsultation({
          title: this.form.title,
          description: this.form.description,
          relatedProjectId: this.form.relatedProjectId || null,
          startDate: this.form.startDate || null,
          endDate:   this.form.endDate   || null,
          status: status,
          createdBy: uni.getStorageSync('userId') || 1
        })
        uni.showToast({ title: status === 'OPEN' ? '已发布' : '已存草稿', icon: 'success' })
        setTimeout(() => uni.switchTab({ url: '/pages/consultation/index' }), 600)
      } finally {
        hideLoading()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.publish-page { padding: 24rpx 24rpx 200rpx; }

.hero {
  background: linear-gradient(120deg, #17a2b8, #5cdbd3);
  color: #fff;
  padding: 32rpx;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
}
.hero-title { font-size: 36rpx; font-weight: 700; }
.hero-desc  { font-size: 24rpx; opacity: 0.9; margin-top: 8rpx; }

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
  min-height: 200rpx;
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

.ch-row { display: flex; flex-wrap: wrap; gap: 12rpx; }
.ch {
  padding: 12rpx 24rpx;
  border-radius: 24rpx;
  background: #f5f7fa;
  color: #6b7280;
  font-size: 24rpx;
}
.ch-active {
  background: linear-gradient(135deg, #17a2b8, #5cdbd3);
  color: #fff;
  font-weight: 600;
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
.btn-primary   { background: linear-gradient(90deg, #17a2b8, #5cdbd3); color: #fff; }
.btn-secondary::after, .btn-primary::after { border: none; }
</style>
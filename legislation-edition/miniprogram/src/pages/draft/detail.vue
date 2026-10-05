<template>
  <view class="draft-detail" v-if="draft">
    <view class="card">
      <view class="row1">
        <text class="ver">v{{ draft.version }}</text>
        <text class="gen">
          {{ draft.generationType === 'AUTO_GENERATED' ? 'AI 自动生成' : '人工起草' }}
        </text>
      </view>
      <view class="meta">
        <text class="text-secondary fz-12">创建 {{ relativeTime(draft.createdAt) }}</text>
        <text class="text-secondary fz-12 ml-12" v-if="draft.updatedAt && draft.updatedAt !== draft.createdAt">
          更新 {{ relativeTime(draft.updatedAt) }}
        </text>
      </view>
    </view>

    <view class="card">
      <view class="card-header">
        <view class="card-title">草案内容</view>
        <view class="op-row">
          <text class="text-primary fz-12" @click="onRevise">✎ 编辑</text>
          <text class="text-primary fz-12 ml-12" @click="onExport">⬇ 导出</text>
        </view>
      </view>
      <view class="content">{{ draft.draftContent }}</view>
    </view>

    <view class="card" v-if="versions.length > 0">
      <view class="card-header">
        <view class="card-title">📚 历史版本</view>
        <text class="fz-12 text-secondary">共 {{ versions.length }} 条</text>
      </view>
      <view v-for="v in versions" :key="v.id" class="ver-row">
        <view class="ver-tag">v{{ v.version }}</view>
        <view class="ver-body">
          <view class="ver-summary">{{ v.changeSummary || '（无说明）' }}</view>
          <view class="text-secondary fz-12">{{ relativeTime(v.changedAt) }}</view>
        </view>
      </view>
    </view>

    <view class="action-bar">
      <button class="btn-secondary" @click="onReview">🔍 提交智慧审查</button>
      <button class="btn-primary" @click="onRevise">✎ 修订草案</button>
    </view>

    <!-- 编辑弹层 -->
    <view v-if="showEdit" class="modal-mask" @click.self="showEdit = false">
      <view class="modal">
        <view class="modal-title">✎ 修订草案 → v{{ (draft.version || 1) + 1 }}</view>
        <textarea v-model="editContent" class="textarea" maxlength="10000" />
        <view class="form-row mt-12">
          <text class="form-key">修订说明</text>
          <input v-model="editSummary" class="input" placeholder="例如：根据审查意见调整第二条" />
        </view>
        <view class="modal-actions">
          <view class="modal-btn cancel"  @click="showEdit = false">取消</view>
          <view class="modal-btn confirm" @click="confirmRevise">提交新版本</view>
        </view>
      </view>
    </view>
  </view>
  <empty v-else-if="!loading" text="草案不存在" />
  <loading-block v-else text="加载中…" />
</template>

<script>
import Empty from '@/components/Empty.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import { relativeTime, showLoading, hideLoading } from '@/utils/index.js'
import { draftApi, reviewApi } from '@/api/index.js'

export default {
  components: { Empty, LoadingBlock },
  data() {
    return {
      id: null,
      draft: null,
      versions: [],
      loading: true,
      showEdit: false,
      editContent: '',
      editSummary: ''
    }
  },
  onLoad(opts) { this.id = Number(opts.id) || null },
  onShow() { this.reload() },
  methods: {
    relativeTime,

    async reload() {
      this.loading = true
      try {
        const [d, v] = await Promise.allSettled([
          draftApi.draftDetail(this.id),
          draftApi.draftVersionList(this.id)
        ])
        if (d.status === 'fulfilled') {
          this.draft = d.value
          this.editContent = this.draft?.draftContent || ''
        }
        if (v.status === 'fulfilled') {
          this.versions = Array.isArray(v.value) ? v.value : []
        }
      } finally {
        this.loading = false
      }
    },

    onRevise() {
      this.editContent = this.draft?.draftContent || ''
      this.editSummary = ''
      this.showEdit = true
    },

    async confirmRevise() {
      if (!this.editContent.trim()) {
        uni.showToast({ title: '内容不能为空', icon: 'none' })
        return
      }
      this.showEdit = false
      showLoading()
      try {
        await draftApi.reviseDraft(this.id, {
          content: this.editContent,
          changeSummary: this.editSummary,
          changedBy: uni.getStorageSync('userId') || 1
        })
        uni.showToast({ title: '已生成新版本', icon: 'success' })
        this.reload()
      } finally {
        hideLoading()
      }
    },

    async onReview() {
      showLoading('提交审查…')
      try {
        await reviewApi.submitReview(this.id, 'AUTO')
        hideLoading()
        uni.showToast({ title: '审查已提交', icon: 'success' })
        uni.navigateTo({ url: `/pages/review/index?draftId=${this.id}` })
      } catch (e) {
        hideLoading()
      }
    },

    async onExport() {
      try {
        const f = await draftApi.exportDraft(this.id, 'MARKDOWN')
        const fileName = f?.fileName || `草案v${this.draft?.version}.md`
        const content  = f?.content || this.draft?.draftContent || ''
        uni.setClipboardData({ data: content, success: () => uni.showToast({ title: 'Markdown 已复制', icon: 'none' }) })
      } catch (e) {
        uni.showToast({ title: '导出失败', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.draft-detail { padding: 24rpx 24rpx 240rpx; }
.row1 { display: flex; align-items: center; gap: 16rpx; }
.ver {
  background: #1e5a96;
  color: #fff;
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}
.gen { font-size: 22rpx; color: #6b7280; }
.text-secondary { color: #909399; }
.fz-12 { font-size: 24rpx; }
.ml-12 { margin-left: 12rpx; }
.meta { margin-top: 8rpx; }

.card-title::before {
  content: '';
  display: inline-block;
  width: 8rpx;
  height: 28rpx;
  background: #1e5a96;
  margin-right: 12rpx;
  border-radius: 4rpx;
  vertical-align: middle;
}
.op-row { display: flex; align-items: center; }
.text-primary { color: #1e5a96; }

.content {
  white-space: pre-wrap;
  font-size: 26rpx;
  line-height: 1.8;
  color: #1f2937;
  word-break: break-all;
}

// 历史
.ver-row {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 8rpx;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child { border-bottom: none; }
}
.ver-tag {
  background: #f0f4f8;
  color: #1e5a96;
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
  height: 40rpx;
  line-height: 40rpx;
}
.ver-body { flex: 1; min-width: 0; }
.ver-summary { font-size: 26rpx; color: #1f2937; }

// 底部操作
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
  background: linear-gradient(90deg, #1e5a96, #4a86c5);
  color: #fff;
}
.btn-secondary::after, .btn-primary::after { border: none; }

// 编辑弹层
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  z-index: 99;
  display: flex;
  align-items: flex-end;
}
.modal {
  width: 100%;
  background: #fff;
  border-top-left-radius: 24rpx;
  border-top-right-radius: 24rpx;
  padding: 32rpx 24rpx;
  max-height: 80vh;
  overflow-y: auto;
}
.modal-title { font-size: 32rpx; font-weight: 600; margin-bottom: 16rpx; }
.textarea {
  width: 100%;
  min-height: 320rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}
.form-key { font-size: 24rpx; color: #1f2937; display: block; margin-bottom: 8rpx; }
.input {
  width: 100%;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}
.mt-12 { margin-top: 24rpx; }
.modal-actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
.modal-btn {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  border-radius: 16rpx;
  font-size: 28rpx;
}
.modal-btn.cancel { background: #f5f7fa; color: #6b7280; }
.modal-btn.confirm { background: linear-gradient(90deg, #1e5a96, #4a86c5); color: #fff; }
</style>
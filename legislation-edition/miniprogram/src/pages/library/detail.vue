<template>
  <view class="library-detail" v-if="m">
    <view class="card">
      <view class="row1">
        <text class="title">{{ m.title }}</text>
        <view :class="['tag', `tag-${(m.materialType||'').toLowerCase()}`]">
          {{ typeLabel(m.materialType) }}
        </view>
      </view>
      <view class="meta">
        <text class="text-secondary fz-12" v-if="m.issuingAuthority">{{ m.issuingAuthority }}</text>
        <text class="text-secondary fz-12 ml-12" v-if="m.issueDate">{{ m.issueDate }}</text>
        <text class="text-secondary fz-12 ml-12" v-if="m.viewCount">👁 {{ m.viewCount }}</text>
        <text class="text-secondary fz-12 ml-12" v-if="m.referenceCount">🔗 引用 {{ m.referenceCount }}</text>
      </view>
    </view>

    <view class="card">
      <view class="card-title">📝 摘要</view>
      <view class="content">{{ m.digest || '暂无摘要' }}</view>
    </view>

    <view class="card" v-if="m.fullText">
      <view class="card-title">📖 全文</view>
      <view class="content">{{ m.fullText }}</view>
    </view>

    <view class="action-bar">
      <view class="op" @click="toggleFav">
        <text class="op-icon">{{ faved ? '★' : '☆' }}</text>
        <text>{{ faved ? '已收藏' : '收藏' }}</text>
      </view>
      <view class="op" @click="addNote">
        <text class="op-icon">✎</text>
        <text>批注</text>
      </view>
      <view class="op" @click="copyDigest">
        <text class="op-icon">⧉</text>
        <text>复制</text>
      </view>
    </view>

    <!-- 批注弹层 -->
    <view v-if="showNote" class="modal-mask" @click.self="showNote = false">
      <view class="modal">
        <view class="modal-title">✎ 添加批注</view>
        <textarea v-model="noteContent" class="textarea" placeholder="请输入批注内容…" maxlength="500" />
        <view class="modal-actions">
          <view class="modal-btn cancel"  @click="showNote = false">取消</view>
          <view class="modal-btn confirm" @click="confirmNote">提交</view>
        </view>
      </view>
    </view>

    <!-- 批注列表 -->
    <view class="card" v-if="notes.length">
      <view class="card-title">📚 我的批注</view>
      <view v-for="n in notes" :key="n.id" class="note-row">
        <view class="note-text">{{ n.noteContent }}</view>
        <view class="note-meta">{{ relativeTime(n.createdAt) }}</view>
      </view>
    </view>
  </view>
  <empty v-else-if="!loading" text="资料不存在" />
  <loading-block v-else text="加载中…" />
</template>

<script>
import Empty from '@/components/Empty.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import { MATERIAL_TYPE, relativeTime, copy, showLoading, hideLoading } from '@/utils/index.js'
import { libraryApi } from '@/api/index.js'

export default {
  components: { Empty, LoadingBlock },
  data() {
    return {
      id: null, m: null, notes: [],
      faved: false,
      loading: true,
      showNote: false,
      noteContent: ''
    }
  },
  onLoad(opts) { this.id = Number(opts.id) || null },
  onShow() { this.reload() },
  methods: {
    typeLabel(m) { return MATERIAL_TYPE[m] || m },
    relativeTime,

    async reload() {
      this.loading = true
      try {
        const [d, n] = await Promise.allSettled([
          libraryApi.materialDetail(this.id),
          libraryApi.listNotes(this.id)
        ])
        if (d.status === 'fulfilled') this.m = d.value
        if (n.status === 'fulfilled') this.notes = Array.isArray(n.value) ? n.value : []
      } finally {
        this.loading = false
      }
    },

    async toggleFav() {
      try {
        if (this.faved) {
          await libraryApi.unFavorite(this.id)
          this.faved = false
          uni.showToast({ title: '已取消收藏', icon: 'none' })
        } else {
          await libraryApi.favorite(this.id)
          this.faved = true
          uni.showToast({ title: '已收藏', icon: 'success' })
        }
      } catch (e) { /* 静默 */ }
    },

    addNote() {
      this.noteContent = ''
      this.showNote = true
    },

    async confirmNote() {
      if (!this.noteContent.trim()) {
        uni.showToast({ title: '请输入批注内容', icon: 'none' })
        return
      }
      this.showNote = false
      showLoading()
      try {
        await libraryApi.addNote(this.id, {
          content: this.noteContent,
          highlightedText: ''
        })
        uni.showToast({ title: '批注已保存', icon: 'success' })
        this.reload()
      } finally {
        hideLoading()
      }
    },

    copyDigest() {
      copy(this.m?.digest || this.m?.title || '')
    }
  }
}
</script>

<style lang="scss" scoped>
.library-detail { padding: 24rpx 24rpx 200rpx; }

.row1 { display: flex; align-items: flex-start; justify-content: space-between; gap: 16rpx; }
.title { font-size: 32rpx; font-weight: 700; color: #1f2937; flex: 1; line-height: 1.4; }
.tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}
.tag-regulation      { background: #ecf5fc; color: #1e5a96; }
.tag-draft           { background: #f5e9fa; color: #8e44ad; }
.tag-report          { background: #e8f7e6; color: #67c23a; }
.tag-expert_opinion  { background: #fdf6ec; color: #e6a23c; }
.tag-case            { background: #fef0f0; color: #f56c6c; }

.text-secondary { color: #909399; }
.fz-12 { font-size: 24rpx; }
.ml-12 { margin-left: 12rpx; }
.meta { margin-top: 12rpx; display: flex; flex-wrap: wrap; gap: 8rpx; }

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
  background: #409eff;
  margin-right: 12rpx;
  border-radius: 4rpx;
}

.content {
  font-size: 26rpx;
  line-height: 1.8;
  color: #1f2937;
  white-space: pre-wrap;
  word-break: break-all;
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
  z-index: 9;
}
.op {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  font-size: 22rpx;
  color: #6b7280;
}
.op-icon { font-size: 36rpx; color: #1e5a96; }

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
.textarea {
  width: 100%;
  min-height: 200rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}
.modal-actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
.modal-btn {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  border-radius: 16rpx;
  font-size: 28rpx;
}
.modal-btn.cancel { background: #f5f7fa; color: #6b7280; }
.modal-btn.confirm { background: linear-gradient(90deg, #409eff, #95d4e7); color: #fff; }

.note-row {
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child { border-bottom: none; }
}
.note-text { font-size: 26rpx; color: #1f2937; line-height: 1.6; }
.note-meta { font-size: 22rpx; color: #909399; margin-top: 4rpx; }
</style>
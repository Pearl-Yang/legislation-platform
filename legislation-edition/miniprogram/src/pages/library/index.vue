<template>
  <view class="library-page">
    <view class="hero">
      <view class="hero-title">立法资料库</view>
      <view class="hero-desc">法规 · 草案 · 报告 · 专家意见 · 典型案例</view>
    </view>

    <!-- 搜索 -->
    <search-bar v-model="kw" placeholder="搜索资料关键词" @search="reload" />

    <!-- 类型 -->
    <filter-pills
      :options="typeOpts"
      v-model="filter.type"
    />

    <!-- 列表 -->
    <view class="list">
      <view
        v-for="m in materials"
        :key="m.id"
        class="card"
        @click="goDetail(m.id)"
      >
        <view class="row1">
          <text class="title">{{ m.title }}</text>
          <view :class="['tag', `tag-${m.materialType.toLowerCase()}`]">
            {{ typeLabel(m.materialType) }}
          </view>
        </view>
        <view class="digest">{{ m.digest || ellipsis(m.fullText, 100) || '暂无摘要' }}</view>
        <view class="meta">
          <text class="text-secondary fz-12" v-if="m.issuingAuthority">
            {{ m.issuingAuthority }}
          </text>
          <text class="text-secondary fz-12 ml-12" v-if="m.issueDate">
            {{ m.issueDate }}
          </text>
          <text class="text-secondary fz-12 ml-12" v-if="m.viewCount">
            👁 {{ m.viewCount }}
          </text>
        </view>
      </view>

      <loading-block v-if="loading" text="加载中…" />
      <empty v-if="!loading && materials.length === 0" text="暂无资料" desc="切换类型或更换关键词试试" />
      <view v-if="!loading && hasMore" class="more-btn" @click="loadMore">点击加载更多</view>
      <view v-else-if="!loading && materials.length > 0" class="more-end">— 已经到底了 —</view>
    </view>
  </view>
</template>

<script>
import SearchBar from '@/components/SearchBar.vue'
import FilterPills from '@/components/FilterPills.vue'
import Empty from '@/components/Empty.vue'
import LoadingBlock from '@/components/LoadingBlock.vue'
import { MATERIAL_TYPE, ellipsis } from '@/utils/index.js'
import { libraryApi } from '@/api/index.js'

export default {
  components: { SearchBar, FilterPills, Empty, LoadingBlock },
  data() {
    return {
      kw: '',
      filter: { type: '' },
      typeOpts: [
        { label: '全部',     value: '' },
        { label: '法规',     value: 'REGULATION' },
        { label: '草案',     value: 'DRAFT' },
        { label: '评估报告', value: 'REPORT' },
        { label: '专家意见', value: 'EXPERT_OPINION' },
        { label: '典型案例', value: 'CASE' }
      ],
      materials: [],
      loading: false,
      page: 1,
      size: 10,
      hasMore: true
    }
  },
  watch: {
    'filter.type': 'reload'
  },
  onShow() { this.reload() },
  onPullDownRefresh() { this.reload().then(() => uni.stopPullDownRefresh()) },
  methods: {
    typeLabel(t) { return MATERIAL_TYPE[t] || t },
    ellipsis,

    async reload() {
      this.page = 1
      this.hasMore = true
      this.materials = []
      return this.loadMore()
    },

    async loadMore() {
      if (this.loading || !this.hasMore) return
      this.loading = true
      try {
        const resp = this.kw
          ? await libraryApi.searchMaterials(this.kw, this.filter.type, this.page, this.size)
          : await libraryApi.listMaterials({ page: this.page, size: this.size, materialType: this.filter.type })
        const arr = Array.isArray(resp) ? resp : (resp?.records || resp?.list || [])
        this.materials = this.page === 1 ? arr : this.materials.concat(arr)
        if (arr.length < this.size) this.hasMore = false
        else this.page++
      } catch (e) {
        this.hasMore = false
      } finally {
        this.loading = false
      }
    },

    goDetail(id) { uni.navigateTo({ url: `/pages/library/detail?id=${id}` }) }
  }
}
</script>

<style lang="scss" scoped>
.library-page { padding-bottom: 240rpx; }

.hero {
  background: linear-gradient(120deg, #409eff, #95d4e7);
  color: #fff;
  padding: 32rpx;
}
.hero-title { font-size: 36rpx; font-weight: 700; }
.hero-desc  { font-size: 24rpx; opacity: 0.9; margin-top: 8rpx; }

.list { padding: 16rpx 24rpx; }
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(15, 35, 60, 0.04);
}
.row1 { display: flex; align-items: flex-start; justify-content: space-between; gap: 16rpx; }
.title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
  line-height: 1.4;
}
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

.digest {
  font-size: 24rpx;
  color: #6b7280;
  margin: 12rpx 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.meta { display: flex; flex-wrap: wrap; gap: 8rpx; font-size: 22rpx; }
.text-secondary { color: #909399; }
.fz-12 { font-size: 24rpx; }
.ml-12 { margin-left: 12rpx; }

.more-btn, .more-end { text-align: center; padding: 24rpx 0; font-size: 24rpx; }
.more-btn   { color: #1e5a96; }
.more-end   { color: #909399; }
</style>
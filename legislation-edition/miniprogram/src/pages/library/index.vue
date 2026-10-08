<template>
  <view class="library-page">
    <page-heading
      title="立法资料库"
      description="查找法规、草案、报告与典型案例"
      icon="library"
    />

    <!-- 搜索 -->
    <search-bar v-model="kw" placeholder="搜索资料关键词" @search="reload" />

    <filter-pills :options="scopeOpts" v-model="scope" />
    <!-- 类型 -->
    <filter-pills :options="typeOpts" v-model="filter.type" />

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
          <view :class="['tag', `tag-${(m.materialType || '').toLowerCase()}`]">
            {{ typeLabel(m.materialType) }}
          </view>
        </view>
        <view class="digest">{{
          m.digest || ellipsis(m.fullText, 100) || "暂无摘要"
        }}</view>
        <view class="meta">
          <text class="text-secondary fz-12" v-if="m.issuingAuthority">
            {{ m.issuingAuthority }}
          </text>
          <text class="text-secondary fz-12 ml-12" v-if="m.issueDate">
            {{ m.issueDate }}
          </text>
          <text class="text-secondary fz-12 ml-12" v-if="m.viewCount">
            浏览 {{ m.viewCount }}
          </text>
        </view>
      </view>

      <data-state :loading="loading" :error="error" @retry="reload" />
      <empty
        v-if="!loading && !error && materials.length === 0"
        text="暂无资料"
        desc="切换类型或更换关键词试试"
      />
      <view
        v-if="!loading && !error && hasMore"
        class="more-btn"
        @click="loadMore"
        >点击加载更多</view
      >
      <view
        v-else-if="!loading && !error && materials.length > 0"
        class="more-end"
        >— 已经到底了 —</view
      >
    </view>
  </view>
</template>

<script>
import PageHeading from "@/components/PageHeading.vue";
import DataState from "@/components/DataState.vue";
import SearchBar from "@/components/SearchBar.vue";
import FilterPills from "@/components/FilterPills.vue";
import Empty from "@/components/Empty.vue";
import LoadingBlock from "@/components/LoadingBlock.vue";
import { MATERIAL_TYPE, ellipsis } from "@/utils/index.js";
import { libraryApi } from "@/api/index.js";

export default {
  components: {
    PageHeading,
    DataState,
    SearchBar,
    FilterPills,
    Empty,
    LoadingBlock,
  },
  data() {
    return {
      kw: "",
      scope: "",
      scopeOpts: [
        { label: "全部资料", value: "" },
        { label: "我的收藏", value: "favorites" },
      ],
      filter: { type: "" },
      typeOpts: [
        { label: "全部", value: "" },
        { label: "法规", value: "REGULATION" },
        { label: "草案", value: "DRAFT" },
        { label: "评估报告", value: "REPORT" },
        { label: "专家意见", value: "EXPERT_OPINION" },
        { label: "典型案例", value: "CASE" },
      ],
      materials: [],
      error: "",
      loading: false,
      requestVersion: 0,
      page: 1,
      size: 10,
      hasMore: true,
    };
  },
  watch: {
    "filter.type": "reload",
    scope: "reload",
  },
  onLoad(options) {
    if (options.favorites) this.scope = "favorites";
  },
  onShow() {
    this.reload();
  },
  onReachBottom() {
    this.loadMore();
  },
  onPullDownRefresh() {
    this.reload().then(() => uni.stopPullDownRefresh());
  },
  methods: {
    typeLabel(t) {
      return MATERIAL_TYPE[t] || t;
    },
    ellipsis,

    async reload() {
      this.requestVersion++;
      this.loading = false;
      this.page = 1;
      this.hasMore = true;
      this.materials = [];
      return this.loadMore();
    },

    async loadMore() {
      if (this.loading || !this.hasMore) return;
      const version = this.requestVersion;
      this.loading = true;
      this.error = "";
      try {
        const resp =
          this.scope === "favorites"
            ? await libraryApi.myFavorites()
            : this.kw
              ? await libraryApi.searchMaterials(
                  this.kw,
                  this.filter.type,
                  this.page,
                  this.size,
                )
              : await libraryApi.listMaterials({
                  page: this.page,
                  size: this.size,
                  materialType: this.filter.type,
                });
        if (version !== this.requestVersion) return;
        const raw = Array.isArray(resp)
          ? resp
          : resp?.records || resp?.list || [];
        const arr =
          this.scope === "favorites"
            ? raw.filter(
                (item) =>
                  (!this.kw || (item.title || "").includes(this.kw)) &&
                  (!this.filter.type || item.materialType === this.filter.type),
              )
            : raw;
        this.materials = this.page === 1 ? arr : this.materials.concat(arr);
        if (this.scope === "favorites" || arr.length < this.size)
          this.hasMore = false;
        else this.page++;
      } catch (e) {
        if (version !== this.requestVersion) return;
        this.error = e.message || "加载失败，请重试";
        this.hasMore = false;
      } finally {
        if (version === this.requestVersion) this.loading = false;
      }
    },

    goDetail(id) {
      uni.navigateTo({ url: `/pages/library/detail?id=${id}` });
    },
  },
};
</script>

<style lang="scss" scoped>
.library-page {
  padding-bottom: 240rpx;
}

.hero {
  background: linear-gradient(120deg, #2b66a0, #95d4e7);
  color: #fff;
  padding: 32rpx;
}
.hero-title {
  font-size: 36rpx;
  font-weight: 700;
}
.hero-desc {
  font-size: 24rpx;
  opacity: 0.9;
  margin-top: 8rpx;
}

.list {
  padding: 16rpx 24rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(15, 35, 60, 0.04);
}
.row1 {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}
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
.tag-regulation {
  background: #ecf5fc;
  color: #1e5a96;
}
.tag-draft {
  background: #f5e9fa;
  color: #8e44ad;
}
.tag-report {
  background: #e8f7e6;
  color: #39734c;
}
.tag-expert_opinion {
  background: #fdf6ec;
  color: #9b621f;
}
.tag-case {
  background: #fef0f0;
  color: #a44342;
}

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
.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  font-size: 22rpx;
}
.text-secondary {
  color: #64748b;
}
.fz-12 {
  font-size: 24rpx;
}
.ml-12 {
  margin-left: 12rpx;
}

.more-btn,
.more-end {
  text-align: center;
  padding: 24rpx 0;
  font-size: 24rpx;
}
.more-btn {
  color: #1e5a96;
}
.more-end {
  color: #64748b;
}
</style>

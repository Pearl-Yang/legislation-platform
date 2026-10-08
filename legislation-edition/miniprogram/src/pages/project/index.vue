<template>
  <view class="project-page">
    <page-heading
      title="立法项目"
      description="从立项到公布，跟踪每一步进度"
      icon="scale"
    />
    <search-bar v-model="kw" placeholder="搜索已加载的项目" />
    <!-- 顶部统计 -->
    <view class="hero">
      <view class="hero-cell">
        <view class="hc-value">{{ summary.total }}</view>
        <view class="hc-label">项目总数</view>
      </view>
      <view class="hero-cell">
        <view class="hc-value hc-active">{{ summary.active }}</view>
        <view class="hc-label">进行中</view>
      </view>
      <view class="hero-cell">
        <view class="hc-value hc-published">{{ summary.published }}</view>
        <view class="hc-label">已发布</view>
      </view>
      <view class="hero-cell">
        <view class="hc-value hc-obsolete">{{ summary.obsolete }}</view>
        <view class="hc-label">已废止</view>
      </view>
    </view>

    <!-- 类型过滤 -->
    <filter-pills :options="typeOpts" v-model="filter.type" />
    <!-- 状态过滤 -->
    <filter-pills :options="statusOpts" v-model="filter.status" />

    <!-- 列表 -->
    <view class="list">
      <view
        v-for="p in visibleList"
        :key="p.id"
        class="card"
        @click="goDetail(p.id)"
      >
        <view class="row1">
          <text class="name">{{ p.projectName }}</text>
          <view
            class="type-badge"
            :style="{ background: typeColor(p.projectType) }"
          >
            {{ typeLabel(p.projectType) }}
          </view>
        </view>
        <view class="desc">{{ p.description || "暂无项目说明" }}</view>
        <view class="meta">
          <view class="meta-item">
            <text class="mi-label">状态</text>
            <text
              class="text-primary"
              :style="{ color: statusColor(p.status) }"
            >
              {{ statusLabel(p.status) }}
            </text>
          </view>
          <view class="meta-item" v-if="p.publishDate">
            <text class="mi-label">发布</text>
            <text>{{ p.publishDate }}</text>
          </view>
          <view class="meta-item">
            <text class="mi-label">创建</text>
            <text>{{ relativeTime(p.createdAt) }}</text>
          </view>
        </view>
        <view class="row-progress" v-if="p.progress != null">
          <view class="rp-bar"
            ><view class="rp-fill" :style="{ width: p.progress + '%' }"></view
          ></view>
          <text class="rp-text">{{ p.progress }}%</text>
        </view>
      </view>

      <data-state :loading="loading" :error="error" @retry="reload" />
      <empty
        v-if="!loading && !error && visibleList.length === 0"
        text="暂无项目"
        desc="尝试切换其他过滤条件"
      />
      <view
        v-if="!loading && !error && hasMore"
        class="more-btn"
        @click="loadMore"
        >点击加载更多</view
      >
      <view v-else-if="!loading && !error && list.length > 0" class="more-end"
        >— 已经到底了 —</view
      >
    </view>
  </view>
</template>

<script>
import PageHeading from "@/components/PageHeading.vue";
import SearchBar from "@/components/SearchBar.vue";
import DataState from "@/components/DataState.vue";
import FilterPills from "@/components/FilterPills.vue";
import Empty from "@/components/Empty.vue";
import LoadingBlock from "@/components/LoadingBlock.vue";
import {
  PROJECT_TYPE,
  PROJECT_STATUS,
  relativeTime,
  goPage,
  pickList,
} from "@/utils/index.js";
import { projectApi } from "@/api/index.js";

export default {
  components: {
    PageHeading,
    SearchBar,
    DataState,
    FilterPills,
    Empty,
    LoadingBlock,
  },
  data() {
    return {
      typeOpts: [
        { label: "全部类型", value: "" },
        { label: "行政法规", value: "ADMIN_REGULATION" },
        { label: "部门规章", value: "DEPT_RULE" },
        { label: "地方政府规章", value: "LOCAL_RULE" },
      ],
      statusOpts: [
        { label: "全部状态", value: "" },
        { label: "草稿", value: "DRAFT" },
        { label: "进行中", value: "ACTIVE" },
        { label: "已发布", value: "PUBLISHED" },
        { label: "已废止", value: "OBSOLETE" },
      ],
      kw: "",
      error: "",
      filter: { type: "", status: "" },
      list: [],
      loading: false,
      requestVersion: 0,
      page: 1,
      size: 10,
      hasMore: true,
      summary: { total: 0, active: 0, published: 0, obsolete: 0 },
    };
  },
  computed: {
    visibleList() {
      return this.list.filter((item) =>
        (item.projectName || "").includes(this.kw.trim()),
      );
    },
  },
  watch: {
    "filter.type": "reload",
    "filter.status": "reload",
  },
  onShow() {
    this.loadSummary();
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
      return (PROJECT_TYPE[t] || { label: t }).label;
    },
    typeColor(t) {
      return (PROJECT_TYPE[t] || { color: "#1e5a96" }).color;
    },
    statusLabel(s) {
      return (PROJECT_STATUS[s] || { label: s }).label;
    },
    statusColor(s) {
      return (PROJECT_STATUS[s] || { color: "#1e5a96" }).color;
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/project/detail?id=${id}` });
    },
    relativeTime,

    async loadSummary() {
      try {
        const d = await projectApi.dashboard();
        // 后端可能是 Map{activeCount, draftCount, publishedCount, obsoleteCount, total}
        this.summary = {
          total: Number(d.total ?? d.totalCount ?? 0),
          active: Number(d.activeCount ?? d.active ?? 0),
          published: Number(d.publishedCount ?? d.published ?? 0),
          obsolete: Number(d.obsoleteCount ?? d.obsolete ?? 0),
        };
      } catch (e) {
        /* 静默 */
      }
    },

    async reload() {
      this.requestVersion++;
      this.loading = false;
      this.page = 1;
      this.hasMore = true;
      this.list = [];
      return this.loadMore();
    },

    async loadMore() {
      if (this.loading || !this.hasMore) return;
      const version = this.requestVersion;
      this.loading = true;
      this.error = "";
      try {
        const resp = await projectApi.listProjects({
          page: this.page,
          size: this.size,
          projectType: this.filter.type,
          status: this.filter.status,
        });
        if (version !== this.requestVersion) return;
        const arr = pickList(resp);
        this.list = this.page === 1 ? arr : this.list.concat(arr);
        if (arr.length < this.size) this.hasMore = false;
        else this.page++;
      } catch (e) {
        if (version !== this.requestVersion) return;
        this.error = e.message || "加载失败，请重试";
        if (this.page === 1) this.list = [];
        this.hasMore = false;
      } finally {
        if (version === this.requestVersion) this.loading = false;
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.project-page {
  padding-bottom: 240rpx;
}

// 顶部统计
.hero {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  background: #235a91;
  color: #fff;
  padding: 32rpx 16rpx;
  margin-bottom: 16rpx;
}
.hero-cell {
  text-align: center;
}
.hc-value {
  font-size: 44rpx;
  font-weight: 700;
}
.hc-label {
  font-size: 22rpx;
  opacity: 0.9;
  margin-top: 4rpx;
}
.hc-active {
  color: #ffe57f;
}
.hc-published {
  color: #b8f5cd;
}
.hc-obsolete {
  color: #ffb1b1;
}

// 列表
.list {
  padding: 16rpx 24rpx 32rpx;
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
.name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
  line-height: 1.4;
}
.type-badge {
  flex-shrink: 0;
  color: #fff;
  padding: 4rpx 14rpx;
  font-size: 22rpx;
  border-radius: 8rpx;
}
.desc {
  font-size: 24rpx;
  color: #6b7280;
  margin: 12rpx 0 16rpx;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx;
  font-size: 22rpx;
  color: #1f2937;
}
.meta-item {
  display: flex;
  gap: 8rpx;
  align-items: center;
}
.mi-label {
  color: #64748b;
}

.row-progress {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
}
.rp-bar {
  flex: 1;
  height: 8rpx;
  background: #f0f4f8;
  border-radius: 4rpx;
  overflow: hidden;
}
.rp-fill {
  height: 100%;
  background: linear-gradient(90deg, #1e5a96, #39734c);
}
.rp-text {
  font-size: 22rpx;
  color: #6b7280;
  flex-shrink: 0;
}

.more-btn,
.more-end {
  text-align: center;
  font-size: 24rpx;
  color: #1e5a96;
  padding: 24rpx 0;
}
.more-end {
  color: #64748b;
}
</style>

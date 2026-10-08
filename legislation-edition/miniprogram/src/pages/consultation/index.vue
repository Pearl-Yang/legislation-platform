<template>
  <view class="consultation-page">
    <page-heading
      title="意见征集"
      description="让公众意见参与立法全过程"
      icon="messages-square"
    />

    <view class="publish-entry"
      ><button class="primary-button" @click="goPublish">
        发布意见征集
      </button></view
    >
    <!-- 状态过滤 -->
    <filter-pills :options="statusOpts" v-model="filter.status" />

    <!-- 列表 -->
    <view class="list">
      <view v-for="c in list" :key="c.id" class="card" @click="goDetail(c.id)">
        <view class="row1">
          <text class="title">{{ c.title }}</text>
          <view :class="['tag', `tag-${(c.status || '').toLowerCase()}`]">
            {{ statusLabel(c.status) }}
          </view>
        </view>
        <view class="desc">{{ ellipsis(c.description, 100) }}</view>
        <view class="meta">
          <text class="meta-cell">
            <text class="mi-label">周期</text>
            <text>{{ c.startDate }} ~ {{ c.endDate }}</text>
          </text>
          <text class="meta-cell ml-12">
            <text class="mi-label">意见</text>
            <text class="text-primary fw-600">{{ c.totalOpinions || 0 }}</text>
          </text>
          <text class="meta-cell ml-12" v-if="c.totalViews">
            <text class="mi-label">浏览</text>
            <text>{{ c.totalViews }}</text>
          </text>
        </view>
      </view>

      <data-state :loading="loading" :error="error" @retry="reload" />
      <empty
        v-if="!loading && !error && list.length === 0"
        text="暂无征集"
        desc="暂未发布意见征集活动"
      />
    </view>
  </view>
</template>

<script>
import PageHeading from "@/components/PageHeading.vue";
import DataState from "@/components/DataState.vue";
import FilterPills from "@/components/FilterPills.vue";
import Empty from "@/components/Empty.vue";
import LoadingBlock from "@/components/LoadingBlock.vue";
import { CONSULTATION_STATUS, ellipsis } from "@/utils/index.js";
import { consultationApi } from "@/api/index.js";

export default {
  components: { PageHeading, DataState, FilterPills, Empty, LoadingBlock },
  data() {
    return {
      filter: { status: "" },
      statusOpts: [
        { label: "全部", value: "" },
        { label: "征集中", value: "OPEN" },
        { label: "已结束", value: "CLOSED" },
      ],
      list: [],
      requestVersion: 0,
      error: "",
      loading: false,
    };
  },
  watch: {
    "filter.status": "reload",
  },
  onShow() {
    this.reload();
  },
  onPullDownRefresh() {
    this.reload().then(() => uni.stopPullDownRefresh());
  },
  methods: {
    statusLabel(s) {
      return (CONSULTATION_STATUS[s] || { label: s }).label;
    },
    ellipsis,

    async reload() {
      const version = ++this.requestVersion;
      this.loading = true;
      this.error = "";
      try {
        const list = await consultationApi.listConsultations(
          this.filter.status,
          1,
          30,
        );
        if (version !== this.requestVersion) return;
        this.list = Array.isArray(list) ? list : list?.records || [];
      } catch (e) {
        if (version !== this.requestVersion) return;
        this.error = e.message || "加载失败，请重试";
        this.list = [];
      } finally {
        if (version === this.requestVersion) this.loading = false;
      }
    },

    goPublish() {
      uni.navigateTo({ url: "/pages/consultation/publish" });
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/consultation/detail?id=${id}` });
    },
  },
};
</script>

<style lang="scss" scoped>
.consultation-page {
  padding-bottom: 240rpx;
}

.hero {
  background: linear-gradient(120deg, #17a2b8, #5cdbd3);
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
.tag-draft {
  background: #f4f4f5;
  color: #64748b;
}
.tag-open {
  background: #ecf5fc;
  color: #1e5a96;
}
.tag-closed {
  background: #e8f7e6;
  color: #39734c;
}

.desc {
  font-size: 24rpx;
  color: #6b7280;
  line-height: 1.6;
  margin: 12rpx 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8rpx;
  font-size: 22rpx;
}
.meta-cell {
  display: flex;
  gap: 6rpx;
  align-items: center;
}
.mi-label {
  color: #64748b;
}
.text-primary {
  color: #1e5a96;
}
.fw-600 {
  font-weight: 600;
}
.ml-12 {
  margin-left: 12rpx;
}
.publish-entry {
  padding: 0 24rpx 24rpx;
  background: #edf3fa;
}
</style>

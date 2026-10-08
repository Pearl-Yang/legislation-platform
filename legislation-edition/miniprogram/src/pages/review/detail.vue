<template>
  <view class="review-detail" v-if="record">
    <view class="card">
      <view class="row1">
        <view
          :class="[
            'pass-badge',
            record.overallPass ? 'badge-pass' : 'badge-fail',
          ]"
        >
          {{ record.overallPass ? "✓ 整体通过" : "✗ 存在问题" }}
        </view>
        <view class="record-id">记录 #{{ record.id }}</view>
      </view>
      <view class="meta">
        <text class="text-secondary fz-12"
          >审查人：{{ record.reviewedBy || "自动" }}</text
        >
        <text class="text-secondary fz-12 ml-12">{{
          relativeTime(record.reviewedAt)
        }}</text>
      </view>
    </view>

    <view class="card">
      <view class="card-title">问题列表</view>
      <view v-for="iss in record.issues || []" :key="iss.id" class="iss-row">
        <view
          :class="['sev-badge', `sev-${(iss.severity || '').toLowerCase()}`]"
          >{{ sevLabel(iss.severity) }}</view
        >
        <view class="iss-body">
          <view class="iss-title"
            >{{ issueTypeLabel(iss.issueType) }} · {{ iss.articleIndex }}</view
          >
          <view class="iss-desc">{{ iss.description }}</view>
          <view v-if="iss.suggestion" class="iss-sug"
            >建议：{{ iss.suggestion }}</view
          >
        </view>
      </view>
    </view>
  </view>
  <data-state
    v-else
    :loading="loading"
    :error="error"
    :empty="!loading && !error"
    title="暂无详情"
    description="记录可能已移除，请返回列表查看"
    @retry="reload"
  />
</template>

<script>
import DataState from "@/components/DataState.vue";
import Empty from "@/components/Empty.vue";
import LoadingBlock from "@/components/LoadingBlock.vue";
import { SEVERITY, ISSUE_TYPE, relativeTime } from "@/utils/index.js";
import { reviewApi } from "@/api/index.js";

export default {
  components: { DataState, Empty, LoadingBlock },
  data() {
    return { id: null, record: null, error: "", loading: true };
  },
  onLoad(opts) {
    this.id = Number(opts.id) || null;
  },
  onShow() {
    this.reload();
  },
  methods: {
    relativeTime,
    sevLabel(s) {
      return (SEVERITY[s] || { label: s }).label;
    },
    issueTypeLabel(t) {
      return ISSUE_TYPE[t] || t;
    },

    async reload() {
      this.error = "";
      this.loading = true;
      try {
        this.record = await reviewApi.reviewRecord(this.id);
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.review-detail {
  padding: 24rpx 24rpx 200rpx;
}
.row1 {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pass-badge {
  font-size: 28rpx;
  padding: 8rpx 20rpx;
  border-radius: 8rpx;
  color: #fff;
}
.badge-pass {
  background: #39734c;
}
.badge-fail {
  background: #a44342;
}
.record-id {
  font-size: 24rpx;
  color: #64748b;
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
.meta {
  margin-top: 8rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}
.card-title::before {
  content: "";
  display: inline-block;
  width: 8rpx;
  height: 28rpx;
  background: #1e5a96;
  margin-right: 12rpx;
  border-radius: 4rpx;
}

.iss-row {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child {
    border-bottom: none;
  }
}
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
.sev-red {
  background: #a44342;
}
.sev-yellow {
  background: #9b621f;
}
.sev-blue {
  background: #2b66a0;
}
.sev-grey {
  background: #64748b;
}
.iss-body {
  flex: 1;
  min-width: 0;
}
.iss-title {
  font-size: 26rpx;
  color: #1f2937;
}
.iss-desc {
  font-size: 24rpx;
  color: #6b7280;
  margin-top: 6rpx;
  line-height: 1.6;
}
.iss-sug {
  font-size: 24rpx;
  color: #39734c;
  margin-top: 8rpx;
}
</style>

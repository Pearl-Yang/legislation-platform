<template>
  <view class="project-detail" v-if="project">
    <!-- 顶部项目信息 -->
    <view class="card hero">
      <view class="hero-title">
        <text>{{ project.projectName }}</text>
        <view
          class="type-badge"
          :style="{ background: typeColor(project.projectType) }"
        >
          {{ typeLabel(project.projectType) }}
        </view>
      </view>
      <view class="hero-desc">{{ project.description || "暂无项目说明" }}</view>
      <view class="hero-meta">
        <view class="meta-item">
          <text class="mi-label">状态</text>
          <text :style="{ color: statusColor(project.status) }">{{
            statusLabel(project.status)
          }}</text>
        </view>
        <view class="meta-item" v-if="project.publishDate">
          <text class="mi-label">发布日期</text>
          <text>{{ project.publishDate }}</text>
        </view>
        <view class="meta-item">
          <text class="mi-label">创建</text>
          <text>{{ relativeTime(project.createdAt) }}</text>
        </view>
      </view>
      <view v-if="progress != null" class="progress-bar">
        <view class="pb-fill" :style="{ width: progress + '%' }"></view>
        <text class="pb-text">{{ progress }}%</text>
      </view>
    </view>

    <!-- 阶段流程图 -->
    <view class="card">
      <view class="card-header">
        <view class="card-title">流程节点</view>
        <text class="text-primary fz-12" @click="reload">刷新</text>
      </view>
      <stage-bar :stages="stages" @tap="onStageTap" />
      <view v-if="currentStage" class="cur-stage">
        当前：<text class="text-primary fw-600">{{
          currentStage.stageName
        }}</text>
      </view>
    </view>

    <!-- 期限预警 -->
    <view class="card" v-if="deadlines && deadlines.length">
      <view class="card-header">
        <view class="card-title"
          ><app-icon name="clock" :size="32" /> 即将到期</view
        >
        <text class="fz-12 text-secondary">{{ deadlines.length }} 项</text>
      </view>
      <view v-for="d in deadlines" :key="d.id || d.deadlineDate" class="dl-row">
        <view class="dl-name">{{ d.stageName || d.nodeName || "阶段" }}</view>
        <view class="dl-date">{{ d.deadlineDate }}</view>
        <view :class="['dl-tag', d.urgent ? 'tag-warn' : 'tag-info']">
          {{
            d.daysLeft >= 0 ? d.daysLeft + " 天" : "逾期 " + -d.daysLeft + " 天"
          }}
        </view>
      </view>
    </view>

    <!-- 操作区 -->
    <view class="card">
      <view class="card-header">
        <view class="card-title">操作</view>
      </view>
      <view class="op-grid">
        <view class="op-btn" @click="onAdvance">
          <view class="op-icon" style="background: #39734c"
            ><app-icon name="arrow-right" :size="36" tone="white"
          /></view>
          <text>推进下一阶段</text>
        </view>
        <view class="op-btn" @click="onRollback">
          <view class="op-icon" style="background: #a44342"
            ><app-icon name="history" :size="36" tone="white"
          /></view>
          <text>回退</text>
        </view>
        <view class="op-btn" @click="goDrafts">
          <view class="op-icon" style="background: #8e44ad"
            ><app-icon name="file-pen-line" :size="36" tone="white"
          /></view>
          <text>查看草案</text>
        </view>
        <view class="op-btn" @click="goCleanup">
          <view class="op-icon" style="background: #9b621f"
            ><app-icon name="brush-cleaning" :size="36" tone="white"
          /></view>
          <text>清理建议</text>
        </view>
      </view>
    </view>

    <!-- 节点详情列表 -->
    <view class="card">
      <view class="card-header">
        <view class="card-title">节点详情</view>
        <text class="fz-12 text-secondary">共 {{ stages.length }} 步</text>
      </view>
      <view
        v-for="s in stages"
        :key="s.id"
        :class="['stage-row', s.status === 'IN_PROGRESS' ? 'row-active' : '']"
      >
        <view class="sr-no">{{ s.stageOrder }}</view>
        <view class="sr-body">
          <view class="sr-name">{{ s.stageName }}</view>
          <view class="sr-meta">
            <text class="text-secondary">{{ s.stageCode }}</text>
            <text v-if="s.operatorTime" class="text-secondary ml-12">
              {{ relativeTime(s.operatorTime) }}
            </text>
          </view>
          <view v-if="s.remark" class="sr-remark">{{ s.remark }}</view>
        </view>
        <view :class="['dot', dotCls(s.status)]"></view>
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
import StageBar from "@/components/StageBar.vue";
import Empty from "@/components/Empty.vue";
import LoadingBlock from "@/components/LoadingBlock.vue";
import {
  PROJECT_TYPE,
  PROJECT_STATUS,
  STAGE_STATUS,
  relativeTime,
  showLoading,
  hideLoading,
} from "@/utils/index.js";
import { projectApi } from "@/api/index.js";

export default {
  components: { DataState, StageBar, Empty, LoadingBlock },
  data() {
    return {
      id: null,
      project: null,
      stages: [],
      currentStage: null,
      progress: null,
      deadlines: [],
      error: "",
      loading: true,
    };
  },
  onLoad(opts) {
    this.id = Number(opts.id) || null;
  },
  onShow() {
    this.reload();
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
    dotCls(s) {
      switch (s) {
        case "DONE":
          return "dot-done";
        case "IN_PROGRESS":
          return "dot-doing";
        case "RETURNED":
          return "dot-returned";
        case "SKIPPED":
          return "dot-skipped";
        default:
          return "dot-pending";
      }
    },
    relativeTime,

    async reload() {
      this.error = "";
      this.loading = true;
      try {
        const [detail, progress, stages, cur, deadlines] =
          await Promise.allSettled([
            projectApi.projectDetail(this.id),
            projectApi.projectProgress(this.id),
            projectApi.listStages(this.id),
            projectApi.currentStage(this.id),
            projectApi.projectDeadlines(this.id),
          ]);
        if (detail.status === "rejected") throw detail.reason;
        if (detail.status === "fulfilled") {
          this.project = detail.value;
        }
        if (progress.status === "fulfilled") {
          this.progress = progress.value?.progress ?? null;
        }
        if (stages.status === "fulfilled") {
          this.stages = Array.isArray(stages.value) ? stages.value : [];
        }
        if (cur.status === "fulfilled") {
          this.currentStage = cur.value || null;
        }
        if (deadlines.status === "fulfilled") {
          this.deadlines = Array.isArray(deadlines.value)
            ? deadlines.value
            : [];
        }
      } catch (e) {
        this.error = e.message || "加载失败，请重试";
      } finally {
        this.loading = false;
      }
    },

    onStageTap(stage) {
      uni.showToast({ title: stage.stageName, icon: "none" });
    },

    async onAdvance() {
      if (!this.project) return;
      const { confirm } = await new Promise((r) =>
        uni.showModal({
          title: "推进阶段",
          content: `将推进「${this.project.projectName}」到下一阶段？`,
          success: ({ confirm }) => r({ confirm }),
        }),
      );
      if (!confirm) return;
      showLoading();
      try {
        await projectApi.advanceProject(this.id, "小程序端推进");
        uni.showToast({ title: "已推进", icon: "success" });
        this.reload();
      } finally {
        hideLoading();
      }
    },

    async onRollback() {
      if (!this.project || !this.stages.length) return;
      // 简化：弹一个输入框选择目标顺序号
      uni.showToast({ title: "请在 Web 端操作回退（带原因）", icon: "none" });
    },

    goDrafts() {
      uni.navigateTo({ url: `/pages/draft/index?projectId=${this.id}` });
    },
    goCleanup() {
      uni.navigateTo({ url: `/pages/cleanup/index?projectId=${this.id}` });
    },
  },
};
</script>

<style lang="scss" scoped>
.project-detail {
  padding: 24rpx 24rpx 240rpx;
}

// Hero
.hero {
  padding: 32rpx;
}
.hero-title {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}
.hero-title text {
  font-size: 36rpx;
  font-weight: 700;
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
.hero-desc {
  font-size: 26rpx;
  color: #6b7280;
  line-height: 1.5;
  margin: 16rpx 0;
}
.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx;
  font-size: 24rpx;
}
.meta-item {
  display: flex;
  gap: 8rpx;
  align-items: center;
}
.mi-label {
  color: #64748b;
}

.progress-bar {
  position: relative;
  margin-top: 24rpx;
  height: 16rpx;
  background: #f0f4f8;
  border-radius: 8rpx;
  overflow: hidden;
}
.pb-fill {
  height: 100%;
  background: linear-gradient(90deg, #1e5a96, #39734c);
}
.pb-text {
  position: absolute;
  right: 0;
  top: -32rpx;
  font-size: 22rpx;
  color: #6b7280;
}

// 阶段流程
.cur-stage {
  margin-top: 16rpx;
  padding: 16rpx;
  background: #f0f4f8;
  border-radius: 12rpx;
  font-size: 26rpx;
}
.text-primary {
  color: #1e5a96;
}
.text-secondary {
  color: #64748b;
}
.fz-12 {
  font-size: 24rpx;
}
.fw-600 {
  font-weight: 600;
}
.ml-12 {
  margin-left: 12rpx;
}

// 期限
.dl-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child {
    border-bottom: none;
  }
}
.dl-name {
  flex: 1;
  font-size: 26rpx;
  color: #1f2937;
}
.dl-date {
  font-size: 24rpx;
  color: #6b7280;
}
.dl-tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}
.tag-info {
  background: #ecf5fc;
  color: #1e5a96;
}
.tag-warn {
  background: #fdf6ec;
  color: #9b621f;
}

// 操作
.op-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}
.op-btn {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #1f2937;
}
.op-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
}

// 节点详情
.stage-row {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 16rpx 8rpx;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child {
    border-bottom: none;
  }
  &.row-active {
    background: #f0f7ff;
    border-radius: 12rpx;
  }
}
.sr-no {
  width: 36rpx;
  height: 36rpx;
  background: #f0f4f8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #6b7280;
  flex-shrink: 0;
}
.sr-body {
  flex: 1;
  min-width: 0;
}
.sr-name {
  font-size: 28rpx;
  color: #1f2937;
}
.sr-meta {
  font-size: 22rpx;
  margin-top: 4rpx;
}
.sr-remark {
  font-size: 22rpx;
  color: #6b7280;
  background: #f9fafb;
  padding: 8rpx 12rpx;
  border-radius: 6rpx;
  margin-top: 8rpx;
}

.dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-top: 10rpx;
  flex-shrink: 0;
}
.dot-pending {
  background: #d1d5db;
}
.dot-done {
  background: #39734c;
}
.dot-doing {
  background: #1e5a96;
}
.dot-returned {
  background: #a44342;
}
.dot-skipped {
  background: #64748b;
}
</style>

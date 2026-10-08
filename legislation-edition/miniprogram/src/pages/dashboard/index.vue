<template>
  <view class="dashboard mobile-content"
    ><view class="welcome"
      ><view
        ><view class="welcome-date">{{ today }}</view
        ><view class="welcome-title">{{ greeting }}，{{ userName }}</view
        ><view class="welcome-subtitle">让立法工作有序向前</view></view
      ><button
        class="avatar"
        aria-label="查看个人信息"
        @click="goPage('/pages/profile/index')"
      >
        <app-icon name="user-round" :size="42" /></button
    ></view>
    <data-state
      v-if="loading || error"
      :loading="loading"
      :error="error"
      @retry="loadAll"
    />
    <view class="refresh-row"
      ><text>工作概览 · {{ updatedAt || "等待更新" }}</text
      ><button :disabled="loading" :loading="loading" @click="loadAll">
        刷新数据
      </button></view
    >
    <view class="stats-grid"
      ><stat-card
        label="进行中项目"
        :value="stats.active"
        hint="查看项目进度"
        @click="goPage('/pages/project/index')" /><stat-card
        label="近期到期"
        :value="stats.upcoming"
        hint="未来 30 天的工作"
        @click="goPage('/pages/project/index')" /><stat-card
        label="公开征集"
        :value="stats.consultation"
        hint="收集公众意见"
        @click="goPage('/pages/consultation/index')" /><stat-card
        label="实施评估"
        :value="stats.evaluation"
        hint="跟踪法规实施"
        @click="goPage('/pages/evaluation/index')"
    /></view>
    <view class="card module-card"
      ><section-header
        title="业务模块"
        description="立法全生命周期，八个工作入口" /><module-grid
        :items="modules"
        @select="goPage"
    /></view>
    <view class="card deadline-card"
      ><section-header
        title="期限提醒"
        :description="
          deadlines.length
            ? deadlines.length + ' 项近期工作需要关注'
            : '关注未来 30 天的工作期限'
        "
        action="全部项目"
        @action="goPage('/pages/project/index')"
      /><data-state
        :loading="loading"
        :error="error"
        :empty="!deadlines.length"
        title="暂无期限提醒"
        description="已加载的项目暂无近期到期节点"
        @retry="loadAll"
      /><button
        v-for="item in deadlines"
        :key="item.id"
        class="deadline-item"
        @click="openProject(item.projectId)"
      >
        <view
          class="deadline-dot"
          :class="{ urgent: item.daysLeft < 7 }"
        /><view class="deadline-copy"
          ><view class="deadline-title">{{
            item.projectName || "立法项目"
          }}</view
          ><view class="deadline-caption"
            >{{ item.stageName || item.nodeName || "待推进节点" }} ·
            {{ date(item.deadlineDate || item.deadlineAt) }}</view
          ></view
        ><view
          class="deadline-tag"
          :class="{ 'tag-urgent': item.daysLeft < 7 }"
          >{{
            item.daysLeft < 0 ? "已逾期" : "剩 " + item.daysLeft + " 天"
          }}</view
        >
      </button></view
    >
    <view class="guide-card"
      ><view class="guide-mark"
        ><app-icon name="messages-square" :size="42" /></view
      ><view class="guide-copy"
        ><view class="guide-title">听见公众的声音</view
        ><view class="guide-description"
          >查看公开征集，阅读草案并提交建议</view
        ></view
      ><button
        class="guide-action"
        aria-label="查看意见征集"
        @click="goPage('/pages/consultation/index')"
      >
        <app-icon name="arrow-right" :size="36" /></button
    ></view>
  </view>
</template>
<script>
import DataState from "@/components/DataState.vue";
import StatCard from "@/components/StatCard.vue";
import SectionHeader from "@/components/SectionHeader.vue";
import ModuleGrid from "@/components/ModuleGrid.vue";
import { projectApi, consultationApi, evaluationApi } from "@/api/index.js";
import { formatDate, daysFromNow, goPage, pickList } from "@/utils/index.js";
export default {
  components: { DataState, StatCard, SectionHeader, ModuleGrid },
  data: () => ({
    updatedAt: "",
    today: "",
    userName: "",
    loading: false,
    error: "",
    stats: {
      active: null,
      upcoming: null,
      consultation: null,
      evaluation: null,
    },
    deadlines: [],
    modules: [
      {
        title: "立法项目",
        description: "全流程管理",
        icon: "scale",
        path: "/pages/project/index",
        color: "#235a91",
        background: "#e8f0fa",
      },
      {
        title: "草案生成",
        description: "辅助起草",
        icon: "file-pen-line",
        path: "/pages/draft/index",
        color: "#7253a2",
        background: "#f0eaf8",
      },
      {
        title: "智慧审查",
        description: "风险核查",
        icon: "shield-check",
        path: "/pages/review/index",
        color: "#a44342",
        background: "#faeaea",
      },
      {
        title: "智能清理",
        description: "联动清理",
        icon: "brush-cleaning",
        path: "/pages/cleanup/index",
        color: "#8a541c",
        background: "#fbefdf",
      },
      {
        title: "实施评估",
        description: "效果跟踪",
        icon: "chart-no-axes-combined",
        path: "/pages/evaluation/index",
        color: "#3d7250",
        background: "#e8f3eb",
      },
      {
        title: "意见征集",
        description: "公众参与",
        icon: "messages-square",
        path: "/pages/consultation/index",
        color: "#24747b",
        background: "#e4f2f3",
      },
      {
        title: "资料库",
        description: "全文检索",
        icon: "library",
        path: "/pages/library/index",
        color: "#235a91",
        background: "#e8f0fa",
      },
      {
        title: "立法动态",
        description: "资讯聚合",
        icon: "newspaper",
        path: "/pages/info/index",
        color: "#536278",
        background: "#edf0f4",
      },
    ],
  }),
  computed: {
    avatar() {
      return (this.userName || "智").slice(0, 1);
    },
    greeting() {
      const h = new Date().getHours();
      return h < 11
        ? "早上好"
        : h < 14
          ? "中午好"
          : h < 18
            ? "下午好"
            : "晚上好";
    },
  },
  onShow() {
    this.userName = uni.getStorageSync("userName") || "立法工作者";
    this.today = formatDate(new Date(), "YYYY年MM月DD日");
    this.loadAll();
  },
  onPullDownRefresh() {
    this.loadAll().finally(() => uni.stopPullDownRefresh());
  },
  methods: {
    goPage,
    date: formatDate,
    openProject(id) {
      if (id) uni.navigateTo({ url: "/pages/project/detail?id=" + id });
    },
    async loadAll() {
      if (this.loading) return;
      this.loading = true;
      this.error = "";
      const results = await Promise.allSettled([
        projectApi.dashboard(),
        projectApi.upcomingAll(30),
        consultationApi.listConsultations("OPEN"),
        evaluationApi.listEvaluations("RUNNING"),
      ]);
      const [projects, upcoming, consultations, evaluations] = results;
      this.stats.active =
        projects.status === "fulfilled"
          ? Number(projects.value.activeCount ?? projects.value.active ?? 0)
          : null;
      this.deadlines =
        upcoming.status === "fulfilled"
          ? pickList(upcoming.value).map((item, i) => ({
              ...item,
              id: item.id || i,
              daysLeft:
                item.daysLeft ??
                daysFromNow(item.deadlineDate || item.deadlineAt) ??
                0,
            }))
          : [];
      this.stats.upcoming =
        upcoming.status === "fulfilled" ? this.deadlines.length : null;
      this.stats.consultation =
        consultations.status === "fulfilled"
          ? pickList(consultations.value).length
          : null;
      this.stats.evaluation =
        evaluations.status === "fulfilled"
          ? pickList(evaluations.value).length
          : null;
      if (results.some((r) => r.status === "rejected"))
        this.error = "部分数据未能加载，请检查连接后重试";
      this.updatedAt = formatDate(new Date(), "HH:mm");
      this.loading = false;
    },
  },
};
</script>
<style scoped>
.dashboard {
  padding: 32rpx 24rpx 48rpx;
}
.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 12rpx 8rpx 32rpx;
}
.welcome-date {
  font-size: 24rpx;
  color: #5b7089;
  letter-spacing: 2rpx;
}
.welcome-title {
  font-size: 38rpx;
  font-weight: 700;
  color: #163b64;
  margin-top: 12rpx;
  line-height: 1.5;
}
.welcome-subtitle {
  font-size: 26rpx;
  color: #526277;
  margin-top: 8rpx;
}
.avatar {
  width: 88rpx;
  height: 88rpx;
  line-height: 88rpx;
  padding: 0;
  margin: 0;
  background: #dce8f5;
  border-radius: 24rpx;
  font-size: 36rpx;
  color: #235a91;
  flex-shrink: 0;
}
.avatar::after {
  border: none;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin: 0 0 24rpx;
}
.module-card {
  padding-bottom: 36rpx;
}
.deadline-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  text-align: left;
  background: transparent;
  padding: 24rpx 0;
  line-height: 1.5;
  margin: 0;
  border-bottom: 1rpx solid #e7edf4;
}
.deadline-item:last-child {
  border-bottom: 0;
}
.deadline-item::after {
  border: 0;
}
.deadline-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #537da8;
  flex-shrink: 0;
}
.urgent {
  background: #a65a22;
}
.deadline-copy {
  flex: 1;
  min-width: 0;
}
.deadline-title {
  font-size: 28rpx;
  color: #23364d;
  overflow-wrap: anywhere;
}
.deadline-caption {
  font-size: 24rpx;
  color: #617085;
  margin-top: 8rpx;
}
.deadline-tag {
  font-size: 24rpx;
  color: #235a91;
  background: #edf3fa;
  padding: 8rpx 14rpx;
  border-radius: 10rpx;
  flex-shrink: 0;
}
.tag-urgent {
  background: #fff0df;
  color: #99551f;
}
.guide-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: #e8f0fa;
  border: 1rpx solid #dce5ef;
  border-radius: 20rpx;
  padding: 28rpx;
}
.guide-mark {
  font-size: 36rpx;
  color: #235a91;
}
.guide-copy {
  flex: 1;
  min-width: 0;
}
.guide-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #163b64;
}
.guide-description {
  font-size: 24rpx;
  color: #536b87;
  line-height: 1.6;
  margin-top: 8rpx;
}
.guide-action {
  background: transparent;
  color: #235a91;
  line-height: 80rpx;
  padding: 0;
  width: 80rpx;
  margin: 0;
  font-size: 40rpx;
}
.guide-action::after {
  border: 0;
}
.refresh-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #526277;
  font-size: 24rpx;
  margin: 16rpx 0;
}
.refresh-row button {
  margin: 0;
  padding: 0 20rpx;
  line-height: 72rpx;
  background: #e8f0fa;
  color: #235a91;
  font-size: 24rpx;
  border-radius: 12rpx;
}
.refresh-row button::after {
  border: none;
}
@media (min-width: 960px) {
  .dashboard {
    display: grid;
    grid-template-columns: minmax(0, 1.7fr) minmax(300px, 1fr);
    gap: 24px;
    padding: 32px;
  }
  .welcome,
  .refresh-row,
  .stats-grid {
    grid-column: 1 / -1;
  }
  .welcome {
    margin-bottom: 0;
  }
  .refresh-row {
    margin: 0;
  }
  .stats-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    margin: 0;
  }
  .module-card {
    grid-column: 1;
    grid-row: 4 / 6;
    margin: 0;
  }
  .deadline-card {
    grid-column: 2;
    grid-row: 4;
    margin: 0;
  }
  .guide-card {
    grid-column: 2;
    grid-row: 5;
    align-self: start;
  }
  .avatar {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

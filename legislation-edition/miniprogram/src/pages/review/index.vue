<template>
  <view class="review-page"
    ><page-heading
      title="智慧审查"
      description="查看审查规则，或从草案提交风险核查"
      icon="shield-check"
    /><view class="card review-intro"
      ><section-header
        :title="draftId ? '当前草案 #' + draftId : '审查规则库'"
        :description="
          draftId
            ? '提交后可查看真实审查结果及问题列表'
            : '规则用于核查草案，不代表已发现的问题'
        "
      /><button
        class="primary-button"
        :loading="submitting"
        :disabled="submitting"
        @click="submit"
      >
        {{ draftId ? "提交草案审查" : "选择草案并审查" }}
      </button></view
    ><filter-pills v-model="severity" :options="options" /><view class="list"
      ><data-state
        :loading="loading"
        :error="error"
        :empty="!visibleRules.length"
        title="暂无匹配规则"
        description="尝试切换风险等级"
        @retry="reload"
      /><view v-for="rule in visibleRules" :key="rule.id" class="card rule-card"
        ><view class="rule-heading"
          ><view class="rule-name">{{ rule.ruleName || "审查规则" }}</view
          ><status-tag
            :label="severityLabel(rule.severity)"
            :color="severityColor(rule.severity)" /></view
        ><view class="rule-description">{{
          rule.description || "暂无规则说明"
        }}</view
        ><view class="rule-meta"
          >审查规则 · {{ rule.isEnabled === 0 ? "已停用" : "启用中" }}</view
        ></view
      ></view
    ></view
  >
</template>
<script>
import PageHeading from "@/components/PageHeading.vue";
import SectionHeader from "@/components/SectionHeader.vue";
import FilterPills from "@/components/FilterPills.vue";
import DataState from "@/components/DataState.vue";
import StatusTag from "@/components/StatusTag.vue";
import { reviewApi } from "@/api/index.js";
import { SEVERITY, pickList, goPage } from "@/utils/index.js";
export default {
  components: { PageHeading, SectionHeader, FilterPills, DataState, StatusTag },
  data: () => ({
    draftId: null,
    severity: "",
    rules: [],
    loading: false,
    submitting: false,
    error: "",
    options: [
      { label: "全部等级", value: "" },
      { label: "严重", value: "RED" },
      { label: "关注", value: "YELLOW" },
      { label: "格式", value: "BLUE" },
      { label: "优化", value: "GREY" },
    ],
  }),
  computed: {
    visibleRules() {
      return this.rules.filter(
        (r) => !this.severity || r.severity === this.severity,
      );
    },
  },
  onLoad(options) {
    this.draftId = Number(options.draftId) || null;
  },
  onShow() {
    this.reload();
  },
  onPullDownRefresh() {
    this.reload().finally(() => uni.stopPullDownRefresh());
  },
  methods: {
    severityLabel(s) {
      return SEVERITY[s]?.label || s || "未分级";
    },
    severityColor(s) {
      return (
        { RED: "#a44342", YELLOW: "#99551f", BLUE: "#235a91", GREY: "#536278" }[
          s
        ] || "#536278"
      );
    },
    async reload() {
      if (this.loading) return;
      this.loading = true;
      this.error = "";
      try {
        this.rules = pickList(await reviewApi.listRules());
      } catch (error) {
        this.error = error.message;
      } finally {
        this.loading = false;
      }
    },
    async submit() {
      if (!this.draftId) {
        goPage("/pages/draft/index");
        return;
      }
      if (this.submitting) return;
      this.submitting = true;
      try {
        const record = await reviewApi.submitReview(this.draftId, "AUTO");
        const id = record?.id || record?.recordId;
        if (id) uni.navigateTo({ url: "/pages/review/detail?id=" + id });
        else
          uni.showToast({ title: "审查已提交，请稍后查看结果", icon: "none" });
      } catch (error) {
        this.error = error.message;
      } finally {
        this.submitting = false;
      }
    },
  },
};
</script>
<style scoped>
.review-page {
  padding-bottom: 40rpx;
}
.review-intro {
  margin: 24rpx;
}
.list {
  padding: 16rpx 24rpx;
}
.rule-heading {
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
  justify-content: space-between;
}
.rule-name {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  font-weight: 600;
  line-height: 1.5;
  color: #23364d;
}
.rule-description {
  font-size: 26rpx;
  line-height: 1.8;
  color: #526277;
  margin-top: 16rpx;
}
.rule-meta {
  font-size: 24rpx;
  color: #617085;
  margin-top: 16rpx;
}
</style>

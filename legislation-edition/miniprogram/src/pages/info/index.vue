<template>
  <view class="info-page"
    ><page-heading
      title="立法动态"
      description="浏览法规资讯，了解立法工作全貌"
      icon="newspaper"
    /><filter-pills v-model="tab" :options="tabs" /><view class="info-content"
      ><data-state :loading="loading" :error="error" @retry="load" /><view
        v-if="tab === 'dashboard' && !loading && !error"
        ><view class="stats-grid"
          ><stat-card
            label="法规总数"
            :value="dash.totalRegulations"
            hint="已纳入平台的法规" /><stat-card
            label="立法项目"
            :value="dash.totalProjects"
            hint="平台管理的项目" /><stat-card
            label="征集活动"
            :value="dash.totalConsultations"
            hint="已发布的意见征集" /><stat-card
            label="公众意见"
            :value="dash.totalOpinions"
            hint="累计收到的建议" /></view
        ><view class="card"
          ><section-header title="法规类型分布" /><view
            v-for="item in types"
            :key="item.key"
            class="type-row"
            ><view class="type-meta"
              ><text>{{ item.label }}</text
              ><text>{{ item.count }} 部</text></view
            ><view class="bar-track"
              ><view
                class="bar-fill"
                :style="{
                  width: item.percent + '%',
                }" /></view></view></view></view
      ><view v-else-if="!loading && !error"
        ><data-state
          :empty="!entries.length"
          title="暂无资讯"
          description="新的立法动态将在这里更新"
        /><button
          v-for="item in entries"
          :key="item.id || item.title"
          class="card info-item"
          @click="read(item)"
        >
          <view class="info-category">{{ item.category || tabLabel }}</view
          ><view class="info-title">{{
            item.title || item.regulationName
          }}</view
          ><view class="info-description">{{
            item.summary || item.digest || item.description
          }}</view
          ><view class="info-meta"
            >{{ item.source || item.issuingAuthority || "平台资讯" }} ·
            {{ item.publishDate || item.issueDate || "暂无日期" }}</view
          >
        </button></view
      ></view
    ><bottom-sheet v-model="showArticle" :title="article.title || '资讯详情'"
      ><view class="article-body">{{
        article.content ||
        article.fullText ||
        article.summary ||
        article.digest ||
        article.description ||
        "暂无正文"
      }}</view></bottom-sheet
    ></view
  >
</template>
<script>
import PageHeading from "@/components/PageHeading.vue";
import FilterPills from "@/components/FilterPills.vue";
import StatCard from "@/components/StatCard.vue";
import SectionHeader from "@/components/SectionHeader.vue";
import DataState from "@/components/DataState.vue";
import BottomSheet from "@/components/BottomSheet.vue";
import { infoApi } from "@/api/index.js";
import { pickList } from "@/utils/index.js";
export default {
  components: {
    PageHeading,
    FilterPills,
    StatCard,
    SectionHeader,
    DataState,
    BottomSheet,
  },
  data: () => ({
    tab: "news",
    tabs: [
      { label: "立法动态", value: "news" },
      { label: "政策解读", value: "interpretation" },
      { label: "政府公报", value: "bulletin" },
      { label: "数据概览", value: "dashboard" },
    ],
    loading: false,
    error: "",
    version: 0,
    entries: [],
    dash: {},
    showArticle: false,
    article: {},
  }),
  computed: {
    tabLabel() {
      return this.tabs.find((t) => t.value === this.tab)?.label;
    },
    types() {
      const raw = this.dash.byType || {};
      const total = Number(this.dash.totalRegulations) || 0;
      return [
        { key: "ADMIN_REGULATION", label: "行政法规" },
        { key: "DEPT_RULE", label: "部门规章" },
        { key: "LOCAL_RULE", label: "地方政府规章" },
      ].map((item) => ({
        ...item,
        count: Number(raw[item.key] || 0),
        percent: total
          ? Math.min(100, (100 * Number(raw[item.key] || 0)) / total)
          : 0,
      }));
    },
  },
  watch: { tab: "load" },
  onShow() {
    this.load();
  },
  onPullDownRefresh() {
    this.load().finally(() => uni.stopPullDownRefresh());
  },
  methods: {
    async load() {
      const version = ++this.version;
      this.loading = true;
      this.error = "";
      try {
        const response = await {
          news: () => infoApi.newsList(null, 1, 30),
          interpretation: infoApi.policyInterpretations,
          bulletin: infoApi.bulletin,
          dashboard: infoApi.dashboard,
        }[this.tab]();
        if (version !== this.version) return;
        if (this.tab === "dashboard") {
          this.dash = {
            totalRegulations:
              response.totalRegulations ?? response.regulations ?? 0,
            totalProjects: response.totalProjects ?? response.projects ?? 0,
            totalConsultations:
              response.totalConsultations ?? response.consultations ?? 0,
            totalOpinions: response.totalOpinions ?? response.opinions ?? 0,
            byType: response.byType || {},
          };
        } else this.entries = pickList(response);
      } catch (error) {
        if (version === this.version) this.error = error.message;
      } finally {
        if (version === this.version) this.loading = false;
      }
    },
    async read(item) {
      this.article = item;
      this.showArticle = true;
      if (this.tab === "news" && item.id) {
        try {
          const detail = await infoApi.newsDetail(item.id);
          this.article = detail || item;
        } catch {
          /* retain the available summary */
        }
      }
    },
  },
};
</script>
<style scoped>
.info-content {
  padding: 24rpx;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.info-item {
  display: block;
  width: 100%;
  text-align: left;
  line-height: 1.5;
  margin: 0 0 20rpx;
}
.info-item::after {
  border: 0;
}
.info-category {
  font-size: 24rpx;
  color: #235a91;
  margin-bottom: 12rpx;
}
.info-title {
  font-size: 32rpx;
  font-weight: 600;
  line-height: 1.6;
  color: #23364d;
}
.info-description {
  font-size: 26rpx;
  line-height: 1.8;
  color: #526277;
  margin-top: 12rpx;
}
.info-meta {
  font-size: 24rpx;
  color: #617085;
  margin-top: 20rpx;
}
.type-row {
  margin: 24rpx 0;
}
.type-meta {
  display: flex;
  justify-content: space-between;
  font-size: 28rpx;
  color: #526277;
  margin-bottom: 12rpx;
}
.bar-track {
  height: 12rpx;
  border-radius: 8rpx;
  background: #edf3fa;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  background: #537da8;
  border-radius: 8rpx;
}
.article-body {
  font-size: 28rpx;
  line-height: 1.9;
  color: #23364d;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

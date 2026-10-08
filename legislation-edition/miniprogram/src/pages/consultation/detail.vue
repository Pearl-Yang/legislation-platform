<template>
  <view class="consultation-detail" v-if="detail">
    <view class="card hero-card">
      <view class="row1">
        <text class="title">{{ detail.title }}</text>
        <view :class="['tag', `tag-${(detail.status || '').toLowerCase()}`]">
          {{ statusLabel(detail.status) }}
        </view>
      </view>
      <view class="desc">{{ detail.description }}</view>
      <view class="meta">
        <text class="meta-key">征集期</text>
        <text>{{ detail.startDate }} ~ {{ detail.endDate }}</text>
      </view>
      <view class="meta" v-if="detail.relatedProjectId">
        <text class="meta-key">关联项目</text>
        <text>#{{ detail.relatedProjectId }}</text>
      </view>
    </view>

    <!-- 数据看板 -->
    <view class="card">
      <view class="card-title"
        ><app-icon name="chart-column" :size="32" /> 征集概览</view
      >
      <view class="stat-grid">
        <view class="stat-cell">
          <view class="stat-value">{{ opinions.length }}</view>
          <view class="stat-label">已加载意见</view>
        </view>
        <view class="stat-cell">
          <view class="stat-value">{{ loadedStances.neutral }}</view>
          <view class="stat-label">已加载中立</view>
        </view>
        <view class="stat-cell">
          <view class="stat-value">{{ loadedStances.support }}</view>
          <view class="stat-label">已加载支持</view>
        </view>
        <view class="stat-cell">
          <view class="stat-value">{{ loadedStances.oppose }}</view>
          <view class="stat-label">已加载反对</view>
        </view>
      </view>
    </view>

    <!-- 提交意见 -->
    <view class="card" v-if="detail.status === 'OPEN'">
      <view class="card-title"
        ><app-icon name="pencil-line" :size="32" /> 提交我的意见</view
      >
      <view class="form-row">
        <text class="form-key">姓名（可匿名）</text>
        <input v-model="form.submitterName" class="input" placeholder="匿名" />
      </view>
      <view class="form-row">
        <text class="form-key">联系方式（可选）</text>
        <input
          v-model="form.submitterContact"
          class="input"
          placeholder="邮箱 / 手机"
        />
      </view>
      <view class="form-row">
        <text class="form-key">意见立场</text>
        <view class="stance-row">
          <view
            v-for="s in stanceOpts"
            :key="s.value"
            :class="[
              'stance',
              `stance-${s.value.toLowerCase()}`,
              form.viewpoint === s.value ? 'stance-active' : '',
            ]"
            @click="form.viewpoint = s.value"
            >{{ s.label }}</view
          >
        </view>
      </view>
      <view class="form-row">
        <text class="form-key">意见内容</text>
        <textarea
          v-model="form.content"
          class="textarea"
          placeholder="请详细说明您的意见与建议…"
          maxlength="1000"
        />
      </view>
      <view class="field-hint">{{ form.content.length }}/1000 字</view>
      <button
        class="btn-primary"
        :loading="submitting"
        :disabled="submitting"
        @click="onSubmit"
      >
        提交意见
      </button>
    </view>

    <!-- AI 分类 + 词云 -->
    <view class="card" v-if="words.length > 0">
      <view class="card-title"
        ><app-icon name="cloud" :size="32" /> 关键词词云</view
      >
      <view class="word-cloud">
        <view
          v-for="w in words"
          :key="w.word"
          :class="['wc-tag', wordSizeCls(w.weight)]"
          @click="filterKw = w.word"
          >{{ w.word }} <text class="word-count">{{ w.count }}</text></view
        >
      </view>
    </view>

    <bottom-sheet v-model="showReport" title="意见征集报告">
      <view class="report-copy">{{ reportContent }}</view>
      <button class="btn-primary" @click="copyReport">复制报告</button>
    </bottom-sheet>

    <!-- 工具按钮 -->
    <view class="card">
      <view class="card-title"><app-icon name="wrench" :size="32" /> 工具</view>
      <view class="tool-grid">
        <view class="tool-btn" @click="onClassify">
          <view class="ti" style="background: #1e5a96"
            ><app-icon name="file-pen-line" :size="32" tone="white"
          /></view>
          <text>AI 自动归类</text>
        </view>
        <view class="tool-btn" @click="onDedup">
          <view class="ti" style="background: #8e44ad"
            ><app-icon name="copy" :size="32" tone="white"
          /></view>
          <text>智能去重</text>
        </view>
        <view
          class="tool-btn"
          @click="onReport"
          :class="{ busy: reportLoading }"
        >
          <view class="ti" style="background: #39734c"
            ><app-icon name="clipboard-list" :size="32" tone="white"
          /></view>
          <text>生成报告</text>
        </view>
        <view class="tool-btn" @click="onLoadWords">
          <view class="ti" style="background: #9b621f"
            ><app-icon name="cloud" :size="32" tone="white"
          /></view>
          <text>刷新词云</text>
        </view>
      </view>
    </view>

    <!-- 意见列表 -->
    <view class="card">
      <view class="card-title"
        ><app-icon name="messages-square" :size="32" /> 意见列表</view
      >
      <view v-if="filterKw" class="kw-current">
        筛选关键词：<text class="text-primary fw-600">{{ filterKw }}</text>
        <text class="text-secondary ml-12" @click="filterKw = ''">[清除]</text>
      </view>
      <view v-for="o in visibleOpinions" :key="o.id" class="op-row">
        <view class="op-head">
          <view
            :class="[
              'stance-dot',
              `dot-${(o.viewpoint || 'neutral').toLowerCase()}`,
            ]"
          ></view>
          <text class="op-name">{{ o.submitterName || "匿名" }}</text>
          <text class="text-secondary fz-12 ml-12">{{
            relativeTime(o.submittedAt)
          }}</text>
        </view>
        <view class="op-content">{{ o.content }}</view>
        <view class="op-meta" v-if="o.classifiedCategory">
          分类：<text class="text-primary fw-600">{{
            o.classifiedCategory
          }}</text>
        </view>
      </view>

      <view class="field-hint"
        >已加载 {{ opinions.length }} / {{ opinionTotal }} 条意见</view
      >
      <data-state v-if="opinionError" :error="opinionError" @retry="reload" />
      <button
        v-if="opinions.length < opinionTotal"
        class="load-more"
        :loading="loadingMore"
        :disabled="loadingMore"
        @click="loadMoreOpinions"
      >
        加载更多意见
      </button>
      <loading-block v-if="loading" text="加载中…" />
      <empty
        v-if="!loading && !opinionError && visibleOpinions.length === 0"
        :text="filterKw ? '已加载意见中没有匹配结果' : '暂无意见'"
        desc="可加载更多意见，或清除关键词查看全部"
      />
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
import BottomSheet from "@/components/BottomSheet.vue";
import { normalizeWords, reportText } from "@/utils/consultation-view.js";
import DataState from "@/components/DataState.vue";
import Empty from "@/components/Empty.vue";
import LoadingBlock from "@/components/LoadingBlock.vue";
import {
  CONSULTATION_STATUS,
  OPINION_STANCE,
  OPINION_STATUS,
  relativeTime,
  showLoading,
  hideLoading,
} from "@/utils/index.js";
import { consultationApi } from "@/api/index.js";

export default {
  components: { BottomSheet, DataState, Empty, LoadingBlock },
  data() {
    return {
      id: null,
      detail: null,
      opinions: [],
      submitting: false,
      error: "",
      loading: true,
      stat: { total: 0, deduped: 0, support: 0, oppose: 0 },
      words: [],
      filterKw: "",
      opinionPage: 1,
      opinionTotal: 0,
      loadingMore: false,
      opinionError: "",
      showReport: false,
      reportContent: "",
      reportLoading: false,
      stanceOpts: [
        { value: "SUPPORT", label: "支持" },
        { value: "OPPOSE", label: "反对" },
        { value: "NEUTRAL", label: "中立" },
      ],
      form: {
        submitterName: "",
        submitterContact: "",
        viewpoint: "SUPPORT",
        content: "",
      },
    };
  },
  computed: {
    loadedStances() {
      return {
        support: this.opinions.filter((o) => o.viewpoint === "SUPPORT").length,
        oppose: this.opinions.filter((o) => o.viewpoint === "OPPOSE").length,
        neutral: this.opinions.filter((o) => o.viewpoint === "NEUTRAL").length,
      };
    },
    visibleOpinions() {
      return this.opinions.filter(
        (o) => !this.filterKw || (o.content || "").includes(this.filterKw),
      );
    },
  },
  onLoad(opts) {
    this.id = Number(opts.id) || null;
    this.form.submitterName = uni.getStorageSync("userName") || "";
  },
  onShow() {
    this.reload();
  },
  methods: {
    statusLabel(s) {
      return (CONSULTATION_STATUS[s] || { label: s }).label;
    },
    relativeTime,

    wordSizeCls(weight) {
      if (weight > 0.7) return "wc-lg";
      if (weight > 0.4) return "wc-md";
      return "wc-sm";
    },

    async reload() {
      this.error = "";
      this.loading = true;
      this.opinionError = "";
      this.opinionPage = 1;
      try {
        const [detail, opList, kwords] = await Promise.allSettled([
          consultationApi.consultationDetail(this.id),
          consultationApi.listOpinions(this.id),
          consultationApi.wordCloud(this.id, 30),
        ]);
        if (detail.status === "rejected") throw detail.reason;
        if (detail.status === "fulfilled") this.detail = detail.value;
        if (opList.status === "fulfilled") {
          this.opinions = Array.isArray(opList.value)
            ? opList.value
            : opList.value?.records || [];
          this.opinionTotal = Number(
            opList.value?.total ?? this.opinions.length,
          );
          // 计算支持/反对统计
          this.stat.support = this.opinions.filter(
            (o) => o.viewpoint === "SUPPORT",
          ).length;
          this.stat.oppose = this.opinions.filter(
            (o) => o.viewpoint === "OPPOSE",
          ).length;
          this.stat.total = this.opinions.length;
        }
        if (opList.status === "rejected")
          this.opinionError = "意见列表加载失败，请重试";
        if (kwords.status === "fulfilled") {
          this.words = normalizeWords(kwords.value);
          // 计算去重数（仅当有 dedupSourceId 时）
          if (this.opinions.length) {
            this.stat.deduped = this.opinions.filter(
              (o) => !o.dedupSourceId,
            ).length;
          }
        }
      } catch (e) {
        this.error = e.message || "加载失败，请重试";
      } finally {
        this.loading = false;
      }
    },

    async onSubmit() {
      if (this.submitting) return;
      if (!this.form.content.trim()) {
        uni.showToast({ title: "意见内容不能为空", icon: "none" });
        return;
      }
      this.submitting = true;
      showLoading();
      try {
        await consultationApi.submitOpinion(this.id, {
          submitterName: this.form.submitterName,
          submitterContact: this.form.submitterContact,
          viewpoint: this.form.viewpoint,
          content: this.form.content,
        });
        uni.showToast({ title: "提交成功", icon: "success" });
        this.form.content = "";
        this.reload();
      } catch (e) {
        /* keep form contents on failure */
      } finally {
        this.submitting = false;
        hideLoading();
      }
    },

    async onClassify() {
      showLoading("AI 正在分类…");
      try {
        await consultationApi.classify(this.id);
        uni.showToast({ title: "归类完成", icon: "success" });
        this.reload();
      } finally {
        hideLoading();
      }
    },

    async onDedup() {
      showLoading("智能去重中…");
      try {
        await consultationApi.dedup(this.id);
        uni.showToast({ title: "去重完成", icon: "success" });
        this.reload();
      } finally {
        hideLoading();
      }
    },

    async loadMoreOpinions() {
      if (this.loadingMore || this.loading) return;
      this.loadingMore = true;
      try {
        const result = await consultationApi.listOpinions(
          this.id,
          this.opinionPage + 1,
        );
        const rows = Array.isArray(result) ? result : result?.records || [];
        const ids = new Set(this.opinions.map((item) => item.id));
        this.opinions.push(...rows.filter((item) => !ids.has(item.id)));
        this.opinionPage++;
        this.opinionTotal = Number(result?.total ?? this.opinions.length);
      } catch (e) {
        uni.showToast({ title: "加载失败，请再次尝试", icon: "none" });
      } finally {
        this.loadingMore = false;
      }
    },
    async onReport() {
      if (this.reportLoading) return;
      this.reportLoading = true;
      try {
        this.reportContent = reportText(await consultationApi.report(this.id));
        this.showReport = true;
      } catch (e) {
        /* request displays the failure */
      } finally {
        this.reportLoading = false;
      }
    },
    copyReport() {
      uni.setClipboardData({ data: this.reportContent });
    },

    async onLoadWords() {
      try {
        const k = await consultationApi.wordCloud(this.id, 30);
        this.words = normalizeWords(k);
        uni.showToast({ title: "词云已更新", icon: "none" });
      } catch (e) {
        /* */
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.consultation-detail {
  padding: 24rpx 24rpx 240rpx;
}

.hero-card {
}
.row1 {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}
.title {
  font-size: 32rpx;
  font-weight: 700;
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
  font-size: 26rpx;
  color: #6b7280;
  line-height: 1.6;
  margin: 16rpx 0;
}
.meta {
  display: flex;
  gap: 8rpx;
  font-size: 24rpx;
  margin-top: 8rpx;
}
.meta-key {
  color: #64748b;
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
  background: #17a2b8;
  margin-right: 12rpx;
  border-radius: 4rpx;
}

.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
}
.stat-cell {
  text-align: center;
  padding: 16rpx 0;
}
.stat-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #1f2937;
}
.stat-label {
  font-size: 22rpx;
  color: #6b7280;
  margin-top: 4rpx;
}

// 表单
.form-row {
  margin-bottom: 24rpx;
}
.form-key {
  font-size: 26rpx;
  color: #1f2937;
  display: block;
  margin-bottom: 8rpx;
}
.input {
  width: 100%;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}
.textarea {
  width: 100%;
  min-height: 240rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}

.stance-row {
  display: flex;
  gap: 12rpx;
}
.stance {
  flex: 1;
  padding: 16rpx 0;
  text-align: center;
  background: #f5f7fa;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #6b7280;
}
.stance-support {
  color: #39734c;
}
.stance-oppose {
  color: #a44342;
}
.stance-neutral {
  color: #64748b;
}
.stance-active.stance-support {
  background: #e8f7e6;
  color: #39734c;
  font-weight: 600;
}
.stance-active.stance-oppose {
  background: #fef0f0;
  color: #a44342;
  font-weight: 600;
}
.stance-active.stance-neutral {
  background: #f4f4f5;
  color: #1f2937;
  font-weight: 600;
}

.btn-primary {
  background: #235a91;
  color: #fff;
  border-radius: 16rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
  width: 100%;
  margin-top: 8rpx;
}
.btn-primary::after {
  border: none;
}

// 词云
.word-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  padding: 8rpx 0;
}
.wc-tag {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  background: #ecf5fc;
  color: #235a91;
  display: inline-block;
}
.wc-sm {
  font-size: 22rpx;
  opacity: 1;
}
.wc-md {
  font-size: 28rpx;
  font-weight: 500;
}
.wc-lg {
  font-size: 36rpx;
  font-weight: 700;
  background: #cef5f1;
}

.kw-current {
  font-size: 24rpx;
  margin-bottom: 12rpx;
  padding: 12rpx 16rpx;
  background: #f9fafb;
  border-radius: 8rpx;
}
.text-primary {
  color: #1e5a96;
}
.text-secondary {
  color: #64748b;
}
.fw-600 {
  font-weight: 600;
}
.ml-12 {
  margin-left: 12rpx;
}
.fz-12 {
  font-size: 24rpx;
}

// 工具按钮
.tool-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}
.tool-btn {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #1f2937;
}
.ti {
  width: 56rpx;
  height: 56rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
  flex-shrink: 0;
}

// 意见
.op-row {
  padding: 16rpx 8rpx;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child {
    border-bottom: none;
  }
}
.op-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.stance-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-support {
  background: #39734c;
}
.dot-oppose {
  background: #a44342;
}
.dot-neutral {
  background: #64748b;
}
.op-name {
  font-size: 26rpx;
  color: #1f2937;
  font-weight: 500;
}
.op-content {
  font-size: 26rpx;
  color: #1f2937;
  margin: 8rpx 0;
  line-height: 1.6;
}
.op-meta {
  font-size: 22rpx;
  color: #6b7280;
}
.report-copy {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 28rpx;
  color: #23364d;
  padding-bottom: 24rpx;
}
.word-count {
  font-size: 22rpx;
  margin-left: 8rpx;
}
.load-more {
  background: #edf3fa;
  color: #235a91;
  font-size: 26rpx;
  margin-top: 20rpx;
}
.busy {
  opacity: 0.6;
  pointer-events: none;
}
</style>

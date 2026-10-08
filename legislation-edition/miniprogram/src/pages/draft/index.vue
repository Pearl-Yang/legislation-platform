<template>
  <view class="draft-page">
    <page-heading
      title="草案生成"
      description="选择项目，查看草案版本与起草进度"
      icon="file-pen-line"
    />

    <!-- 项目切换 -->
    <filter-pills
      v-if="projectOpts.length > 0"
      :options="projectOpts"
      v-model="filter.projectId"
    />

    <!-- 操作栏 -->
    <view class="toolbar">
      <button class="btn-primary" @click="onGenerate">
        <app-icon name="file-pen-line" :size="32" tone="white" /> 一键生成草案
      </button>
    </view>

    <!-- 草案列表 -->
    <view class="list">
      <view v-for="d in drafts" :key="d.id" class="card" @click="openDraft(d)">
        <view class="row1">
          <view class="title">
            <text class="ver">v{{ d.version }}</text>
            <text class="name">{{
              d.projectName || "项目 #" + d.projectId
            }}</text>
          </view>
          <view
            :class="[
              'gen-tag',
              d.generationType === 'AUTO_GENERATED' ? 'tag-auto' : 'tag-manual',
            ]"
          >
            {{ d.generationType === "AUTO_GENERATED" ? "AI 生成" : "人工起草" }}
          </view>
        </view>
        <view class="excerpt">{{ excerpt(d.draftContent) }}</view>
        <view class="meta">
          <text class="text-secondary fz-12">{{
            relativeTime(d.updatedAt || d.createdAt)
          }}</text>
          <text class="text-secondary fz-12 ml-12" v-if="d.sourceRegulationId"
            >参考上位法 #{{ d.sourceRegulationId }}</text
          >
        </view>
      </view>

      <data-state :loading="loading" :error="error" @retry="reload" />
      <empty
        v-if="!loading && !error && drafts.length === 0"
        text="暂无草案"
        desc="选择项目后点击「一键生成」"
      />
    </view>

    <!-- 生成弹层 -->
    <view
      v-if="showGenModal"
      class="modal-mask"
      @click.self="showGenModal = false"
    >
      <view class="modal">
        <view class="modal-title"> AI 生成草案</view>
        <view class="modal-body">
          <view class="form-row">
            <text class="form-key">关联项目</text>
            <picker
              mode="selector"
              :range="projectOpts"
              range-key="label"
              :value="projectIdx"
              @change="onProjectChange"
            >
              <view class="picker">
                {{ projectOpts[projectIdx]?.label || "请选择" }}
                <text class="picker-arrow">▾</text>
              </view>
            </picker>
          </view>
          <view class="form-row">
            <text class="form-key">生成提示词（可选）</text>
            <textarea
              v-model="genPrompt"
              class="textarea"
              placeholder="例如：根据上位法第三条制定本市网络数据安全管理办法，强调个人信息保护"
              maxlength="500"
            />
          </view>
        </view>
        <view class="modal-actions">
          <view class="modal-btn cancel" @click="showGenModal = false"
            >取消</view
          >
          <view class="modal-btn confirm" @click="confirmGenerate"
            >开始生成</view
          >
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import PageHeading from "@/components/PageHeading.vue";
import DataState from "@/components/DataState.vue";
import FilterPills from "@/components/FilterPills.vue";
import Empty from "@/components/Empty.vue";
import LoadingBlock from "@/components/LoadingBlock.vue";
import {
  relativeTime,
  ellipsis,
  showLoading,
  hideLoading,
} from "@/utils/index.js";
import { draftApi, projectApi } from "@/api/index.js";

export default {
  components: { PageHeading, DataState, FilterPills, Empty, LoadingBlock },
  data() {
    return {
      filter: { projectId: "" },
      projectIdx: 0,
      drafts: [],
      requestVersion: 0,
      error: "",
      loading: false,
      projectOpts: [], // [{value,label}]
      showGenModal: false,
      genPrompt: "",
    };
  },
  onLoad(opts) {
    if (opts && opts.projectId) {
      this.filter.projectId = Number(opts.projectId);
    }
  },
  onShow() {
    this.loadProjects().then(() => this.reload());
  },
  watch: {
    "filter.projectId"() {
      this.reload();
    },
  },
  methods: {
    relativeTime,
    ellipsis,
    excerpt(text) {
      return ellipsis(text || "（无正文）", 100);
    },

    async loadProjects() {
      try {
        // 取前 50 条项目作为下拉
        const resp = await projectApi.listProjects({ page: 1, size: 50 });
        const arr = (resp && (resp.records || resp.list || resp)) || [];
        this.projectOpts = arr.map((p) => ({
          value: p.id,
          label: p.projectName,
        }));
        if (!this.filter.projectId && this.projectOpts.length > 0) {
          this.filter.projectId = this.projectOpts[0].value;
          this.projectIdx = 0;
        } else {
          this.projectIdx = Math.max(
            0,
            this.projectOpts.findIndex(
              (o) => o.value === this.filter.projectId,
            ),
          );
        }
      } catch (e) {
        this.projectOpts = [];
        this.error = e.message || "项目加载失败，请重试";
      }
    },

    async reload() {
      const version = ++this.requestVersion;
      if (!this.filter.projectId) {
        this.drafts = [];
        return;
      }
      this.loading = true;
      this.error = "";
      try {
        const list = await draftApi.listDrafts(this.filter.projectId);
        if (version !== this.requestVersion) return;
        this.drafts = Array.isArray(list) ? list : [];
      } catch (e) {
        if (version !== this.requestVersion) return;
        this.error = e.message || "加载失败，请重试";
        this.drafts = [];
      } finally {
        if (version === this.requestVersion) this.loading = false;
      }
    },

    openDraft(d) {
      uni.navigateTo({ url: `/pages/draft/detail?id=${d.id}` });
    },

    onProjectChange(e) {
      const idx = Number(e.detail.value);
      this.projectIdx = idx;
      this.filter.projectId = this.projectOpts[idx].value;
    },

    onGenerate() {
      if (!this.filter.projectId) {
        uni.showToast({ title: "请先选择项目", icon: "none" });
        return;
      }
      this.genPrompt = "";
      this.showGenModal = true;
    },

    async confirmGenerate() {
      if (!this.filter.projectId) return;
      this.showGenModal = false;
      showLoading("AI 正在生成…");
      try {
        const taskId = await draftApi.submitGenerate({
          projectId: this.filter.projectId,
          prompt: this.genPrompt,
          createdBy: uni.getStorageSync("userId") || 1,
        });
        hideLoading();
        uni.showToast({ title: "生成任务已提交", icon: "success" });
        // 简单轮询 3 次
        this.pollTask(taskId);
      } catch (e) {
        hideLoading();
      }
    },

    async pollTask(taskId, times = 5) {
      if (times <= 0) return;
      await new Promise((r) => setTimeout(r, 1500));
      try {
        const t = await draftApi.taskStatus(taskId);
        if (t && t.status === "SUCCESS") {
          uni.showToast({ title: "生成成功", icon: "success" });
          this.reload();
          return;
        } else if (t && t.status === "FAILED") {
          uni.showToast({
            title: "生成失败：" + (t.message || ""),
            icon: "none",
          });
          return;
        }
      } catch (e) {
        /* 忽略 */
      }
      return this.pollTask(taskId, times - 1);
    },
  },
};
</script>

<style lang="scss" scoped>
.draft-page {
  padding-bottom: 240rpx;
}

.hero {
  background: linear-gradient(120deg, #8e44ad, #b07cc6);
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

.toolbar {
  padding: 16rpx 24rpx;
}
.btn-primary {
  background: linear-gradient(90deg, #1e5a96, #4a86c5);
  color: #fff;
  border-radius: 16rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
}
.btn-primary::after {
  border: none;
}

.list {
  padding: 0 24rpx 32rpx;
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
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.title {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
  min-width: 0;
}
.ver {
  flex-shrink: 0;
  background: #1e5a96;
  color: #fff;
  font-size: 22rpx;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}
.name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1f2937;
}
.gen-tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}
.tag-auto {
  background: #f5e9fa;
  color: #8e44ad;
}
.tag-manual {
  background: #ecf5fc;
  color: #1e5a96;
}

.excerpt {
  font-size: 24rpx;
  color: #6b7280;
  margin: 12rpx 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}
.meta {
  display: flex;
  align-items: center;
  gap: 8rpx;
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

// 弹层
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  z-index: 99;
}
.modal {
  width: 100%;
  background: #fff;
  border-top-left-radius: 24rpx;
  border-top-right-radius: 24rpx;
  padding: 32rpx 24rpx;
}
.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}
.modal-body {
  padding: 8rpx 0 24rpx;
}

.form-row {
  margin-bottom: 24rpx;
}
.form-key {
  font-size: 26rpx;
  color: #1f2937;
  margin-bottom: 8rpx;
  display: block;
}
.picker {
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx 24rpx;
  font-size: 26rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #1f2937;
}
.picker-arrow {
  color: #64748b;
}
.textarea {
  width: 100%;
  min-height: 180rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}

.modal-actions {
  display: flex;
  gap: 16rpx;
}
.modal-btn {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  border-radius: 16rpx;
  font-size: 28rpx;
}
.modal-btn.cancel {
  background: #f5f7fa;
  color: #6b7280;
}
.modal-btn.confirm {
  background: linear-gradient(90deg, #1e5a96, #4a86c5);
  color: #fff;
}
</style>

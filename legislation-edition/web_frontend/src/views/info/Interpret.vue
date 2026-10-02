<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">政策解读</span>
      <span class="title-en">Policy Interpretations</span>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="16">
        <el-card v-for="item in items" :key="item.id" class="mb-16">
          <div class="interpret-head">
            <div class="interpret-tag" :style="{ background: item.color }">
              <el-icon><Document /></el-icon>
            </div>
            <div>
              <h3 class="interpret-title">{{ item.title }}</h3>
              <p class="interpret-meta">
                <span><el-icon><User /></el-icon>{{ item.author }}</span>
                <span><el-icon><OfficeBuilding /></el-icon>{{ item.org }}</span>
                <span><el-icon><Calendar /></el-icon>{{ item.publishedAt }}</span>
              </p>
            </div>
          </div>
          <p class="interpret-summary">{{ item.summary }}</p>
          <div class="interpret-actions">
            <el-button type="primary" link>阅读全文</el-button>
            <el-button link>收藏</el-button>
            <el-button link>分享</el-button>
            <el-button link>{{ item.viewCount }} 次阅读</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card>
          <template #header>
            <span class="title">最新解读</span>
          </template>
          <ul class="latest-list">
            <li v-for="l in latest" :key="l.id">
              <div class="ll-date">{{ l.date }}</div>
              <a class="ll-title">{{ l.title }}</a>
            </li>
          </ul>
        </el-card>

        <el-card class="mt-16">
          <template #header>
            <span class="title">权威解读专家</span>
          </template>
          <div v-for="(e, idx) in experts" :key="e.name" class="expert-row">
            <el-avatar :size="42" class="ea">{{ e.name.slice(0, 1) }}</el-avatar>
            <div class="expert-info">
              <div class="expert-name">{{ e.name }} <el-tag size="small" type="info">{{ e.title }}</el-tag></div>
              <div class="expert-org text-secondary">{{ e.org }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Document, User, OfficeBuilding, Calendar } from '@element-plus/icons-vue'

const items = ref([
  { id: 1, title: '《网络数据安全管理条例》要点解读：从立法本意到落地实施', summary: '本文由中国法学会副会长深度解读《网络数据安全管理条例》的立法背景、核心条款和实施要点，重点分析数据分类分级、跨境流动、平台责任等关键制度设计。', author: '王教授', org: '中国法学会', publishedAt: '2026-10-02', viewCount: 2356, color: 'linear-gradient(135deg,#4f46e5,#6366f1)' },
  { id: 2, title: '《数据安全法》与《网络安全法》的衔接适用', summary: '本文分析《数据安全法》《网络安全法》《个人信息保护法》三部上位法的衔接关系，以及下位法如何配套。', author: '李博士',   org: '中国信息安全测评中心', publishedAt: '2026-09-28', viewCount: 1824, color: 'linear-gradient(135deg,#6366f1,#10b981)' },
  { id: 3, title: '行政处罚类规章的修订要点与基层执法衔接', summary: '针对 2025 年集中清理的行政处罚类规章，分析修订要点与基层执法的衔接建议。', author: '张律师',   org: '司法部', publishedAt: '2026-09-15', viewCount: 1421, color: 'linear-gradient(135deg,#f59e0b,#f43f5e)' },
  { id: 4, title: '行政许可事项清单管理改革的制度逻辑', summary: '本文解读"放管服"改革下的行政许可事项清单管理，包括设定依据、实施机关、办理流程等关键要素。', author: '陈研究',  org: '国务院发展研究中心', publishedAt: '2026-09-08', viewCount: 1152, color: 'linear-gradient(135deg,#06b6d4,#6366f1)' }
])

const latest = ref([
  { id: 1, date: '2026-10-02', title: '《网络数据安全管理条例》要点解读' },
  { id: 2, date: '2026-09-28', title: '《数据安全法》与《网络安全法》的衔接' },
  { id: 3, date: '2026-09-22', title: '行政处罚法修订对基层执法的影响' },
  { id: 4, date: '2026-09-15', title: '行政许可事项清单管理改革解读' },
  { id: 5, date: '2026-09-08', title: '《公平竞争审查制度实施细则》解读' }
])

const experts = ref([
  { name: '王教授', title: '中国法学会副会长', org: '中国法学会' },
  { name: '李博士', title: '信息安全专家',  org: '中国信息安全测评中心' },
  { name: '张律师', title: '高级律师',     org: '司法部' },
  { name: '陈研究', title: '研究员',       org: '国务院发展研究中心' }
])
</script>

<style lang="scss" scoped>
.title-en { font-size: 13px; color: $text-secondary; margin-left: 12px; }

.interpret-head { display: flex; gap: 12px; }
.interpret-tag {
  width: 56px; height: 56px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  font-size: 26px;
  flex-shrink: 0;
}
.interpret-title { font-size: 17px; font-weight: 600; line-height: 1.4; }
.interpret-meta { display: flex; gap: 16px; font-size: 12px; color: $text-secondary; margin-top: 4px; }
.interpret-meta span { display: inline-flex; align-items: center; gap: 4px; }
.interpret-summary { font-size: 13px; color: $text-regular; margin: 12px 0; line-height: 1.7; }
.interpret-actions { display: flex; gap: 16px; padding-top: 8px; border-top: 1px dashed $border-light; font-size: 13px; }

.mb-16 { margin-bottom: 16px; }
.mt-16 { margin-top: 16px; }

.latest-list { list-style: none; padding: 0; margin: 0; }
.latest-list li { padding: 8px 0; border-bottom: 1px dashed $border-light; }
.ll-date { font-size: 12px; color: $text-secondary; }
.ll-title { font-size: 13px; color: $text-primary; cursor: pointer; &:hover { color: $primary-color; } }

.expert-row { display: flex; gap: 10px; align-items: center; padding: 10px 0; border-bottom: 1px dashed $border-light; }
.expert-row:last-child { border-bottom: none; }
.ea { background: linear-gradient(135deg, $primary-light, $primary-color); color: #fff; font-weight: 600; }
.expert-name { font-size: 13px; font-weight: 500; display: flex; gap: 6px; align-items: center; }
.expert-org { font-size: 12px; }
</style>
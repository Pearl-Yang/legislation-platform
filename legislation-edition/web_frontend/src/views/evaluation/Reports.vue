<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">评估报告归档</span>
      <span class="subtitle">所有历史评估报告，支持在线预览与下载</span>
    </div>

    <el-row :gutter="16">
      <el-col v-for="r in reports" :key="r.id" :xs="24" :sm="12" :md="8">
        <el-card class="report-card" shadow="hover">
          <div class="report-cover" :style="{ background: r.cover }">
            <div class="cover-title">评估报告</div>
            <div class="cover-year">{{ r.periodLabel }}</div>
          </div>
          <div class="report-content">
            <h3 class="report-name">{{ r.regulationName }}</h3>
            <div class="report-meta">
              <span><el-icon><Calendar /></el-icon>{{ r.createdAt }}</span>
              <span><el-icon><User /></el-icon>{{ r.createdBy }}</span>
            </div>
            <div class="report-score">
              <div class="score-circle" :style="{ '--c': scoreColor(r.score) }">
                <span class="score-value">{{ r.score }}</span>
                <span class="score-unit">分</span>
              </div>
              <ul class="score-list">
                <li><span class="dot" style="background:#4f46e5"></span>合法性 {{ r.legality }}</li>
                <li><span class="dot" style="background:#6366f1"></span>落实性 {{ r.execution }}</li>
                <li><span class="dot" style="background:#10b981"></span>满意度 {{ r.satisfaction }}</li>
              </ul>
            </div>
            <div class="report-actions">
              <el-button :icon="View" size="small" plain @click="onPreview(r)">预览</el-button>
              <el-button :icon="Download" size="small" type="primary">下载</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, User, View, Download } from '@element-plus/icons-vue'

const reports = ref([
  { id: 1, regulationName: '中华人民共和国数据安全法', periodLabel: '2025 全年', score: 92, legality: 96, execution: 88, satisfaction: 91, createdAt: '2026-01-15', createdBy: '系统', cover: 'linear-gradient(135deg,#4f46e5,#6366f1)' },
  { id: 2, regulationName: '某省数据交易管理办法',     periodLabel: '2025H1',   score: 81, legality: 88, execution: 78, satisfaction: 76, createdAt: '2025-08-20', createdBy: '张三', cover: 'linear-gradient(135deg,#6366f1,#10b981)' },
  { id: 3, regulationName: '某市网络数据管理细则',     periodLabel: '2025H1',   score: 68, legality: 75, execution: 65, satisfaction: 64, createdAt: '2025-09-12', createdBy: '李四', cover: 'linear-gradient(135deg,#f59e0b,#f43f5e)' },
  { id: 4, regulationName: 'XX 部门信息安全规范',     periodLabel: '2024 全年', score: 84, legality: 90, execution: 80, satisfaction: 82, createdAt: '2025-01-08', createdBy: '系统', cover: 'linear-gradient(135deg,#8b5cf6,#4f46e5)' },
  { id: 5, regulationName: '某省医疗保障办法',         periodLabel: '2024H1',   score: 76, legality: 82, execution: 72, satisfaction: 74, createdAt: '2024-08-15', createdBy: '王五', cover: 'linear-gradient(135deg,#06b6d4,#6366f1)' },
  { id: 6, regulationName: '某市人才公寓管理办法',     periodLabel: '2024 全年', score: 88, legality: 92, execution: 86, satisfaction: 85, createdAt: '2025-01-20', createdBy: '系统', cover: 'linear-gradient(135deg,#10b981,#4f46e5)' }
])

const scoreColor = (s) => s >= 85 ? '#10b981' : s >= 75 ? '#4f46e5' : s >= 60 ? '#f59e0b' : '#f43f5e'
const onPreview = (r) => ElMessage.info(`预览评估报告：${r.regulationName}`)
</script>

<style lang="scss" scoped>
.report-card { margin-bottom: 16px; overflow: hidden; }
.report-cover {
  height: 110px;
  color: #fff;
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-radius: 6px 6px 0 0;
  margin: -20px -20px 12px;
  width: calc(100% + 40px);
}
.cover-title { font-size: 14px; opacity: 0.9; }
.cover-year  { font-size: 26px; font-weight: 700; }

.report-name { font-size: 16px; font-weight: 600; }
.report-meta { display: flex; gap: 16px; font-size: 12px; color: $text-secondary; margin: 8px 0 12px; }
.report-meta span { display: inline-flex; align-items: center; gap: 4px; }

.report-score { display: flex; align-items: center; gap: 16px; padding: 8px 0; border-top: 1px dashed $border-light; border-bottom: 1px dashed $border-light; margin-bottom: 12px; }
.score-circle {
  width: 64px; height: 64px; border-radius: 50%;
  border: 4px solid var(--c);
  display: flex; align-items: baseline; justify-content: center;
  color: var(--c);
  .score-value { font-size: 22px; font-weight: 700; }
  .score-unit  { font-size: 12px; margin-left: 1px; }
}
.score-list { list-style: none; padding: 0; margin: 0; flex: 1; }
.score-list li { display: flex; align-items: center; gap: 6px; padding: 2px 0; font-size: 13px; }
.score-list .dot { width: 8px; height: 8px; border-radius: 50%; }

.report-actions { display: flex; gap: 8px; }
</style>
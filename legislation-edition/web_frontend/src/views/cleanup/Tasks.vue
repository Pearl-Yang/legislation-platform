<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">清理任务</span>
      <span class="subtitle">日常 / 定期 / 专项 三种触发模式，AI 给出保留 / 修改 / 废止建议</span>
      <el-button type="primary" :icon="Plus" class="ml-auto" @click="createVisible = true">创建清理任务</el-button>
    </div>

    <el-row :gutter="12" class="stat-row">
      <el-col :xs="12" :sm="6"><div class="stat-card stat-blue">
        <div class="icon-wrap"><el-icon><List /></el-icon></div>
        <div class="stat-content"><div class="stat-label">任务总数</div><div class="stat-value">{{ stat.total }}</div></div>
      </div></el-col>
      <el-col :xs="12" :sm="6"><div class="stat-card stat-orange">
        <div class="icon-wrap"><el-icon><Loading /></el-icon></div>
        <div class="stat-content"><div class="stat-label">进行中</div><div class="stat-value">{{ stat.running }}</div></div>
      </div></el-col>
      <el-col :xs="12" :sm="6"><div class="stat-card stat-red">
        <div class="icon-wrap"><el-icon><Warning /></el-icon></div>
        <div class="stat-content"><div class="stat-label">建议废止</div><div class="stat-value">{{ stat.obsoleteSuggest }}</div></div>
      </div></el-col>
      <el-col :xs="12" :sm="6"><div class="stat-card stat-green">
        <div class="icon-wrap"><el-icon><Select /></el-icon></div>
        <div class="stat-content"><div class="stat-label">已完成</div><div class="stat-value">{{ stat.done }}</div></div>
      </div></el-col>
    </el-row>

    <el-card class="mt-16">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="全部"        name="all" />
        <el-tab-pane label="日常清理"    name="DAILY" />
        <el-tab-pane label="定期清理"    name="PERIODIC" />
        <el-tab-pane label="专项清理"    name="THEMATIC" />
      </el-tabs>

      <el-table :data="filteredTasks" stripe>
        <el-table-column label="任务名称" min-width="240" prop="taskName">
          <template #default="{ row }">
            <a @click="openTask(row)">{{ row.taskName }}</a>
            <el-tag v-if="row.theme" size="small" type="warning" effect="plain" class="ml-8">主题：{{ row.theme }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="任务类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.taskType === 'DAILY' ? 'info' : row.taskType === 'PERIODIC' ? 'primary' : 'warning'">
              {{ cleanupTypeLabel(row.taskType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="触发来源" width="140" prop="triggerSource" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已建议 / 候选" width="120" align="center">
          <template #default="{ row }">
            {{ row.suggested }} / {{ row.candidates }}
          </template>
        </el-table-column>
        <el-table-column label="责任部门" prop="responsibleDept" width="120" />
        <el-table-column label="创建时间" prop="createdAt" width="160" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openTask(row)">详情</el-button>
            <el-button link size="small" @click="onSuggest(row)" v-if="['PENDING','RUNNING'].includes(row.status)">AI 生成建议</el-button>
            <el-button link size="small" @click="onExport(row)">导出报告</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="`清理任务 #${active?.id} - ${active?.taskName}`" size="80%">
      <div v-if="active" class="task-detail">
        <el-descriptions :column="3" border class="mb-16">
          <el-descriptions-item label="任务类型">
            <el-tag size="small">{{ cleanupTypeLabel(active.taskType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="清理模式">
            <el-tag size="small" :type="active.cleanupMode === 'AUTO' ? 'primary' : active.cleanupMode === 'MANUAL_REVIEW' ? 'warning' : 'success'">
              {{ active.cleanupMode === 'AUTO' ? '自动' : active.cleanupMode === 'MANUAL_REVIEW' ? '人工复核' : '混合' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(active.status)" size="small">{{ statusLabel(active.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="触发来源" :span="2">{{ active.triggerSource }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ active.createdAt }}</el-descriptions-item>
        </el-descriptions>

        <h3 class="block-title">受影响法规候选（{{ candidates.length }}）</h3>
        <el-table :data="candidates" stripe>
          <el-table-column label="法规名称" min-width="240" prop="regulationName" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <span class="type-badge" :class="projectTypeCls(row.regulationType)">{{ projectTypeLabel(row.regulationType) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="效力状态" width="120">
            <template #default="{ row }">
              <el-tag :type="regStatusTag(row.status)" size="small">{{ regStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="AI 建议" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.aiSuggestion" :type="suggestionTag(row.aiSuggestion)" size="small">
                {{ suggestionLabel(row.aiSuggestion) }}
              </el-tag>
              <span v-else class="text-secondary">未生成</span>
            </template>
          </el-table-column>
          <el-table-column label="置信度" width="100">
            <template #default="{ row }">
              <el-progress v-if="row.aiSuggestion" :percentage="(row.confidence || 0) * 100" :show-text="false" :stroke-width="6" />
              <span v-else class="text-secondary">—</span>
            </template>
          </el-table-column>
          <el-table-column label="最终决定" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.finalDecision" :type="suggestionTag(row.finalDecision)" size="small">{{ suggestionLabel(row.finalDecision) }}</el-tag>
              <span v-else class="text-warning">待决定</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="onDecide(row, 'KEEP')">保留</el-button>
              <el-button link type="warning" size="small" @click="onDecide(row, 'MODIFY')">修改</el-button>
              <el-button link type="danger"  size="small" @click="onDecide(row, 'OBSOLETE')">废止</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- 创建对话框 -->
    <el-dialog v-model="createVisible" title="创建清理任务" width="540px">
      <el-form label-width="100px">
        <el-form-item label="任务名称" required>
          <el-input placeholder="例：2026Q4 规章集中清理" />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-radio-group>
            <el-radio-button value="DAILY">日常</el-radio-button>
            <el-radio-button value="PERIODIC">定期</el-radio-button>
            <el-radio-button value="THEMATIC">专项</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="清理模式">
          <el-radio-group>
            <el-radio-button value="AUTO">自动</el-radio-button>
            <el-radio-button value="MANUAL_REVIEW">人工复核</el-radio-button>
            <el-radio-button value="HYBRID">混合</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="责任部门">
          <el-input placeholder="例：政策法规司" />
        </el-form-item>
        <el-form-item v-if="true" label="主题（专项）">
          <el-input placeholder="专项清理的关键字，如「数据安全」" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="createVisible = false">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, List, Select, Warning, Loading } from '@element-plus/icons-vue'
import { listCleanupTasks, affectedRegulations, suggestCleanup, decideSuggestion, getCleanupReport } from '@/api/legislation'
import {
  cleanupTypeLabel, projectTypeLabel, projectTypeCls,
  regStatusLabel, regStatusTag, suggestionLabel, suggestionTag
} from '@/utils/dict'

const activeTab = ref('all')
const detailVisible = ref(false)
const createVisible = ref(false)
const active = ref(null)

const tasks_ = ref([
  { id: 1, taskName: '2026Q4 规章集中清理', taskType: 'PERIODIC', status: 'RUNNING', triggerSource: '定期计划', candidates: 124, suggested: 124, responsibleDept: '政策法规司', createdAt: '2026-10-01 09:00', cleanupMode: 'HYBRID' },
  { id: 2, taskName: '《数据安全法》修订引发联动清理', taskType: 'DAILY', status: 'PENDING', triggerSource: '上位法《数据安全法》修订', candidates: 18, suggested: 0, responsibleDept: '网信办', createdAt: '2026-09-28 14:12', cleanupMode: 'AUTO' },
  { id: 3, taskName: '专项：行政处罚类规章专项清理', taskType: 'THEMATIC', status: 'DONE', triggerSource: '手动发起', theme: '行政处罚', candidates: 56, suggested: 56, responsibleDept: '司法局', createdAt: '2026-08-15 10:30', cleanupMode: 'MANUAL_REVIEW' },
  { id: 4, taskName: '2026Q3 规章集中清理', taskType: 'PERIODIC', status: 'DONE', triggerSource: '定期计划', candidates: 102, suggested: 102, responsibleDept: '政策法规司', createdAt: '2026-07-01 09:00', cleanupMode: 'HYBRID' }
])

const stat = computed(() => ({
  total: tasks_.value.length,
  running: tasks_.value.filter(t => t.status === 'RUNNING').length,
  done: tasks_.value.filter(t => t.status === 'DONE').length,
  obsoleteSuggest: 24
}))

const filteredTasks = computed(() =>
  activeTab.value === 'all' ? tasks_.value : tasks_.value.filter(t => t.taskType === activeTab.value)
)

const statusLabel = (s) => ({ PENDING: '待执行', RUNNING: '进行中', DONE: '已完成' }[s] || s)
const statusTag   = (s) => ({ PENDING: 'info', RUNNING: 'warning', DONE: 'success' }[s] || 'info')
const onTabChange = () => {}

const openTask = async (row) => {
  active.value = row
  // 真实场景：candidates = await affectedRegulations(row.id)
  detailVisible.value = true
}

const candidates = ref([
  { id: 11, regulationName: '某省数据安全管理办法', regulationType: 'LOCAL_RULE', status: 'EFFECTIVE', aiSuggestion: 'MODIFY',   confidence: 0.87, finalDecision: null },
  { id: 12, regulationName: '某市网络数据管理细则',   regulationType: 'LOCAL_RULE', status: 'EFFECTIVE', aiSuggestion: 'OBSOLETE', confidence: 0.92, finalDecision: 'OBSOLETE' },
  { id: 13, regulationName: 'XX 部门信息安全规范',     regulationType: 'DEPT_RULE',  status: 'OBSOLETE',  aiSuggestion: 'OBSOLETE', confidence: 0.95, finalDecision: 'OBSOLETE' },
  { id: 14, regulationName: '某行业数据收集指引',      regulationType: 'DEPT_RULE',  status: 'EFFECTIVE', aiSuggestion: 'KEEP',     confidence: 0.81, finalDecision: null },
  { id: 15, regulationName: '某省数据交易管理办法',     regulationType: 'LOCAL_RULE', status: 'REVISING',  aiSuggestion: 'MODIFY',   confidence: 0.78, finalDecision: null }
])

const onSuggest = async (row) => {
  await suggestCleanup(row.id)
  ElMessage.success('AI 建议生成任务已提交')
}

const onExport = async (row) => {
  ElMessage.info(`导出报告：/cleanup/task/${row.id}/report（演示版返回提示）`)
}

const onDecide = async (row, decision) => {
  await decideSuggestion(row.id, { finalDecision: decision })
  row.finalDecision = decision
  ElMessage.success(`已决策：${suggestionLabel(decision)}`)
}
</script>

<style lang="scss" scoped>
.ml-8 { margin-left: 8px; }
.ml-auto { margin-left: auto; }
.stat-row { margin-top: 8px; }
.block-title { font-size: 15px; font-weight: 600; margin: 16px 0 12px; }
.mb-16 { margin-bottom: 16px; }
</style>
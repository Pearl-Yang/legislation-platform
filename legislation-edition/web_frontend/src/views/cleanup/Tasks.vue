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

    <el-card class="mt-16" v-loading="loading">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="全部"        name="all" />
        <el-tab-pane label="日常清理"    name="DAILY" />
        <el-tab-pane label="定期清理"    name="PERIODIC" />
        <el-tab-pane label="专项清理"    name="THEMATIC" />
      </el-tabs>

      <el-table :data="filteredTasks" stripe :empty-text="loading ? '加载中' : '暂无清理任务'">
        <el-table-column label="任务名称" min-width="240">
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
            {{ row.suggested ?? 0 }} / {{ row.candidates ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="责任部门" prop="responsibleDept" width="120" />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openTask(row)">详情</el-button>
            <el-button link size="small" @click="onSuggest(row)" v-if="['PENDING','RUNNING'].includes(row.status)">AI 生成建议</el-button>
            <el-dropdown trigger="click" @command="(fmt) => onExport(row, fmt)">
              <el-button link size="small">导出报告 ▾</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="HTML">HTML</el-dropdown-item>
                  <el-dropdown-item command="MARKDOWN">Markdown</el-dropdown-item>
                  <el-dropdown-item command="DOCX">Word (docx)</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="`清理任务 #${active?.id} - ${active?.taskName}`" size="80%">
      <div v-if="active" class="task-detail" v-loading="candidatesLoading">
        <el-descriptions :column="3" border class="mb-16">
          <el-descriptions-item label="任务类型">
            <el-tag size="small">{{ cleanupTypeLabel(active.taskType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="清理模式">
            <el-tag size="small" :type="active.cleanupMode === 'AUTO' ? 'primary' : active.cleanupMode === 'MANUAL_REVIEW' ? 'warning' : 'success'">
              {{ active.cleanupMode === 'AUTO' ? '自动' : active.cleanupMode === 'MANUAL_REVIEW' ? '人工复核' : active.cleanupMode === 'HYBRID' ? '混合' : '—' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(active.status)" size="small">{{ statusLabel(active.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="触发来源" :span="2">{{ active.triggerSource || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(active.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="active.theme" label="主题关键词" :span="3">
            <el-tag size="small" type="warning">{{ active.theme }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <h3 class="block-title">受影响法规候选（{{ candidates.length }}）</h3>
        <el-table :data="candidates" stripe :empty-text="'暂无可清理候选法规'">
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
              <el-progress v-if="row.aiSuggestion" :percentage="Math.round((row.confidence || 0) * 100)" :show-text="false" :stroke-width="6" />
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
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="任务名称" required>
          <el-input v-model="createForm.taskName" placeholder="例：2026Q4 规章集中清理" />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-radio-group v-model="createForm.taskType">
            <el-radio-button value="DAILY">日常</el-radio-button>
            <el-radio-button value="PERIODIC">定期</el-radio-button>
            <el-radio-button value="THEMATIC">专项</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="清理模式">
          <el-radio-group v-model="createForm.cleanupMode">
            <el-radio-button value="AUTO">自动</el-radio-button>
            <el-radio-button value="MANUAL_REVIEW">人工复核</el-radio-button>
            <el-radio-button value="HYBRID">混合</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="责任部门">
          <el-input v-model="createForm.responsibleDept" placeholder="例：政策法规司" />
        </el-form-item>
        <el-form-item v-if="createForm.taskType === 'THEMATIC'" label="主题（专项）">
          <el-input v-model="createForm.theme" placeholder="专项清理的关键字，如「数据安全」" />
        </el-form-item>
        <el-form-item label="触发来源">
          <el-input v-model="createForm.triggerSource" placeholder="例：定期计划 / 上位法修订触发 / 手动" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { Plus, List, Select, Warning, Loading } from '@element-plus/icons-vue'
import {
  listCleanupTasks, createCleanupTask, getCleanupTask,
  affectedRegulations, suggestCleanup, decideSuggestion, getCleanupReport
} from '@/api/legislation'
import {
  cleanupTypeLabel, projectTypeLabel, projectTypeCls,
  regStatusLabel, regStatusTag, suggestionLabel, suggestionTag
} from '@/utils/dict'
import { formatDate } from '@/utils'

const loading = ref(false)
const creating = ref(false)
const candidatesLoading = ref(false)
const activeTab = ref('all')
const detailVisible = ref(false)
const createVisible = ref(false)
const active = ref(null)
const list = ref([])
const candidates = ref([])

const createForm = ref({
  taskName: '', taskType: 'PERIODIC', cleanupMode: 'HYBRID',
  responsibleDept: '', theme: '', triggerSource: '手动发起'
})

async function loadList () {
  loading.value = true
  try {
    const { data } = await listCleanupTasks()
    list.value = data || []
  } catch (e) {
    ElMessage.error('加载清理任务失败：' + (e.message || ''))
    list.value = []
  } finally {
    loading.value = false
  }
}

const filteredTasks = computed(() =>
  activeTab.value === 'all' ? list.value : list.value.filter(t => t.taskType === activeTab.value)
)

const stat = computed(() => ({
  total:           list.value.length,
  running:         list.value.filter(t => t.status === 'RUNNING').length,
  done:            list.value.filter(t => t.status === 'DONE').length,
  obsoleteSuggest: list.value.reduce((acc, t) => acc + (t.suggested || 0), 0)
}))

const statusLabel = (s) => ({ PENDING: '待执行', RUNNING: '进行中', DONE: '已完成' }[s] || s)
const statusTag   = (s) => ({ PENDING: 'info', RUNNING: 'warning', DONE: 'success' }[s] || 'info')
const onTabChange = () => {}

const openTask = async (row) => {
  active.value = row
  detailVisible.value = true
  candidatesLoading.value = true
  try {
    const { data } = await affectedRegulations(row.id)
    candidates.value = (data || []).map(c => ({
      ...c,
      aiSuggestion: c.aiSuggestion ?? c.suggestion ?? null,
      confidence:   c.confidence ?? null,
      finalDecision:c.finalDecision ?? null
    }))
  } catch (e) {
    ElMessage.error('加载候选法规失败：' + (e.message || ''))
    candidates.value = []
  } finally {
    candidatesLoading.value = false
  }
}

const onSuggest = async (row) => {
  try {
    await suggestCleanup(row.id)
    ElMessage.success('AI 建议生成任务已提交,稍后刷新列表')
    setTimeout(() => { openTask(row); loadList() }, 800)
  } catch (e) {
    ElMessage.error('提交失败：' + (e.message || ''))
  }
}

const onExport = async (row, format = 'HTML') => {
  const ext = format === 'DOCX' ? 'docx' : format === 'MARKDOWN' ? 'md' : 'html'
  const loadingSvc = ElLoading.service({ text: `正在生成 ${format} ...` })
  try {
    const { default: request } = await import('@/utils/request')
    const resp = await request.get(`/cleanup/task/${row.id}/report`, {
      params: { format },
      responseType: 'blob'
    })
    const blob = new Blob([resp.data], { type: resp.headers?.['content-type'] || 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `cleanup-${row.id}.${ext}`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出已开始')
  } catch (e) {
    ElMessage.error('导出失败：' + (e.message || ''))
  } finally {
    loadingSvc.close()
  }
}

const onDecide = async (row, decision) => {
  try {
    await decideSuggestion(row.id, { finalDecision: decision })
    row.finalDecision = decision
    ElMessage.success(`已决策：${suggestionLabel(decision)}`)
  } catch (e) {
    ElMessage.error('决策失败：' + (e.message || ''))
  }
}

const onCreate = async () => {
  if (!createForm.value.taskName) return ElMessage.warning('请填写任务名称')
  creating.value = true
  try {
    await createCleanupTask(createForm.value)
    ElMessage.success('清理任务已创建')
    createVisible.value = false
    createForm.value = {
      taskName: '', taskType: 'PERIODIC', cleanupMode: 'HYBRID',
      responsibleDept: '', theme: '', triggerSource: '手动发起'
    }
    loadList()
  } catch (e) {
    ElMessage.error('创建失败：' + (e.message || ''))
  } finally {
    creating.value = false
  }
}

onMounted(loadList)
</script>

<style lang="scss" scoped>
.ml-8 { margin-left: 8px; }
.ml-auto { margin-left: auto; }
.stat-row { margin-top: 8px; }
.block-title { font-size: 15px; font-weight: 600; margin: 16px 0 12px; }
.mb-16 { margin-bottom: 16px; }
</style>
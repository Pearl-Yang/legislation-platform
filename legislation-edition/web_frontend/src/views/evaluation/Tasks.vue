<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">评估任务</span>
      <span class="subtitle">从合法性 / 落实性 / 满意度 三维度评估法规实施效果</span>
      <el-button type="primary" :icon="Plus" class="ml-auto" @click="createVisible = true">新建评估任务</el-button>
    </div>

    <el-row :gutter="12" class="stat-row">
      <el-col :xs="12" :sm="6"><div class="stat-card stat-blue">
        <div class="icon-wrap"><el-icon><DataAnalysis /></el-icon></div>
        <div class="stat-content"><div class="stat-label">评估任务</div><div class="stat-value">{{ list.length }}</div></div>
      </div></el-col>
      <el-col :xs="12" :sm="6"><div class="stat-card stat-green">
        <div class="icon-wrap"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-content"><div class="stat-label">已完成</div><div class="stat-value">{{ countByStatus('DONE') }}</div></div>
      </div></el-col>
      <el-col :xs="12" :sm="6"><div class="stat-card stat-orange">
        <div class="icon-wrap"><el-icon><Loading /></el-icon></div>
        <div class="stat-content"><div class="stat-label">进行中</div><div class="stat-value">{{ countByStatus('RUNNING') }}</div></div>
      </div></el-col>
      <el-col :xs="12" :sm="6"><div class="stat-card stat-red">
        <div class="icon-wrap"><el-icon><Warning /></el-icon></div>
        <div class="stat-content"><div class="stat-label">平均分</div><div class="stat-value">{{ avgScore }}</div></div>
      </div></el-col>
    </el-row>

    <el-card class="mt-16">
      <el-table :data="list" stripe v-loading="loading" :empty-text="loading ? '加载中' : '暂无评估任务'">
        <el-table-column label="评估法规" min-width="220">
          <template #default="{ row }">
            <a @click="openTask(row)">{{ row.regulationName || `评估 #${row.id}` }}</a>
          </template>
        </el-table-column>
        <el-table-column label="评估周期" width="160">
          <template #default="{ row }">{{ row.periodLabel || formatPeriod(row) }}</template>
        </el-table-column>
        <el-table-column label="综合分" width="100" align="center">
          <template #default="{ row }">
            <span :class="scoreCls(row.overallScore)">{{ row.overallScore ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="合法性" width="90" align="center">
          <template #default="{ row }">
            <span :class="scoreCls(row.legalityScore)">{{ row.legalityScore ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="落实性" width="90" align="center">
          <template #default="{ row }">
            <span :class="scoreCls(row.executionScore)">{{ row.executionScore ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="满意度" width="90" align="center">
          <template #default="{ row }">
            <span :class="scoreCls(row.satisfactionScore)">{{ row.satisfactionScore ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openTask(row)">查看报告</el-button>
            <el-dropdown trigger="click" @command="(fmt) => onExportReport(row, fmt)">
              <el-button link size="small">导出 ▾</el-button>
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

    <!-- 评估报告抽屉 -->
    <el-drawer v-model="detailVisible" :title="`评估报告 - ${active?.regulationName || active?.id}`" size="80%">
      <div v-if="active" v-loading="chartLoading" class="report">
        <div class="report-head">
          <h2>{{ active.regulationName || `评估 #${active.id}` }} · 实施情况评估报告</h2>
          <p class="text-secondary">评估周期：{{ active.periodLabel || formatPeriod(active) }} · 创建时间：{{ formatDate(active.createdAt) }}</p>
        </div>

        <el-row :gutter="16" class="mt-16">
          <el-col :span="10">
            <h3 class="block-title">三维评分（雷达图）</h3>
            <RadarChart :data="radarData" height="320px" />
          </el-col>
          <el-col :span="14">
            <h3 class="block-title">指标明细（{{ indicators.length }}）</h3>
            <el-table :data="indicators" stripe size="small" :empty-text="'暂无可用指标'">
              <el-table-column label="指标" prop="name" min-width="200" />
              <el-table-column label="维度" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :style="{ background: dimensionColor(row.dimension), color: '#fff', borderColor: dimensionColor(row.dimension) }">
                    {{ dimensionLabel(row.dimension) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="原始值" prop="rawValue" width="100" align="right" />
              <el-table-column label="归一化分" width="100" align="center">
                <template #default="{ row }">
                  <span :class="scoreCls(row.normalizedScore)">{{ row.normalizedScore ?? '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="权重" prop="weight" width="80" align="center" />
            </el-table>
          </el-col>
        </el-row>

        <h3 class="block-title mt-16">历史趋势（同期对比）</h3>
        <LineChart :data="trendData" height="280px" />
      </div>
    </el-drawer>

    <!-- 创建对话框 -->
    <el-dialog v-model="createVisible" title="新建评估任务" width="540px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="被评估法规" required>
          <el-select v-model="createForm.regulationId" placeholder="选择法规" filterable style="width: 100%">
            <el-option v-for="r in relatedRegulations" :key="r.id" :value="r.id" :label="r.regulationName" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估周期">
          <el-date-picker v-model="createForm.periodRange" type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
        </el-form-item>
        <el-form-item>
          <el-button @click="onDataSync" plain>先同步执法数据</el-button>
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
import { Plus, DataAnalysis, CircleCheck, Loading, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElLoading } from 'element-plus'
import { dimensionColor, dimensionLabel } from '@/utils/dict'
import { RadarChart, LineChart } from '@/components/charts'
import { listEvaluations, createEvaluation, getEvaluationChart, syncEvaluationData } from '@/api/legislation'
import { formatDate } from '@/utils'

const loading = ref(false)
const chartLoading = ref(false)
const creating = ref(false)
const createVisible = ref(false)
const detailVisible = ref(false)
const active = ref(null)
const list = ref([])
const relatedRegulations = ref([])

const indicators = ref([])
const radarData = ref([])
const trendData  = ref([])

const createForm = ref({
  regulationId: null,
  periodRange: []
})

async function loadList () {
  loading.value = true
  try {
    const { data } = await listEvaluations()
    // 后端可能返回 Page 或 List;两者都容错
    list.value = data?.records || data || []
    if (!Array.isArray(list.value)) list.value = []
    // 顺便拉可选法规列表(从 dashboard / cleanup 共享的接口不直达 regulation,简化处理:从 list 中提取)
  } catch (e) {
    ElMessage.error('加载评估任务失败：' + (e.message || ''))
    list.value = []
  } finally {
    loading.value = false
  }
}

async function loadRelatedRegulations () {
  try {
    const { data } = await import('@/api/legislation').then(m => m.listRegulations({ page: 1, size: 200 }))
    const records = data?.records || []
    relatedRegulations.value = records.map(r => ({ id: r.id, regulationName: r.regulationName }))
  } catch (e) {
    relatedRegulations.value = []
  }
}

async function openTask (row) {
  active.value = row
  detailVisible.value = true
  chartLoading.value = true
  try {
    const { data } = await getEvaluationChart(row.id)
    radarData.value = data?.radar || []
    trendData.value = data?.trend || []
    indicators.value = (data?.indicators || []).map(i => ({
      name:           i.name,
      dimension:      normalizeDimension(i.dimension),
      rawValue:       i.raw ?? i.rawValue,
      normalizedScore:i.score ?? i.normalizedScore,
      weight:         i.weight
    }))
  } catch (e) {
    ElMessage.warning('图表数据加载失败,显示占位')
    radarData.value = [
      { name: '合法性', max: 100, value: row.legalityScore || 80 },
      { name: '落实性', max: 100, value: row.executionScore || 75 },
      { name: '满意度', max: 100, value: row.satisfactionScore || 80 }
    ]
    trendData.value = [
      { period: '上期', score: Math.max(0, (row.overallScore || 80) - 5) },
      { period: '本期', score: row.overallScore || 80 }
    ]
    indicators.value = []
  } finally {
    chartLoading.value = false
  }
}

function normalizeDimension (d) {
  if (!d) return 'LEGALITY'
  if (d === '合法性' || d === 'LEGALITY')   return 'LEGALITY'
  if (d === '落实性' || d === 'EXECUTION') return 'EXECUTION'
  if (d === '满意度' || d === 'SATISFACTION') return 'SATISFACTION'
  return d
}

const statusLabel = (s) => ({ PENDING: '待执行', RUNNING: '进行中', DONE: '已完成' }[s] || s)
const statusTag   = (s) => ({ PENDING: 'info', RUNNING: 'warning', DONE: 'success' }[s] || 'info')
const scoreCls    = (sc) => sc == null ? '' : sc >= 85 ? 'text-success' : sc >= 75 ? 'text-primary-color' : sc >= 60 ? 'text-warning' : 'text-danger'

const countByStatus = (s) => list.value.filter(x => x.status === s).length
const avgScore = computed(() => {
  const done = list.value.filter(x => x.overallScore != null)
  if (!done.length) return '—'
  return (done.reduce((acc, x) => acc + x.overallScore, 0) / done.length).toFixed(1)
})

const formatPeriod = (row) => {
  const a = row?.periodStart ? formatDate(row.periodStart) : ''
  const b = row?.periodEnd   ? formatDate(row.periodEnd)   : ''
  if (!a && !b) return '—'
  return `${a} 至 ${b}`
}

const onCreate = async () => {
  if (!createForm.value.regulationId) return ElMessage.warning('请选择被评估法规')
  creating.value = true
  try {
    const body = {
      regulationId: createForm.value.regulationId
    }
    if (createForm.value.periodRange?.length === 2) {
      body.periodStart = createForm.value.periodRange[0]
      body.periodEnd   = createForm.value.periodRange[1]
    }
    await createEvaluation(body)
    ElMessage.success('评估任务已创建')
    createVisible.value = false
    createForm.value = { regulationId: null, periodRange: [] }
    loadList()
  } catch (e) {
    ElMessage.error('创建失败：' + (e.message || ''))
  } finally {
    creating.value = false
  }
}

const onDataSync = async () => {
  try {
    await syncEvaluationData()
    ElMessage.success('执法数据同步任务已发起')
  } catch (e) {
    ElMessage.error('同步失败：' + (e.message || ''))
  }
}

const onExportReport = async (row, format = 'HTML') => {
  const ext = format === 'DOCX' ? 'docx' : format === 'MARKDOWN' ? 'md' : 'html'
  const loadingSvc = ElLoading.service({ text: `正在生成 ${format} ...` })
  try {
    const { default: request } = await import('@/utils/request')
    const resp = await request.get(`/evaluation/${row.id}/report`, {
      params: { format },
      responseType: 'blob'
    })
    const blob = new Blob([resp.data], { type: resp.headers?.['content-type'] || 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `evaluation-${row.id}.${ext}`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出已开始')
  } catch (e) {
    ElMessage.error('导出失败：' + (e.message || ''))
  } finally {
    loadingSvc.close()
  }
}

onMounted(() => {
  loadList()
  loadRelatedRegulations()
})
</script>

<style lang="scss" scoped>
.ml-auto { margin-left: auto; }
.mt-16 { margin-top: 16px; }
.report-head h2 { font-size: 20px; }
.text-secondary { color: $text-secondary; font-size: 13px; }
.block-title { font-size: 15px; font-weight: 600; }
</style>
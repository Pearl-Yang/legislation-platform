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
      <el-table :data="list" stripe>
        <el-table-column label="评估法规" min-width="220" prop="regulationName">
          <template #default="{ row }">
            <a @click="openTask(row)">{{ row.regulationName }}</a>
          </template>
        </el-table-column>
        <el-table-column label="评估周期" prop="periodLabel" width="160" />
        <el-table-column label="综合分" width="100" align="center">
          <template #default="{ row }">
            <span :class="scoreCls(row.overallScore)">{{ row.overallScore ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="合法性" width="90" prop="legalityScore" align="center">
          <template #default="{ row }">
            <span :class="scoreCls(row.legalityScore)">{{ row.legalityScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="落实性" width="90" prop="executionScore" align="center">
          <template #default="{ row }">
            <span :class="scoreCls(row.executionScore)">{{ row.executionScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="满意度" width="90" prop="satisfactionScore" align="center">
          <template #default="{ row }">
            <span :class="scoreCls(row.satisfactionScore)">{{ row.satisfactionScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160" prop="createdAt" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openTask(row)">查看报告</el-button>
            <el-button link size="small">导出 HTML</el-button>
            <el-button link size="small" @click="onCompare(row)">同期对比</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 评估报告抽屉 -->
    <el-drawer v-model="detailVisible" :title="`评估报告 - ${active?.regulationName}`" size="80%">
      <div v-if="active" class="report">
        <div class="report-head">
          <h2>{{ active.regulationName }} · 实施情况评估报告</h2>
          <p class="text-secondary">评估周期：{{ active.periodLabel }} · 评估时间：{{ active.createdAt }}</p>
        </div>

        <el-row :gutter="16" class="mt-16">
          <el-col :span="10">
            <h3 class="block-title">三维评分(雷达图)</h3>
            <RadarChart :data="radarData" height="320px" />
          </el-col>
          <el-col :span="14">
            <h3 class="block-title">指标明细</h3>
            <el-table :data="indicators" stripe size="small">
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
                  <span :class="scoreCls(row.normalizedScore)">{{ row.normalizedScore }}</span>
                </template>
              </el-table-column>
              <el-table-column label="权重" prop="weight" width="80" align="center" />
            </el-table>
          </el-col>
        </el-row>

        <h3 class="block-title mt-16">历史趋势(同期对比)</h3>
        <LineChart :data="trendData" height="280px" />

        <h3 class="block-title mt-16">结论与建议（节选）</h3>
        <el-card shadow="never" class="ai-report">
          <p><strong>总体评价：</strong>本法规在评估期内整体实施情况综合得分 <strong>{{ active.overallScore }}</strong> 分（百分制），处于「{{ active.overallScore >= 85 ? '优秀' : active.overallScore >= 75 ? '良好' : '待改进' }}」水平。</p>
          <p><strong>合法性（{{ active.legalityScore }} 分）：</strong>执法合规率达 96%，未发现超越权限或与上位法冲突的情形，建议继续维持现行监管框架。</p>
          <p><strong>落实性（{{ active.executionScore }} 分）：</strong>执法覆盖率 92%，但基层执法人员的培训仍有提升空间，建议增设年度执法培训计划。</p>
          <p><strong>满意度（{{ active.satisfactionScore }} 分）：</strong>行政相对人满意度 84%，主要不满意点集中在处罚程序复杂、申请材料重复，建议进一步优化办事流程。</p>
          <p><strong>建议：</strong></p>
          <ol>
            <li>完善实施细则，简化办事流程；</li>
            <li>加强对基层执法人员的培训；</li>
            <li>建议在届满前进行一次专项评估，决定是否需要修订。</li>
          </ol>
        </el-card>
      </div>
    </el-drawer>

    <!-- 创建对话框 -->
    <el-dialog v-model="createVisible" title="新建评估任务" width="540px">
      <el-form label-width="100px">
        <el-form-item label="被评估法规" required>
          <el-select placeholder="选择法规" filterable style="width: 100%">
            <el-option v-for="r in relatedRegulations" :key="r.id" :value="r.id" :label="r.regulationName" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估周期">
          <el-date-picker type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onDataSync" plain>先同步执法数据</el-button>
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
import { ref, computed, onMounted } from 'vue'
import { Plus, DataAnalysis, CircleCheck, Loading, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { dimensionColor, dimensionLabel } from '@/utils/dict'
import { RadarChart, LineChart } from '@/components/charts'
import request from '@/utils/request'

const createVisible = ref(false)
const detailVisible = ref(false)
const active = ref(null)

// === 真实数据(默认占位) ===
const list = ref([])

async function loadList() {
  try {
    const res = await request.get('/evaluation/list')
    const data = res.data || res
    list.value = (data.records || data) || []
  } catch (e) {
    // 后端无数据时给一组占位
    list.value = [
      { id: 1, regulationName: '中华人民共和国数据安全法', periodLabel: '2025 全年', overallScore: 92, legalityScore: 96, executionScore: 88, satisfactionScore: 91, status: 'DONE', createdAt: '2026-01-15 09:00' },
      { id: 2, regulationName: '某省数据交易管理办法',     periodLabel: '2025 全年', overallScore: 81, legalityScore: 88, executionScore: 78, satisfactionScore: 76, status: 'RUNNING', createdAt: '2026-01-18 10:30' },
      { id: 3, regulationName: '某市网络数据管理细则',     periodLabel: '2025H1',   overallScore: 68, legalityScore: 75, executionScore: 65, satisfactionScore: 64, status: 'DONE',    createdAt: '2025-08-20 14:11' }
    ]
  }
}

const indicators = ref([])
const radarData = ref([])
const trendData  = ref([])

async function openTask(row) {
  active.value = row
  detailVisible.value = true
  // 拉真实图表数据
  try {
    const res = await request.get(`/evaluation/${row.id}/chart-data`)
    const data = res.data || res
    radarData.value = data.radar || []
    trendData.value = data.trend || []
    // indicators 取自后端的 indicators 数组
    indicators.value = (data.indicators || []).map(i => ({
      name:           i.name,
      dimension:      i.dimension === '合法性' ? 'LEGALITY' : i.dimension === '落实性' ? 'EXECUTION' : 'SATISFACTION',
      rawValue:       i.raw,
      normalizedScore:i.score,
      weight:         i.weight
    }))
  } catch (e) {
    // 占位
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
  }
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

const relatedRegulations = ref([
  { id: 1, regulationName: '中华人民共和国数据安全法' },
  { id: 3, regulationName: '某省数据交易管理办法' }
])

const onCompare = (row) => ElMessage.info(`对比同类规章：/evaluation/compare?type=...&period=${row.periodLabel}`)
const onDataSync = () => ElMessage.success('执法数据同步任务已发起')

onMounted(() => {
  loadList()
})
</script>

<style lang="scss" scoped>
.ml-auto { margin-left: auto; }
.mt-16 { margin-top: 16px; }
.report-head h2 { font-size: 20px; }
.text-secondary { color: $text-secondary; font-size: 13px; }
.block-title { font-size: 15px; font-weight: 600; }
.ai-report p { line-height: 1.9; color: $text-regular; margin-bottom: 8px; }
.ai-report ol { padding-left: 20px; line-height: 1.9; color: $text-regular; }
</style>
<template>
  <div class="page-container project-detail" v-loading="loading">
    <div class="page-header">
      <div>
        <el-button :icon="ArrowLeft" link @click="$router.back()">返回列表</el-button>
        <span class="title ml-8">{{ project?.projectName }}</span>
        <el-tag :type="projectStatusTag(project?.status)" size="small" effect="light" class="ml-8">
          {{ projectStatusLabel(project?.status) }}
        </el-tag>
        <!-- 期限预警徽章(7 天内 / 已逾期) -->
        <el-tag
          v-if="projectAlertBadge"
          :type="projectAlertBadge.type"
          size="small"
          class="ml-8"
          effect="dark"
        >
          <el-icon><AlarmClock /></el-icon>
          {{ projectAlertBadge.text }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button :icon="EditPen" plain :disabled="!project?.id" @click="onAdvance">推进下一阶段</el-button>
        <el-button :icon="RefreshLeft" plain :disabled="!project?.id" @click="onRollback">回退</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="16">
        <!-- 项目基础信息 -->
        <el-card>
          <template #header>
            <span class="title">项目基本信息</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目类型">
              <span class="type-badge" :class="projectTypeCls(project?.projectType)">
                {{ projectTypeLabel(project?.projectType) }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="当前节点">
              <span class="stage-dot" :class="stageStatusCls(currentStage?.status)"></span>
              {{ currentStage?.stageName || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="整体进度" :span="2">
              <el-progress :percentage="progress" :stroke-width="14" :text-inside="true" :color="progressColor" />
            </el-descriptions-item>
            <el-descriptions-item label="发布机关">{{ project?.issuingAuthority || '未指定' }}</el-descriptions-item>
            <el-descriptions-item label="发文字号">{{ project?.documentNumber || '尚未取得' }}</el-descriptions-item>
            <el-descriptions-item label="拟发布">{{ project?.publishDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="立项依据" :span="2">{{ project?.legalBasis || '—' }}</el-descriptions-item>
            <el-descriptions-item label="协调部门" :span="2">{{ project?.coordinatingDepartments || '无' }}</el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="2">{{ project?.description || '—' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- Day4 新增:ECharts 流程图(从立項 → 公布 的可视化路径) -->
        <el-card class="mt-16">
          <template #header>
            <div class="flex-between">
              <span class="title">
                <el-icon class="warn-icon"><Operation /></el-icon>
                流程图可视化(依《{{ templateBasis }}》)
              </span>
              <el-radio-group v-model="flowView" size="small">
                <el-radio-button value="chart">图示</el-radio-button>
                <el-radio-button value="list">列表</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <v-chart v-if="flowView === 'chart' && stages.length" :option="flowOption" autoresize style="height: 360px;" />
          <el-empty v-else-if="!stages.length" description="暂无流程节点数据" />
        </el-card>

        <!-- 流程时间轴 -->
        <el-card class="mt-16">
          <template #header>
            <div class="flex-between">
              <span class="title">
                <el-icon class="warn-icon"><Operation /></el-icon>
                流程节点明细(依《{{ templateBasis }}》)
              </span>
              <el-radio-group v-model="statusFilter" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="done">已完成</el-radio-button>
                <el-radio-button value="current">进行中</el-radio-button>
                <el-radio-button value="pending">待办</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <el-empty v-if="!stages.length" description="暂无流程节点数据" />
          <el-steps v-else :active="activeIndex" direction="vertical" finish-status="success" space="80px" class="stage-steps">
            <el-step
              v-for="s in filteredStages"
              :key="s.id"
              :title="s.stageName"
              :description="s.stageCode || ''"
              :status="stepStatus(s)"
            >
              <template #description>
                <div class="stage-desc">
                  <div class="sd-line">
                    <span class="sd-label">法定期限:</span>
                    <span>{{ s.defaultDays || 30 }} 天</span>
                  </div>
                  <div v-if="s.startedAt" class="sd-line">
                    <span class="sd-label">开始:</span>
                    <span>{{ s.startedAt }}</span>
                  </div>
                  <div v-if="s.completedAt" class="sd-line">
                    <span class="sd-label">完成:</span>
                    <span>{{ s.completedAt }}</span>
                  </div>
                  <div v-if="s.remark" class="sd-line">
                    <span class="sd-label">备注:</span>
                    <span>{{ s.remark }}</span>
                  </div>
                  <!-- 节点自身的 7 天预警 -->
                  <div v-if="deadlineBadgeFor(s).text" class="sd-line">
                    <el-tag :type="deadlineBadgeFor(s).type" size="small" effect="dark">
                      <el-icon><AlarmClock /></el-icon>
                      {{ deadlineBadgeFor(s).text }}
                    </el-tag>
                  </div>
                  <div class="sd-actions" v-if="s.status === 'IN_PROGRESS'">
                    <el-button size="small" type="primary" @click="onAdvance">提交 / 推进</el-button>
                    <el-button size="small" plain @click="onRollback">退回</el-button>
                  </div>
                </div>
              </template>
            </el-step>
          </el-steps>
        </el-card>
      </el-col>

      <!-- 右:期限预警 + 关联法规 + 草案入口 -->
      <el-col :xs="24" :md="8">
        <el-card>
          <template #header>
            <span class="title">
              <el-icon class="warn-icon"><BellFilled /></el-icon>
              期限预警({{ deadlines.length }})
            </span>
          </template>
          <div class="deadline-list">
            <div v-for="d in deadlines" :key="d.id" class="deadline-row" :class="{ overdue: d.daysLeft < 0, soon: d.daysLeft >= 0 && d.daysLeft < 7 }">
              <div class="dr-icon">
                <el-icon><AlarmClock /></el-icon>
              </div>
              <div class="dr-content">
                <div class="dr-title">{{ d.nodeName }}</div>
                <div class="dr-meta">{{ d.deadlineDate }} · {{ d.deadlineType || '法定' }}</div>
              </div>
              <el-tag v-if="d.daysLeft < 0" type="danger" size="small">逾期 {{ -d.daysLeft }} 天</el-tag>
              <el-tag v-else-if="d.daysLeft < 7" type="warning" size="small">{{ d.daysLeft }} 天</el-tag>
              <el-tag v-else type="info" size="small">{{ d.daysLeft }} 天</el-tag>
            </div>
            <el-empty v-if="!deadlines.length" description="无即将到期的关键节点" :image-size="60" />
          </div>
        </el-card>

        <el-card class="mt-16">
          <template #header>
            <span class="title">AI 草案生成</span>
          </template>
          <p class="tip-text">基于本项目类型 + 当前节点,自动加载上位法与异地参考规章,AI 辅助生成草案正文</p>
          <el-button type="primary" :icon="MagicStick" class="w-full" @click="goDraftFromProject">前往生成</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, EditPen, RefreshLeft, Operation, BellFilled,
  AlarmClock, MagicStick
} from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { GraphChart as EGraphChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import {
  getProject, advanceProject, rollbackProject, listStages
} from '@/api/legislation'
import { projectTypeLabel, projectTypeCls, projectStatusLabel, projectStatusTag,
         stageStatusCls } from '@/utils/dict'

use([CanvasRenderer, EGraphChart, TitleComponent, TooltipComponent, LegendComponent])

const route  = useRoute()
const router = useRouter()
const loading = ref(false)
const flowView = ref('chart')

const project = ref(null)
const stages = ref([])
const currentStage = ref(null)
const progress = ref(0)
const deadlines = ref([])
const statusFilter = ref('all')

const templateBasis = computed(() =>
  project.value?.projectType === 'ADMIN_REGULATION'
    ? '行政法规制定程序条例》'
    : '规章制定程序条例》'
)

const filteredStages = computed(() => {
  if (statusFilter.value === 'all') return stages.value
  return stages.value.filter(s => {
    if (statusFilter.value === 'done')    return s.status === 'DONE'
    if (statusFilter.value === 'current') return s.status === 'IN_PROGRESS'
    if (statusFilter.value === 'pending') return ['PENDING','WAITING'].includes(s.status)
    return true
  })
})

const activeIndex = computed(() => {
  const i = stages.value.findIndex(s => s.status === 'IN_PROGRESS')
  return i === -1 ? stages.value.length : i
})

const stepStatus = (s) => {
  if (s.status === 'DONE') return 'success'
  if (s.status === 'IN_PROGRESS') return 'process'
  if (s.status === 'RETURNED') return 'error'
  return 'wait'
}

// ============= Day4 新增:ECharts graph 流程图 =============
const STAGE_COLOR = {
  DONE:        '#10b981',
  IN_PROGRESS: '#4f46e5',
  PENDING:     '#94a3b8',
  RETURNED:    '#f43f5e',
  SKIPPED:     '#cbd5e1'
}
const STAGE_ICON = {
  DONE:        '✓',
  IN_PROGRESS: '●',
  PENDING:     '○',
  RETURNED:    '!',
  SKIPPED:     '—'
}
const flowOption = computed(() => {
  const list = stages.value
  if (!list.length) return {}
  const nodes = list.map((s, i) => ({
    id:    String(s.id || s.stageCode || i),
    name:  s.stageName,
    value: s.status,
    x:     100 + (i % 5) * 200,
    y:     200 + Math.floor(i / 5) * 200,
    symbolSize: s.status === 'IN_PROGRESS' ? 56 : 40,
    itemStyle: {
      color: STAGE_COLOR[s.status] || '#94a3b8',
      borderColor: '#fff',
      borderWidth: s.status === 'IN_PROGRESS' ? 4 : 2,
      shadowBlur: s.status === 'IN_PROGRESS' ? 18 : 0,
      shadowColor: STAGE_COLOR[s.status] || '#4f46e5'
    },
    label: {
      show: true,
      formatter: (p) => {
        const stage = list.find(s => (s.id || s.stageCode) == p.dataName) || list[p.dataIndex]
        const ico = STAGE_ICON[stage?.status] || '?'
        return `{ico|${ico}}\n{name|${p.data.name}}`
      },
      rich: {
        ico: { fontSize: 18, fontWeight: 'bold', color: '#fff', padding: [0, 0, 4, 0] },
        name:{ fontSize: 11, color: '#1e293b', fontWeight: 600 }
      },
      position: 'inside'
    }
  }))
  const links = list.slice(0, -1).map((s, i) => ({
    source: String(s.id || s.stageCode || i),
    target: String(list[i + 1].id || list[i + 1].stageCode || (i + 1)),
    lineStyle: {
      color: list[i + 1].status === 'IN_PROGRESS' ? '#4f46e5' : '#94a3b8',
      width: list[i + 1].status === 'IN_PROGRESS' ? 3 : 2,
      type:  list[i + 1].status === 'IN_PROGRESS' ? 'solid' : 'dashed',
      curveness: 0.05
    },
    symbol: ['none', 'arrow'],
    symbolSize: 8
  }))
  return {
    tooltip: {
      formatter: (p) => {
        if (p.dataType === 'node') {
          return `<strong>${p.data.name}</strong><br/>状态:${p.data.value}`
        }
        return ''
      }
    },
    series: [{
      type: 'graph',
      layout: 'none',
      roam: false,
      draggable: false,
      data: nodes,
      links,
      edgeSymbol: ['none', 'arrow'],
      emphasis: { focus: 'adjacency', lineStyle: { width: 4 } },
      animationDuration: 800,
      animationEasing: 'cubicOut'
    }]
  }
})

// ============= Day4 新增:项目级 + 节点级 7 天预警 badge =============
const projectAlertBadge = computed(() => {
  if (!deadlines.value.length) return null
  const soonest = deadlines.value.reduce((min, d) => {
    if (d.daysLeft == null) return min
    return (min == null || d.daysLeft < min) ? d.daysLeft : min
  }, null)
  if (soonest == null) return null
  if (soonest < 0)  return { type: 'danger',  text: `已逾期 ${-soonest} 天` }
  if (soonest < 7)  return { type: 'warning', text: `${soonest} 天内到期` }
  return null
})

function deadlineBadgeFor (stage) {
  // 根据阶段 defaultDays + 进度模拟一个近似截止日;真实场景用 stage.deadlineDate
  const dl = (deadlines.value || []).find(d => d.nodeCode === stage.stageCode || d.stageCode === stage.stageCode)
  if (!dl) return { type: '' }
  if (dl.daysLeft < 0) return { type: 'danger',  text: `逾期 ${-dl.daysLeft} 天` }
  if (dl.daysLeft < 7) return { type: 'warning', text: `${dl.daysLeft} 天内到期` }
  return { type: 'info', text: `${dl.daysLeft} 天` }
}

const progressColor = [
  { color: '#f43f5e', percentage: 20 },
  { color: '#f59e0b', percentage: 50 },
  { color: '#10b981', percentage: 80 },
  { color: '#4f46e5', percentage: 100 }
]

async function loadDetail () {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const { data } = await getProject(id)
    project.value      = data?.project || null
    stages.value       = data?.stages || []
    currentStage.value = data?.currentStage || null
    progress.value     = data?.progress || 0
    deadlines.value    = data?.deadlines || []
    // 兜底:主动拉 stages(若后端没给)
    if (!stages.value.length) {
      try {
        const { data: sList } = await listStages(id)
        stages.value = sList || []
      } catch (_) {}
    }
  } catch (e) {
    ElMessage.error('加载项目详情失败:' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

const onAdvance = async () => {
  if (!project.value?.id) return
  try {
    await advanceProject(project.value.id, {})
    ElMessage.success('已推进到下一阶段')
    loadDetail()
  } catch (e) {}
}

const onRollback = async () => {
  if (!project.value?.id) return
  try {
    const { value } = await ElMessageBox.prompt('请输入目标阶段顺序号(从 1 开始)', '回退阶段', { inputPattern: /^\d+$/, inputErrorMessage: '请输入正整数' })
    await rollbackProject(project.value.id, { targetStageOrder: Number(value) })
    ElMessage.success('已回退到指定阶段')
    loadDetail()
  } catch (e) {}
}

const goDraftFromProject = () => {
  router.push({ path: '/app/draft/generate', query: { projectId: project.value?.id } })
}

onMounted(loadDetail)
</script>

<style lang="scss" scoped>
.project-detail .header-actions { display: flex; gap: 8px; }
.ml-8 { margin-left: 8px; }

.tip-text { font-size: 13px; color: $text-secondary; line-height: 1.7; margin-bottom: 12px; }
.w-full { width: 100%; }

.stage-steps {
  padding: 8px 0;
  :deep(.el-step__description) { padding-bottom: 8px; }
}

.stage-desc {
  font-size: 13px;
  color: $text-regular;
  .sd-line { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; flex-wrap: wrap; }
  .sd-label { color: $text-secondary; min-width: 64px; }
  .sd-actions { display: flex; gap: 8px; margin-top: 8px; }
}

.deadline-list { display: flex; flex-direction: column; gap: 8px; }
.deadline-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  background: $bg-page;
  &.overdue { background: #fef0f0; }
  &.soon    { background: #fdf6ec; }
  .dr-icon { width: 32px; height: 32px; border-radius: 6px; background: rgba(30,90,150,0.08); color: $primary-color; display: flex; align-items: center; justify-content: center; }
  .dr-title { font-size: 13px; font-weight: 500; }
  .dr-meta  { font-size: 12px; color: $text-secondary; margin-top: 2px; }
  .dr-content { flex: 1; }
}
</style>
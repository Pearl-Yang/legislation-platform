<template>
  <div class="page-container project-detail" v-loading="loading">
    <div class="page-header">
      <div>
        <el-button :icon="ArrowLeft" link @click="$router.back()">返回列表</el-button>
        <span class="title ml-8">{{ project?.projectName }}</span>
        <el-tag :type="projectStatusTag(project?.status)" size="small" effect="light" class="ml-8">
          {{ projectStatusLabel(project?.status) }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button :icon="EditPen" plain @click="onAdvance">推进下一阶段</el-button>
        <el-button :icon="RefreshLeft" plain @click="onRollback">回退</el-button>
        <el-button :icon="Download" plain>导出材料清单</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 左：项目信息 + 时间轴 -->
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
              <el-progress :percentage="progress" :stroke-width="14" :text-inside="true"
                :color="progressColor" />
            </el-descriptions-item>
            <el-descriptions-item label="发布机关">{{ project?.issuingAuthority || '未指定' }}</el-descriptions-item>
            <el-descriptions-item label="发文字号">{{ project?.documentNumber || '尚未取得' }}</el-descriptions-item>
            <el-descriptions-item label="拟发布">{{ project?.publishDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag :type="priorityTag(project?.priority)" size="small">{{ priorityLabel(project?.priority) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="协调部门" :span="2">{{ project?.coordinatingDepartments || '无' }}</el-descriptions-item>
            <el-descriptions-item label="项目描述" :span="2">{{ project?.description || '—' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 流程时间轴 -->
        <el-card class="mt-16">
          <template #header>
            <div class="flex-between">
              <span class="title">
                <el-icon class="warn-icon"><Operation /></el-icon>
                流程节点（依《{{ templateBasis }}》）
              </span>
              <el-radio-group v-model="statusFilter" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="done">已完成</el-radio-button>
                <el-radio-button value="current">进行中</el-radio-button>
                <el-radio-button value="pending">待办</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <el-steps :active="activeIndex" direction="vertical" finish-status="success" space="80px" class="stage-steps">
            <el-step
              v-for="s in filteredStages"
              :key="s.id"
              :title="s.stageName"
              :description="stageDescription(s)"
              :status="stepStatus(s)"
            >
              <template #description>
                <div class="stage-desc">
                  <div class="sd-line">
                    <span class="sd-label">法规依据：</span>
                    <el-tag size="small" effect="plain" type="info">{{ s.legalBasis || '通用' }}</el-tag>
                  </div>
                  <div class="sd-line">
                    <span class="sd-label">法定期限：</span>
                    <span>{{ s.defaultDays || 30 }} 天</span>
                  </div>
                  <div v-if="s.operatorTime" class="sd-line">
                    <span class="sd-label">操作：</span>
                    <span>操作人 ID {{ s.operatorId }} · {{ s.operatorTime }}</span>
                  </div>
                  <div v-if="s.remark" class="sd-line">
                    <span class="sd-label">备注：</span>
                    <span>{{ s.remark }}</span>
                  </div>
                  <div v-if="s.requiredDocs && s.requiredDocs.length" class="sd-line">
                    <span class="sd-label">所需材料：</span>
                    <el-tag v-for="d in s.requiredDocs" :key="d" size="small" effect="plain">{{ d }}</el-tag>
                  </div>
                  <div class="sd-actions">
                    <el-button v-if="s.status === 'IN_PROGRESS'" size="small" type="primary" @click="onAdvance">提交 / 推进</el-button>
                    <el-button v-if="s.status === 'IN_PROGRESS'" size="small" plain @click="onRollback">退回</el-button>
                    <el-button v-if="s.status === 'IN_PROGRESS'" size="small" plain>加签</el-button>
                    <el-button size="small" plain @click="showMaterials(s)">查看材料</el-button>
                  </div>
                </div>
              </template>
            </el-step>
          </el-steps>
        </el-card>

        <!-- 节点材料 -->
        <el-card class="mt-16">
          <template #header>
            <span class="title">节点附件材料</span>
          </template>
          <el-table :data="documents" stripe size="default">
            <el-table-column label="材料类型" prop="documentType" width="140" />
            <el-table-column label="文件名称" min-width="220" prop="documentName" />
            <el-table-column label="上传时间" width="110" prop="uploadedAt" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === '有效' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default>
                <el-button link type="primary" size="small">预览</el-button>
                <el-button link size="small">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右：期限预警 + 关联法规 + 草案入口 -->
      <el-col :xs="24" :md="8">
        <el-card>
          <template #header>
            <span class="title">
              <el-icon class="warn-icon"><BellFilled /></el-icon>
              期限预警
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
          <p class="tip-text">基于本项目类型 + 当前节点，自动加载上位法与异地参考规章，AI 辅助生成草案正文</p>
          <el-button type="primary" :icon="MagicStick" class="w-full" @click="$router.push('/app/draft/generate')">前往生成</el-button>
        </el-card>

        <el-card class="mt-16">
          <template #header>
            <span class="title">关联法规（{{ relatedRegulations.length }}）</span>
          </template>
          <div class="rel-list">
            <div v-for="r in relatedRegulations" :key="r.id" class="rel-row">
              <span class="type-badge" :class="projectTypeCls(r.regulationType)">
                {{ projectTypeLabel(r.regulationType) }}
              </span>
              <a class="rel-name" @click="onOpenRelation(r)">{{ r.regulationName }}</a>
              <el-tag size="small" effect="plain">{{ r.relationType }}</el-tag>
            </div>
            <el-empty v-if="!relatedRegulations.length" description="暂未关联法规" :image-size="60" />
          </div>
        </el-card>

        <el-card class="mt-16">
          <template #header>
            <span class="title">公平竞争审查 / 风险评估</span>
          </template>
          <div class="check-list">
            <div class="check-row">
              <span>公平竞争审查（起草前）</span>
              <el-tag type="success" size="small">已通过</el-tag>
            </div>
            <div class="check-row">
              <span>社会稳定风险评估</span>
              <el-tag type="warning" size="small">进行中</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 节点材料抽屉 -->
    <el-drawer v-model="drawerVisible" :title="`节点材料：${currentStageName}`" size="540px">
      <el-upload
        action="#"
        :auto-upload="false"
        list-type="text"
        :on-change="onUploadChange"
        :show-file-list="true"
        multiple
      >
        <el-button type="primary" :icon="Upload">点击上传</el-button>
        <template #tip>
          <div class="el-upload__tip">支持 .docx / .pdf / .xlsx / .zip，单文件不超过 50MB</div>
        </template>
      </el-upload>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, EditPen, RefreshLeft, Download, Operation, BellFilled,
  AlarmClock, MagicStick, Upload
} from '@element-plus/icons-vue'
import { getProject, listStages, currentStage as apiCurrentStage,
         progress as apiProgress, deadlines as apiDeadlines, advanceProject, rollbackProject } from '@/api/legislation'
import { projectTypeLabel, projectTypeCls, projectStatusLabel, projectStatusTag,
         stageStatusCls, STAGE_TEMPLATE_ADMIN, STAGE_TEMPLATE_RULE } from '@/utils/dict'

const route = useRoute()
const loading = ref(false)

const project = ref({
  id: 1, projectName: '网络数据安全管理条例', projectType: 'ADMIN_REGULATION',
  description: '为规范网络数据处理活动，保障数据安全与合法权益',
  status: 'ACTIVE', issuingAuthority: '国务院', publishDate: '2026-09-01',
  documentNumber: '国务院令第 765 号', priority: 'HIGH',
  coordinatingDepartments: '网信办、公安部、工信部'
})

const stages = ref([
  { id: 1, stageCode: 'PROPOSAL',        stageName: '立项建议',     stageOrder: 1, status: 'DONE', legalBasis: '条例第7条',  defaultDays: 15, operatorTime: '2026-07-10' },
  { id: 2, stageCode: 'PROPOSAL_REVIEW', stageName: '立项审查',     stageOrder: 2, status: 'DONE', legalBasis: '条例第8条',  defaultDays: 30, operatorTime: '2026-07-25' },
  { id: 3, stageCode: 'DRAFTING',        stageName: '起草',         stageOrder: 3, status: 'DONE', legalBasis: '条例第9-12条', defaultDays: 90, operatorTime: '2026-08-30' },
  { id: 4, stageCode: 'PUBLIC_COMMENT',  stageName: '征求意见',     stageOrder: 4, status: 'IN_PROGRESS', legalBasis: '条例第14条', defaultDays: 30, operatorTime: '2026-09-15', requiredDocs: ['征求意见稿','起草说明'] },
  { id: 5, stageCode: 'EXPERT_REVIEW',   stageName: '专家论证',     stageOrder: 5, status: 'PENDING', legalBasis: '条例第14条', defaultDays: 30 },
  { id: 6, stageCode: 'RISK_ASSESSMENT', stageName: '社会稳定风险评估', stageOrder: 6, status: 'PENDING', legalBasis: '中办发〔2010〕25号', defaultDays: 30 },
  { id: 7, stageCode: 'LEGAL_REVIEW',    stageName: '法制机构审查', stageOrder: 7, status: 'PENDING', legalBasis: '条例第13条', defaultDays: 30 },
  { id: 8, stageCode: 'DELIBERATION',    stageName: '审议',         stageOrder: 8, status: 'PENDING', legalBasis: '条例第17-18条', defaultDays: 30 },
  { id: 9, stageCode: 'PUBLICATION',     stageName: '公布',         stageOrder: 9, status: 'PENDING', legalBasis: '条例第21-22条', defaultDays: 15 },
  { id: 10, stageCode: 'FILING',         stageName: '备案',         stageOrder: 10, status: 'PENDING', legalBasis: '条例第23条', defaultDays: 30 }
])

const currentStage = ref(stages.value.find(s => s.status === 'IN_PROGRESS'))
const progress = ref(55)
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
    if (statusFilter.value === 'pending') return s.status === 'PENDING'
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

const stageDescription = (s) => s.stageCode

const deadlines = ref([
  { id: 1, nodeName: '提交征求意见汇总', deadlineDate: '2026-10-05', daysLeft: 2,  deadlineType: '法定' },
  { id: 2, nodeName: '法制机构审查签发', deadlineDate: '2026-10-08', daysLeft: 5,  deadlineType: '计划' },
  { id: 3, nodeName: '备案',             deadlineDate: '2026-10-30', daysLeft: 27, deadlineType: '法定' }
])

const documents = ref([
  { id: 1, documentType: '立项申请表', documentName: '网络数据安全管理条例立项申请表.docx', uploadedAt: '2026-07-10', status: '有效' },
  { id: 2, documentType: '论证报告',   documentName: '立法必要性与可行性论证报告.pdf',     uploadedAt: '2026-07-20', status: '有效' },
  { id: 3, documentType: '成本效益分析', documentName: '立法成本效益分析报告.docx',       uploadedAt: '2026-07-22', status: '有效' },
  { id: 4, documentType: '起草说明',   documentName: '起草说明（含背景与制度设计）.docx', uploadedAt: '2026-08-30', status: '有效' },
  { id: 5, documentType: '征求意见稿', documentName: '网络数据安全管理条例（征求意见稿）.docx', uploadedAt: '2026-09-15', status: '有效' }
])

const relatedRegulations = ref([
  { id: 1, regulationName: '中华人民共和国数据安全法', regulationType: 'ADMIN_REGULATION', relationType: '上位法依据' },
  { id: 2, regulationName: '中华人民共和国网络安全法', regulationType: 'ADMIN_REGULATION', relationType: '上位法依据' },
  { id: 3, regulationName: '某省数据交易管理办法',     regulationType: 'LOCAL_RULE',       relationType: '需配套清理' }
])

const progressColor = [
  { color: '#f43f5e', percentage: 20 },
  { color: '#f59e0b', percentage: 50 },
  { color: '#10b981', percentage: 80 },
  { color: '#4f46e5', percentage: 100 }
]

const priorityLabel = (p) => ({ HIGH: '高', MEDIUM: '中', LOW: '低' }[p] || '—')
const priorityTag   = (p) => ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }[p] || 'info')

const drawerVisible = ref(false)
const currentStageName = ref('')
const showMaterials = (s) => { currentStageName.value = s.stageName; drawerVisible.value = true }
const onUploadChange = (file) => { ElMessage.success(`已选择文件：${file.name}（待上传到后端）`) }

const onAdvance = async () => {
  await advanceProject(project.value.id)
  ElMessage.success('已推进到下一阶段')
}
const onRollback = async () => {
  ElMessage.info('回退需选目标节点，已记录到操作日志')
}

const onOpenRelation = (r) => {
  ElMessage.info(`跳转到法规详情：${r.regulationName}`)
}

onMounted(async () => {
  // 真实场景调用：
  // project.value = await getProject(route.params.id)
  // stages.value  = await listStages(route.params.id)
})
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

.rel-list { display: flex; flex-direction: column; gap: 8px; }
.rel-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  background: $bg-page;
  &:hover { background: $primary-lighter; }
  .rel-name { flex: 1; color: $primary-color; font-size: 13px; cursor: pointer; }
}

.check-list { display: flex; flex-direction: column; gap: 10px; }
.check-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 12px;
  background: $bg-page;
  border-radius: 6px;
  font-size: 13px;
}
</style>
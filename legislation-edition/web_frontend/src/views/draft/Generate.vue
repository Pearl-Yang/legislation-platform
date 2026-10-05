<template>
  <div class="page-container draft-page">
    <div class="page-header">
      <span class="title">AI 智能生成 · 立法草案</span>
      <span class="subtitle">RAG + LLM：解析上位法 → 检索异地规章 → 合成草案</span>
    </div>

    <el-row :gutter="16">
      <!-- 左：表单 -->
      <el-col :xs="24" :md="10">
        <el-card>
          <template #header>
            <span class="title">生成参数</span>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="关联项目" prop="projectId">
              <el-select v-model="form.projectId" placeholder="选择立法项目" filterable style="width: 100%">
                <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.projectName" />
              </el-select>
            </el-form-item>

            <el-form-item label="上位法条款" prop="superiorArticle">
              <el-input v-model="form.superiorArticle" type="textarea" :rows="3"
                placeholder="粘贴上位法中需要细化的事项条款，例如《数据安全法》第二十一条 ..." />
            </el-form-item>

            <el-form-item label="需要细化的事项" prop="items">
              <el-input v-model="form.items" type="textarea" :rows="3"
                placeholder="可填写多个事项，用分号分隔" />
            </el-form-item>

            <el-form-item label="章节数量">
              <el-input-number v-model="form.chapterCount" :min="3" :max="8" />
              <span class="form-tip">含总则 / 主体 / 法律责任 / 附则</span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="generating" :icon="MagicStick" @click="onGenerate">开始生成</el-button>
              <el-button :icon="DocumentCopy" plain @click="onTemplate">加载模板</el-button>
            </el-form-item>
          </el-form>

          <el-alert type="info" :closable="false" show-icon class="mt-16">
            <template #title>生成采用异步任务，完成后会推送到消息中心，此处可轮询查看</template>
          </el-alert>
        </el-card>
      </el-col>

      <!-- 右：任务进度 + 预览 -->
      <el-col :xs="24" :md="14">
        <el-card v-if="task">
          <template #header>
            <div class="flex-between">
              <span class="title">任务 #{{ task.taskId }}</span>
              <el-tag :type="taskStatusTag" size="small">{{ task.statusLabel }}</el-tag>
            </div>
          </template>
          <div class="task-progress">
            <el-progress :percentage="task.progress" :stroke-width="10" :text-inside="true" />
            <div class="task-meta">
              <span><el-icon><Loading /></el-icon>{{ task.currentStep || '初始化' }}</span>
              <span>开始：{{ formatTime(task.startTime) }}</span>
              <span v-if="task.endTime">结束：{{ formatTime(task.endTime) }}</span>
            </div>
          </div>

          <el-divider />

          <div v-if="task.status === 'SUCCESS' && task.draft">
            <el-tabs v-model="activeTab" class="result-tabs">
              <el-tab-pane label="生成结果" name="result">
                <div class="result-actions mb-12">
                  <el-button :icon="DocumentCopy" @click="copyResult">复制全文</el-button>
                  <el-button :icon="Download" @click="onExport('MARKDOWN')">导出 Markdown</el-button>
                  <el-button :icon="Download" @click="onExport('DOCX')">导出 Word</el-button>
                  <el-button type="primary" :icon="View" @click="goReview(task.draft.id)">提交智慧审查</el-button>
                </div>
                <pre class="result-text">{{ task.draft.draftContent || '(空)' }}</pre>
              </el-tab-pane>
              <el-tab-pane label="提示词快照" name="prompt">
                <pre class="prompt-text">{{ task.draft.promptSnapshot || form.superiorArticle }}</pre>
              </el-tab-pane>
            </el-tabs>
          </div>

          <div v-else-if="task.status === 'FAILED'" class="text-danger">
            <el-icon><CircleCloseFilled /></el-icon> {{ task.error || '生成失败' }}
          </div>
        </el-card>

        <el-empty v-else description="填写左侧表单后，点击「开始生成」创建生成任务" :image-size="120" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElLoading } from 'element-plus'
import {
  MagicStick, DocumentCopy, Download, View, Loading, CircleCloseFilled
} from '@element-plus/icons-vue'
import {
  listProjects, generateDraft, pollDraftTask, listDrafts, exportDraft, getDraft
} from '@/api/legislation'
import { formatDateTime, downloadFromResponse } from '@/utils'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const generating = ref(false)
const projects = ref([])
const task = ref(null)
let pollTimer = null

const form = reactive({
  projectId: route.query.projectId ? Number(route.query.projectId) : null,
  superiorArticle: '',
  items: '',
  chapterCount: 6
})

const rules = {
  projectId: [{ required: true, message: '请选择立法项目', trigger: 'change' }],
  superiorArticle: [{ required: true, message: '请输入上位法条款', trigger: 'blur' }],
  items: [{ required: true, message: '请说明需要细化的事项', trigger: 'blur' }]
}

const activeTab = ref('result')

const taskStatusTag = computed(() => {
  switch (task.value?.status) {
    case 'PROCESSING': return 'warning'
    case 'SUCCESS':    return 'success'
    case 'FAILED':     return 'danger'
    default:           return 'info'
  }
})

const formatTime = (t) => t ? formatDateTime(t) : '—'

async function loadProjects () {
  try {
    const { data } = await listProjects({ page: 1, size: 200 })
    projects.value = (data?.records || []).map(p => ({ id: p.id, projectName: p.projectName }))
    // 若从项目详情页跳过来,自动选中
    if (!form.projectId && projects.value.length) form.projectId = projects.value[0].id
  } catch (e) {
    ElMessage.error('加载项目列表失败：' + (e.message || ''))
  }
}

const onGenerate = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    generating.value = true
    try {
      const { data: taskId } = await generateDraft({
        projectId: form.projectId,
        prompt:    `${form.superiorArticle}\n\n需要细化的事项:\n${form.items}\n\n期望章节数: ${form.chapterCount}`
      })
      task.value = {
        taskId,
        status:      'PROCESSING',
        statusLabel: '进行中',
        progress:    5,
        currentStep: '解析上位法条款',
        startTime:   Date.now(),
        draft:       null
      }
      ElMessage.success('生成任务已提交，开始轮询进度')
      startPolling(taskId)
    } catch (e) {
      ElMessage.error('提交任务失败：' + (e.message || ''))
    } finally {
      generating.value = false
    }
  })
}

function startPolling (taskId) {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const { data } = await pollDraftTask(taskId)
      const status = data?.status || 'PROCESSING'
      task.value.status = status
      task.value.statusLabel = { PROCESSING: '进行中', SUCCESS: '已完成', FAILED: '失败' }[status] || status
      task.value.progress = data?.progress ?? task.value.progress
      task.value.currentStep = data?.currentStep || task.value.currentStep

      if (status === 'SUCCESS') {
        stopPolling()
        task.value.endTime = Date.now()
        // 后端把生成结果落到 legislative_draft, 拿到 draftId 拉详情
        const draftId = data?.draftId
        if (draftId) {
          try {
            const { data: draft } = await getDraft(draftId)
            task.value.draft = {
              id: draft.id,
                draftContent:   draft.draftContent,
                promptSnapshot: draft.promptSnapshot,
                generationType: draft.generationType,
                version:        draft.version
              }
          } catch (_) {}
        }
        ElMessage.success('草案生成完成')
      } else if (status === 'FAILED') {
        stopPolling()
        task.value.error = data?.error || data?.message || '生成失败'
        task.value.endTime = Date.now()
      }
    } catch (e) {
      // 单轮失败继续轮询,不停
      console.warn('[draft poll] error:', e?.message)
    }
  }, 1500)
}

function stopPolling () {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

const copyResult = () => {
  if (task.value?.draft?.draftContent) {
    navigator.clipboard.writeText(task.value.draft.draftContent)
    ElMessage.success('已复制到剪贴板')
  }
}

const onExport = async (format) => {
  const id = task.value?.draft?.id
  if (!id) return ElMessage.warning('暂无可导出草案')
  const ext = format === 'DOCX' ? 'docx' : format === 'HTML' ? 'html' : 'md'
  const loading = ElLoading.service({ text: `正在生成 ${format} ...` })
  try {
    const resp = await exportDraft(id, format)
    downloadFromResponse(resp, `draft-${id}-v${task.value?.draft?.version || ''}.${ext}`)
    ElMessage.success('导出已开始')
  } catch (e) {
    ElMessage.error('导出失败：' + (e.message || ''))
  } finally {
    loading.close()
  }
}

const goReview = (draftId) => {
  router.push({ path: '/app/review/submit', query: { draftId } })
}

const onTemplate = () => {
  form.superiorArticle = '《XX法》第XX条:'
  form.items = '一、XX 制度的具体适用情形;二、XX 工作的程序;'
  ElMessage.success('已加载「规章体例」模板')
}

onMounted(loadProjects)
onUnmounted(stopPolling)
</script>

<style lang="scss" scoped>
.form-tip { font-size: 12px; color: $text-secondary; margin-left: 8px; }

.task-progress { padding: 8px 0; }
.task-meta { display: flex; gap: 16px; flex-wrap: wrap; font-size: 12px; color: $text-secondary; margin-top: 12px; }
.task-meta span { display: inline-flex; align-items: center; gap: 4px; }

.result-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.result-text {
  background: #fafbfc;
  border: 1px solid $border-light;
  border-radius: 6px;
  padding: 16px;
  max-height: 540px;
  overflow: auto;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  line-height: 1.9;
  color: $text-regular;
  white-space: pre-wrap;
}

.prompt-text {
  background: #fafbfc;
  border: 1px solid $border-light;
  border-radius: 6px;
  padding: 12px;
  font-size: 12px;
  color: $text-secondary;
  line-height: 1.7;
  max-height: 400px;
  overflow: auto;
  white-space: pre-wrap;
}
</style>
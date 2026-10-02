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

            <el-form-item label="参考地区" prop="regions">
              <el-select v-model="form.regions" multiple placeholder="自动推荐异地参考，可手动选择" style="width: 100%">
                <el-option v-for="r in regionOptions" :key="r" :value="r" :label="r" />
              </el-select>
            </el-form-item>

            <el-form-item label="风格偏好">
              <el-radio-group v-model="form.style">
                <el-radio-button value="strict">严谨</el-radio-button>
                <el-radio-button value="balanced">均衡</el-radio-button>
                <el-radio-button value="concise">简明</el-radio-button>
              </el-radio-group>
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
            <template #title>生成采用异步任务，完成后会推送到消息中心，可在此处轮询查看进度</template>
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

          <div v-if="task.status === 'SUCCESS'">
            <el-tabs v-model="activeTab" class="result-tabs">
              <el-tab-pane label="生成结果" name="result">
                <div class="result-actions mb-12">
                  <el-button :icon="DocumentCopy" @click="copyResult">复制全文</el-button>
                  <el-button :icon="Download" @click="onExport('MARKDOWN')">导出 Markdown</el-button>
                  <el-button :icon="Download" @click="onExport('DOCX')">导出 Word</el-button>
                  <el-button type="primary" :icon="View" @click="$router.push('/app/review/submit')">提交智慧审查</el-button>
                </div>
                <pre class="result-text">{{ task.result }}</pre>
              </el-tab-pane>

              <el-tab-pane label="参考片段" name="references">
                <div v-for="r in task.references" :key="r.id" class="ref-item">
                  <div class="ref-head">
                    <span class="ref-name">{{ r.regulationName }}</span>
                    <span class="ref-region">{{ r.region }}</span>
                    <span class="ref-similarity">相似度 {{ (r.similarity * 100).toFixed(0) }}%</span>
                  </div>
                  <div class="ref-content">{{ r.excerpt }}</div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="提示词快照" name="prompt">
                <pre class="prompt-text">{{ task.prompt }}</pre>
              </el-tab-pane>
            </el-tabs>
          </div>

          <div v-else-if="task.status === 'FAILED'" class="text-danger">
            <el-icon><CircleCloseFilled /></el-icon> {{ task.error }}
          </div>
        </el-card>

        <el-empty v-else description="填写左侧表单后，点击「开始生成」创建生成任务" :image-size="120" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  MagicStick, DocumentCopy, Download, View, Loading, CircleCloseFilled
} from '@element-plus/icons-vue'
import { generateDraft, pollDraftTask, exportDraft } from '@/api/legislation'
import { formatDateTime, downloadBlob } from '@/utils'

const formRef = ref()
const generating = ref(false)
const task = ref(null)
let timer = null

const projects = ref([
  { id: 1, projectName: '网络数据安全管理条例' },
  { id: 2, projectName: '某省医疗保障办法' },
  { id: 5, projectName: '烟花爆竹安全管理规定' }
])

const regionOptions = ['北京', '上海', '广东', '江苏', '浙江', '四川', '山东', '湖北']

const form = reactive({
  projectId: 1,
  superiorArticle: '《数据安全法》第二十一条：国家建立数据分类分级保护制度。',
  items: '网络数据处理活动的具体分类标准；重要数据识别机制；',
  regions: ['北京', '上海'],
  style: 'balanced',
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

const onGenerate = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    generating.value = true
    try {
        const { data: { taskId } } = await generateDraft(form)
        task.value = {
          taskId, status: 'PROCESSING', statusLabel: '进行中',
          progress: 5, currentStep: '解析上位法条款',
          startTime: Date.now(), references: [], prompt: form.superiorArticle
        }
        ElMessage.success('生成任务已提交')
        // 模拟进度
        timer = setInterval(() => {
          if (!task.value || task.value.status !== 'PROCESSING') return
          task.value.progress = Math.min(task.value.progress + Math.random() * 12, 95)
          if (task.value.progress < 30) task.value.currentStep = '解析上位法条款'
          else if (task.value.progress < 55) task.value.currentStep = '检索异地参考规章'
          else if (task.value.progress < 80) task.value.currentStep = 'LLM 合成草案'
          else task.value.currentStep = '格式校验与润色'
          if (task.value.progress >= 95) {
            clearInterval(timer)
            task.value.status = 'SUCCESS'
            task.value.statusLabel = '已完成'
            task.value.progress = 100
            task.value.endTime = Date.now()
            task.value.currentStep = '完成'
            task.value.result = mockResult
            task.value.references = mockRefs
          }
        }, 800)
      } catch (e) {
        ElMessage.error('生成失败：' + (e.message || ''))
      } finally {
        generating.value = false
      }
  })
}

const mockResult = `第一章 总则
第一条 为了规范网络数据处理活动，保障数据安全与合法权益，根据《中华人民共和国数据安全法》《中华人民共和国网络安全法》等法律，制定本条例。

第二条 在中华人民共和国境内开展网络数据处理活动，及其安全监督管理，适用本条例。

第三条 网络数据处理活动应当遵循合法、正当、必要和诚信原则。

第二章 数据分类分级
第四条 国家建立数据分类分级保护制度。
网络数据处理者应当按照行业特点、风险等级，对所处理的网络数据进行分类分级管理。
涉及国家安全、公共利益的重要数据，实行目录管理。

第五条 重要数据处理者应当：
（一）明确数据安全管理责任人；
（二）建立重要数据处理台账；
（三）定期开展风险评估。

第三章 数据安全保护义务
第六条 网络数据处理者应当建立健全数据安全管理制度。
第七条 发生数据安全事件的，应当立即启动应急预案，并按要求向有关主管部门报告。
...

第七章 附则
第三十一条 本法自 XXXX 年 X 月 X 日起施行。`

const mockRefs = [
  { id: 1, regulationName: '北京市数据安全管理规定', region: '北京', similarity: 0.91, excerpt: '第十条 数据处理者应当建立数据分类分级管理制度...' },
  { id: 2, regulationName: '上海市公共数据管理办法', region: '上海', similarity: 0.86, excerpt: '第七条 重要数据处理者应当建立重要数据处理台账...' },
  { id: 3, regulationName: '浙江省数据要素市场化配置改革条例', region: '浙江', similarity: 0.83, excerpt: '第十五条 数据处理者应当定期开展数据安全风险评估...' }
]

const copyResult = () => {
  if (task.value?.result) {
    navigator.clipboard.writeText(task.value.result)
    ElMessage.success('已复制到剪贴板')
  }
}

const onExport = async (format) => {
  ElMessage.info(`导出 ${format}：后端接口 /draft/{id}/export?format=${format}（演示版暂返回提示）`)
}

onUnmounted(() => clearInterval(timer))

const onTemplate = () => {
  form.superiorArticle = '《XX法》第XX条：'
  form.items = '一、XX 制度的具体适用情形；二、XX 工作的程序；'
  ElMessage.success('已加载「规章体例」模板')
}
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
}

.ref-item {
  background: $bg-page;
  border-radius: 6px;
  padding: 12px 14px;
  margin-bottom: 8px;
  .ref-head { display: flex; gap: 12px; align-items: center; margin-bottom: 6px; }
  .ref-name { font-weight: 600; color: $text-primary; }
  .ref-region { font-size: 12px; color: $text-secondary; }
  .ref-similarity { font-size: 12px; color: $primary-color; margin-left: auto; }
  .ref-content { font-size: 13px; color: $text-regular; line-height: 1.7; }
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
}
</style>
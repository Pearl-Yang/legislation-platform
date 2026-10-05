<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">意见征集公告</span>
      <span class="subtitle">多渠道汇总 · AI 智能去重归类 · 意见采纳审议</span>
      <el-button type="primary" :icon="Plus" class="ml-auto" @click="createVisible = true">发布征集</el-button>
    </div>

    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-radio-group v-model="filter.status" @change="onSearch">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="OPEN">征集中</el-radio-button>
            <el-radio-button value="CLOSED">已结束</el-radio-button>
            <el-radio-button value="DRAFT">草稿</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="filter.keyword" placeholder="支持模糊搜索" clearable style="width: 240px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16" v-loading="loading">
      <el-table :data="list" stripe :empty-text="loading ? '加载中' : '暂无征集'">
        <el-table-column label="征集标题" min-width="280">
          <template #default="{ row }">
            <a @click="openDetail(row)">{{ row.title }}</a>
            <div class="sub-text">{{ row.description || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联" min-width="180">
          <template #default="{ row }">{{ row.relatedProjectName || row.relatedName || '—' }}</template>
        </el-table-column>
        <el-table-column label="渠道" width="240">
          <template #default="{ row }">
            <el-tag v-for="c in (row.channels || [])" :key="c" size="small" effect="plain" class="mr-4">{{ channelLabel(c) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="意见数" width="120" align="center">
          <template #default="{ row }">
            <div class="opp-row">
              <strong>{{ row.totalOpinions ?? 0 }}</strong>
              <span class="text-secondary">/去重 {{ row.dedupedCount ?? 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="采纳率" width="90" align="center">
          <template #default="{ row }">{{ row.adoptionRate ? row.adoptionRate + '%' : '—' }}</template>
        </el-table-column>
        <el-table-column label="截止日期" width="120">
          <template #default="{ row }">{{ row.endDate || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">意见列表</el-button>
            <el-button link size="small" @click="onDedup(row)">去重</el-button>
            <el-button link size="small" @click="onClassify(row)">AI 归类</el-button>
            <el-dropdown trigger="click" @command="(fmt) => onReport(row, fmt)">
              <el-button link size="small">生成报告 ▾</el-button>
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

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :total="page.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, jumper, prev, pager, next"
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="`意见征集详情 - ${active?.title}`" size="90%">
      <div v-if="active" v-loading="opinionsLoading">
        <el-card class="mb-16">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTag(active.status)" size="small">{{ statusLabel(active.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="起止日期">{{ active.startDate || '—' }} 至 {{ active.endDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="浏览 / 意见">{{ active.totalViews ?? 0 }} / {{ active.totalOpinions ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="关联草案" :span="3">{{ active.relatedProjectName || active.relatedName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="已启用渠道" :span="3">
              <el-tag v-for="c in (active.channels || [])" :key="c" size="small" effect="plain" class="mr-4">{{ channelLabel(c) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-row :gutter="16">
          <el-col :xs="24" :md="14">
            <h3 class="block-title">意见列表（{{ opinions.length }}）</h3>
            <el-card>
              <el-radio-group v-model="opinionFilter" size="small">
                <el-radio-button value="">全部 ({{ countOp(null) }})</el-radio-button>
                <el-radio-button value="NEW">新提交 ({{ countOp('NEW') }})</el-radio-button>
                <el-radio-button value="PROCESSED">已处理 ({{ countOp('PROCESSED') }})</el-radio-button>
                <el-radio-button value="REPLIED">已回复 ({{ countOp('REPLIED') }})</el-radio-button>
              </el-radio-group>

              <el-input v-model="oppSearch" placeholder="意见内容关键词" clearable class="mt-12" />

              <div class="opinion-list mt-12">
                <el-empty v-if="!opinionsLoading && filteredOpinions.length === 0" description="暂无符合条件的意见" :image-size="60" />
                <div v-for="o in filteredOpinions" :key="o.id" class="opinion-row">
                  <div class="op-head">
                    <span class="op-name">{{ o.isAnonymous ? '匿名' : (o.submitterName || `意见 #${o.id}`) }}</span>
                    <el-tag size="small" :type="opinionStatusTag(o.status)" effect="plain">{{ opinionStatusLabel(o.status) }}</el-tag>
                    <el-tag v-if="o.classifiedCategory" size="small" type="primary">{{ o.classifiedCategory }}</el-tag>
                    <el-tag v-if="o.viewpoint" size="small" :type="viewpointTag(o.viewpoint)">{{ viewpointLabel(o.viewpoint) }}</el-tag>
                    <span class="text-secondary op-time">{{ formatDate(o.submittedAt) }}</span>
                  </div>
                  <div class="op-content">{{ o.content }}</div>
                  <div class="op-foot">
                    <span v-if="o.aiConfidence != null">置信度 {{ Math.round(o.aiConfidence * 100) }}%</span>
                    <span v-if="o.keywords && o.keywords.length">关键词：{{ o.keywords.join('、') }}</span>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :md="10">
            <h3 class="block-title">分类统计</h3>
            <el-card>
              <v-chart :option="categoryOption" autoresize style="height: 300px;" v-if="scenarioTitle" />
              <el-empty v-else description="尚未生成统计数据" :image-size="60" />
            </el-card>

            <h3 class="block-title mt-16">关键词词云</h3>
            <el-card>
              <div class="word-cloud">
                <el-empty v-if="wordCloud.length === 0" description="暂无关键词" :image-size="50" />
                <span v-for="(w, idx) in wordCloud" :key="idx"
                      :style="{ fontSize: w.fontSize + 'px', color: w.color }">
                  {{ w.word }}
                </span>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-drawer>

    <!-- 发布对话框 -->
    <el-dialog v-model="createVisible" title="发布意见征集" width="640px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="征集标题" required>
          <el-input v-model="createForm.title" placeholder="关于《XX 条例（草案）》公开征求意见" />
        </el-form-item>
        <el-form-item label="征集说明">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="征集背景、目的、范围等" />
        </el-form-item>
        <el-form-item label="关联项目">
          <el-select v-model="createForm.relatedProjectId" placeholder="可选" filterable clearable style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.projectName" />
          </el-select>
        </el-form-item>
        <el-form-item label="征集起止">
          <el-date-picker v-model="createForm.periodRange" type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
        </el-form-item>
        <el-form-item label="启用渠道">
          <el-checkbox-group v-model="createForm.channels">
            <el-checkbox value="WEB">Web 后台</el-checkbox>
            <el-checkbox value="H5">H5 公众页</el-checkbox>
            <el-checkbox value="MINI_APP">微信小程序</el-checkbox>
            <el-checkbox value="GOV_APP">政务 APP</el-checkbox>
            <el-checkbox value="MEETING">座谈会</el-checkbox>
            <el-checkbox value="PAPER">纸质</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { ElMessage, ElLoading } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  listConsultations, createConsultation, getConsultation,
  listOpinions, getStatistics, getWordcloud,
  dedupOpinions, classifyOpinions, getConsultationReport,
  submitOpinion
} from '@/api/legislation'
import { opinionStatusLabel, opinionStatusTag } from '@/utils/dict'
import { formatDate } from '@/utils'

use([CanvasRenderer, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent])

const loading = ref(false)
const creating = ref(false)
const opinionsLoading = ref(false)
const filter = ref({ status: '', keyword: '' })
const createVisible = ref(false)
const detailVisible = ref(false)
const active = ref(null)
const opinionFilter = ref('')
const oppSearch = ref('')
const list = ref([])
const opinions = ref([])
const wordCloud = ref([])
const projects = ref([])
const page = ref({ current: 1, size: 20, total: 0 })

const createForm = ref({
  title: '',
  relatedProjectId: null,
  periodRange: [],
  channels: ['WEB', 'H5'],
  description: ''
})

const CHANNEL_LABEL = {
  WEB: 'Web 后台', H5: 'H5 公众页', MINI_APP: '微信小程序', GOV_APP: '政务 APP',
  LEGISLATIVE_CONTACT_POINT: '基层联系点', MEETING: '座谈会', PAPER: '纸质'
}
const channelLabel = (c) => CHANNEL_LABEL[c] || c

const VIEWPOINT_LABEL = { SUPPORT: '支持', OPPOSE: '反对', NEUTRAL: '中立', SUGGEST: '建议' }
const viewpointLabel = (v) => VIEWPOINT_LABEL[v] || v
const viewpointTag   = (v) => ({ SUPPORT: 'success', OPPOSE: 'danger', NEUTRAL: 'info', SUGGEST: 'warning' }[v] || 'info')

async function loadList () {
  loading.value = true
  try {
    const params = { page: page.value.current, size: page.value.size }
    if (filter.value.status) params.status = filter.value.status
    if (filter.value.keyword) params.keyword = filter.value.keyword
    const { data } = await listConsultations(params)
    list.value = data?.records || []
    page.value.total = data?.total || 0
  } catch (e) {
    ElMessage.error('加载征集列表失败：' + (e.message || ''))
    list.value = []
  } finally {
    loading.value = false
  }
}

async function loadProjects () {
  try {
    const { data } = await import('@/api/legislation').then(m => m.listProjects({ page: 1, size: 200 }))
    projects.value = (data?.records || []).map(p => ({ id: p.id, projectName: p.projectName }))
  } catch (_) {
    projects.value = []
  }
}

const onSearch = () => { page.value.current = 1; loadList() }

const openDetail = async (row) => {
  active.value = row
  detailVisible.value = true
  opinionsLoading.value = true
  try {
    // 详情
    const { data: detailData } = await getConsultation(row.id)
    active.value = { ...active.value, ...(detailData?.consultation || {}) }

    // 意见列表
    const { data: opData } = await listOpinions(row.id, { page: 1, size: 50 })
    opinions.value = opData?.records || []

    // 统计
    try {
      const { data: stat } = await getStatistics(row.id)
      buildCategoryOption(stat?.byCategory || {})
    } catch (_) {
      buildCategoryOption({})
    }

    // 词云
    try {
      const { data: wc } = await getWordcloud(row.id, 30)
      wordCloud.value = (wc || []).map((w, idx) => ({
        word: w.word || w.name,
        fontSize: 14 + Math.min(20, (w.weight || 1) * 3),
        color: ['#4f46e5', '#10b981', '#f59e0b', '#f43f5e', '#8b5cf6', '#06b6d4', '#94a3b8'][idx % 7]
      }))
    } catch (_) {
      wordCloud.value = []
    }
  } catch (e) {
    ElMessage.error('加载详情失败：' + (e.message || ''))
  } finally {
    opinionsLoading.value = false
  }
}

const categoryOption = ref({})
function buildCategoryOption (byCategory) {
  const entries = Object.entries(byCategory || {})
  if (!entries.length) { categoryOption.value = {}; return }
  categoryOption.value = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    color: ['#4f46e5', '#6366f1', '#10b981', '#f59e0b', '#f43f5e', '#06b6d4'],
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{d}%' },
      data: entries.map(([name, info]) => ({ value: info.total ?? info, name }))
    }]
  }
}

// 让 template 知道 scenarioTitle 是否该显示
const scenarioTitle = computed(() => Object.keys(categoryOption.value || {}).length > 0)

const filteredOpinions = computed(() => {
  let r = opinions.value
  if (opinionFilter.value) r = r.filter(o => o.status === opinionFilter.value)
  if (oppSearch.value)     r = r.filter(o => o.content?.includes(oppSearch.value))
  return r
})

const countOp = (s) => s == null ? opinions.value.length : opinions.value.filter(o => o.status === s).length

const onDedup = async (row) => {
  try {
    await dedupOpinions(row.id)
    ElMessage.success('去重任务已提交')
  } catch (e) {
    ElMessage.error('去重失败：' + (e.message || ''))
  }
}

const onClassify = async (row) => {
  try {
    await classifyOpinions(row.id)
    ElMessage.success('AI 归类任务已提交,稍后刷新详情查看')
  } catch (e) {
    ElMessage.error('归类失败：' + (e.message || ''))
  }
}

const onReport = async (row, format = 'HTML') => {
  const ext = format === 'DOCX' ? 'docx' : format === 'MARKDOWN' ? 'md' : 'html'
  const loadingSvc = ElLoading.service({ text: `正在生成 ${format} ...` })
  try {
    const { default: request } = await import('@/utils/request')
    const resp = await request.get(`/consultation/${row.id}/report`, {
      params: { format },
      responseType: 'blob'
    })
    const blob = new Blob([resp.data], { type: resp.headers?.['content-type'] || 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `consultation-${row.id}.${ext}`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出已开始')
  } catch (e) {
    ElMessage.error('导出失败：' + (e.message || ''))
  } finally {
    loadingSvc.close()
  }
}

const onCreate = async () => {
  if (!createForm.value.title) return ElMessage.warning('请填写征集标题')
  creating.value = true
  try {
    const body = {
      title: createForm.value.title,
      description: createForm.value.description,
      relatedProjectId: createForm.value.relatedProjectId,
      channels: createForm.value.channels
    }
    if (createForm.value.periodRange?.length === 2) {
      body.startDate = createForm.value.periodRange[0]
      body.endDate   = createForm.value.periodRange[1]
    }
    await createConsultation(body)
    ElMessage.success('意见征集已发布')
    createVisible.value = false
    createForm.value = { title: '', relatedProjectId: null, periodRange: [], channels: ['WEB', 'H5'], description: '' }
    loadList()
  } catch (e) {
    ElMessage.error('发布失败：' + (e.message || ''))
  } finally {
    creating.value = false
  }
}

import { onMounted } from 'vue'
onMounted(() => {
  loadList()
  loadProjects()
})
</script>

<style lang="scss" scoped>
.ml-auto { margin-left: auto; }
.mb-16 { margin-bottom: 16px; }
.mt-12 { margin-top: 12px; }
.mt-16 { margin-top: 16px; }
.sub-text { font-size: 12px; color: $text-secondary; margin-top: 4px; line-height: 1.5; }
.opp-row strong { color: $primary-color; font-size: 16px; }
.mr-4 { margin-right: 4px; }

.opinion-list { display: flex; flex-direction: column; gap: 8px; max-height: 70vh; overflow: auto; }
.opinion-row {
  padding: 12px 14px;
  border: 1px solid $border-light;
  border-radius: 6px;
  background: $bg-card;
  .op-head { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
  .op-name { font-size: 13px; font-weight: 500; }
  .op-time { margin-left: auto; font-size: 12px; }
  .op-content { margin-top: 8px; font-size: 13px; color: $text-regular; line-height: 1.7; }
  .op-foot { margin-top: 8px; display: flex; gap: 16px; font-size: 12px; color: $text-secondary; }
}

.block-title { font-size: 15px; font-weight: 600; }

.word-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  padding: 12px;
  span { transition: transform 0.18s; cursor: pointer; }
  span:hover { transform: scale(1.15); }
}

.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
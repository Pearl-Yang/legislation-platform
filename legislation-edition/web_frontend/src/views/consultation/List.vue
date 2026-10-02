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
          <el-button type="primary" :icon="Search">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16">
      <el-table :data="list" stripe>
        <el-table-column label="征集标题" min-width="280">
          <template #default="{ row }">
            <a @click="openDetail(row)">{{ row.title }}</a>
            <div class="sub-text">{{ row.description }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联" prop="relatedName" min-width="180" />
        <el-table-column label="渠道" width="240">
          <template #default="{ row }">
            <el-tag v-for="c in row.channels" :key="c" size="small" effect="plain" class="mr-4">{{ c }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="意见数" width="120" align="center">
          <template #default="{ row }">
            <div class="opp-row">
              <strong>{{ row.totalOpinions }}</strong>
              <span class="text-secondary">/去重 {{ row.dedupedCount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="采纳率" width="90" align="center" prop="adoptionRate">
          <template #default="{ row }">{{ row.adoptionRate ? row.adoptionRate + '%' : '—' }}</template>
        </el-table-column>
        <el-table-column label="截止日期" width="120" prop="endDate" />
        <el-table-column label="操作" width="320" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">意见列表</el-button>
            <el-button link size="small" @click="onDedup(row)">去重</el-button>
            <el-button link size="small" @click="onClassify(row)">AI 归类</el-button>
            <el-button link size="small" @click="onReport(row)">生成报告</el-button>
            <el-button link size="small" @click="onPublish(row)">发布</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="`意见征集详情 - ${active?.title}`" size="90%">
      <div v-if="active">
        <el-card class="mb-16">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTag(active.status)" size="small">{{ statusLabel(active.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="起止日期">{{ active.startDate }} 至 {{ active.endDate }}</el-descriptions-item>
            <el-descriptions-item label="浏览 / 意见">{{ active.totalViews }} / {{ active.totalOpinions }}</el-descriptions-item>
            <el-descriptions-item label="关联草案" :span="3">{{ active.relatedName }}</el-descriptions-item>
            <el-descriptions-item label="已启用渠道" :span="3">
              <el-tag v-for="c in active.channels" :key="c" size="small" effect="plain" class="mr-4">{{ c }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-row :gutter="16">
          <el-col :xs="24" :md="14">
            <h3 class="block-title">意见列表</h3>
            <el-card>
              <el-radio-group v-model="opinionFilter" size="small">
                <el-radio-button value="">全部 ({{ opinions.length }})</el-radio-button>
                <el-radio-button value="NEW">新提交 ({{ countOp('NEW') }})</el-radio-button>
                <el-radio-button value="PROCESSED">已处理 ({{ countOp('PROCESSED') }})</el-radio-button>
                <el-radio-button value="REPLIED">已回复 ({{ countOp('REPLIED') }})</el-radio-button>
              </el-radio-group>

              <el-input v-model="oppSearch" placeholder="意见内容关键词" clearable class="mt-12" />

              <div class="opinion-list mt-12">
                <div v-for="o in filteredOpinions" :key="o.id" class="opinion-row">
                  <div class="op-head">
                    <span class="op-name">{{ o.isAnonymous ? '匿名' : o.submitterName }}</span>
                    <el-tag size="small" :type="opinionStatusTag(o.status)" effect="plain">{{ opinionStatusLabel(o.status) }}</el-tag>
                    <el-tag v-if="o.classifiedCategory" size="small" type="primary">{{ o.classifiedCategory }}</el-tag>
                    <el-tag size="small" :type="viewpointTag(o.viewpoint)">{{ o.viewpoint || '中立' }}</el-tag>
                    <span class="text-secondary op-time">{{ o.submittedAt }}</span>
                  </div>
                  <div class="op-content">{{ o.content }}</div>
                  <div class="op-foot">
                    <span>置信度 {{ (o.aiConfidence * 100).toFixed(0) }}%</span>
                    <span v-if="o.keywords">关键词：{{ o.keywords.join('、') }}</span>
                    <div class="op-actions">
                      <el-button link type="primary" size="small">采纳</el-button>
                      <el-button link type="danger" size="small">不采纳</el-button>
                      <el-button link size="small">回复</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :md="10">
            <h3 class="block-title">分类统计</h3>
            <el-card>
              <v-chart :option="categoryOption" autoresize style="height: 300px;" />
            </el-card>

            <h3 class="block-title mt-16">关键词词云</h3>
            <el-card>
              <div class="word-cloud">
                <span v-for="(w, idx) in wordCloud" :key="w.word" :style="{ fontSize: w.size + 'px', color: w.color }">
                  {{ w.word }}
                </span>
              </div>
            </el-card>

            <h3 class="block-title mt-16">渠道分布</h3>
            <el-card>
              <ul class="channel-list">
                <li v-for="c in channelStats" :key="c.name">
                  <span>{{ c.name }}</span>
                  <div class="channel-bar"><div :style="{ width: c.percent + '%', background: c.color }"></div></div>
                  <span class="text-secondary">{{ c.count }} 条 · {{ c.percent }}%</span>
                </li>
              </ul>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-drawer>

    <!-- 发布对话框 -->
    <el-dialog v-model="createVisible" title="发布意见征集" width="640px">
      <el-form label-width="100px">
        <el-form-item label="征集标题" required>
          <el-input placeholder="关于《XX 条例（草案）》公开征求意见" />
        </el-form-item>
        <el-form-item label="征集说明">
          <el-input type="textarea" :rows="3" placeholder="征集背景、目的、范围等" />
        </el-form-item>
        <el-form-item label="关联项目">
          <el-select placeholder="可选" filterable clearable style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.projectName" />
          </el-select>
        </el-form-item>
        <el-form-item label="征集起止">
          <el-date-picker type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
        </el-form-item>
        <el-form-item label="启用渠道">
          <el-checkbox-group>
            <el-checkbox value="WEB">Web 后台</el-checkbox>
            <el-checkbox value="H5">H5 公众页</el-checkbox>
            <el-checkbox value="MINI_APP">微信小程序</el-checkbox>
            <el-checkbox value="GOV_APP">政务 APP</el-checkbox>
            <el-checkbox value="LEGISLATIVE_CONTACT_POINT">基层联系点</el-checkbox>
            <el-checkbox value="MEETING">座谈会</el-checkbox>
            <el-checkbox value="PAPER">纸质</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="createVisible = false">发布</el-button>
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
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { listConsultations, dedupOpinions, classifyOpinions } from '@/api/legislation'
import { opinionStatusLabel, opinionStatusTag } from '@/utils/dict'

use([CanvasRenderer, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent])

const filter = ref({ status: '', keyword: '' })
const createVisible = ref(false)
const detailVisible = ref(false)
const active = ref(null)
const opinionFilter = ref('')
const oppSearch = ref('')

const onSearch = () => {}

const projects = ref([
  { id: 1, projectName: '网络数据安全管理条例' },
  { id: 2, projectName: '某省医疗保障办法' }
])

const list = ref([
  { id: 1, title: '关于《网络数据安全管理条例（草案）》公开征求意见', description: '为规范网络数据处理活动，保障数据安全与合法权益', relatedName: '网络数据安全管理条例', status: 'OPEN', startDate: '2026-09-15', endDate: '2026-10-15', totalViews: 1532, totalOpinions: 248, dedupedCount: 198, adoptionRate: 22, channels: ['WEB', 'H5', 'MINI_APP'] },
  { id: 2, title: '关于《某省医疗保障办法》公开征求意见', description: '完善基本医保省级统筹制度', relatedName: '某省医疗保障办法', status: 'OPEN', startDate: '2026-09-01', endDate: '2026-10-10', totalViews: 884, totalOpinions: 132, dedupedCount: 110, adoptionRate: 18, channels: ['WEB', 'H5'] },
  { id: 3, title: '关于《某市人才公寓管理办法》草案征集意见', description: '明确人才公寓的建设分配管理机制', relatedName: '某市人才公寓管理办法', status: 'CLOSED', startDate: '2026-07-01', endDate: '2026-08-01', totalViews: 2156, totalOpinions: 312, dedupedCount: 256, adoptionRate: 34, channels: ['WEB', 'H5', 'MINI_APP', 'MEETING'] },
  { id: 4, title: '关于行政处罚类规章专项清理方案征求意见', description: '系统清理 2018 年前的行政处罚类规章', relatedName: '行政处罚类规章专项清理', status: 'DRAFT', startDate: '', endDate: '', totalViews: 0, totalOpinions: 0, dedupedCount: 0, adoptionRate: 0, channels: ['WEB'] }
])

const statusLabel = (s) => ({ DRAFT: '草稿', OPEN: '征集中', CLOSED: '已结束' }[s] || s)
const statusTag   = (s) => ({ DRAFT: 'info', OPEN: 'success', CLOSED: 'danger' }[s] || 'info')

const openDetail = (row) => { active.value = row; detailVisible.value = true }

const opinions = ref([
  { id: 1, submitterName: '张三', isAnonymous: false, status: 'NEW', content: '建议第二十条增加处罚力度，对违反数据安全的行为加重处罚。', submittedAt: '2026-10-02 09:23', classifiedCategory: '合理性意见/处罚力度', aiConfidence: 0.92, viewpoint: '支持', keywords: ['处罚', '数据安全'] },
  { id: 2, submitterName: '李四', isAnonymous: false, status: 'NEW', content: '本条可能与上位法《数据安全法》存在冲突。', submittedAt: '2026-10-02 10:11', classifiedCategory: '合法性意见/上位法冲突', aiConfidence: 0.87, viewpoint: '反对', keywords: ['上位法', '冲突'] },
  { id: 3, submitterName: '', isAnonymous: true, status: 'PROCESSED', content: '建议明确"重要数据"的定义，否则基层难以判断。', submittedAt: '2026-10-01 14:36', classifiedCategory: '操作性意见/表述不清', aiConfidence: 0.85, viewpoint: '建议', keywords: ['重要数据', '定义'] },
  { id: 4, submitterName: '王五', isAnonymous: false, status: 'REPLIED', content: '建议简化办事流程，目前办理时限较长。', submittedAt: '2026-09-30 11:20', classifiedCategory: '操作性意见/程序繁琐', aiConfidence: 0.89, viewpoint: '反对', keywords: ['办事流程', '简化'] },
  { id: 5, submitterName: '某行业协会', isAnonymous: false, status: 'NEW', content: '本条例对中小企业成本压力较大，建议差异化设计。', submittedAt: '2026-10-02 15:50', classifiedCategory: '可行性意见/执行成本', aiConfidence: 0.94, viewpoint: '反对', keywords: ['中小企业', '成本'] },
  { id: 6, submitterName: '', isAnonymous: true, status: 'PROCESSED', content: '第二十三条和第二十五条存在逻辑矛盾，请检查。', submittedAt: '2026-09-29 16:42', classifiedCategory: '操作性意见/逻辑矛盾', aiConfidence: 0.78, viewpoint: '建议', keywords: ['矛盾', '逻辑'] }
])

const filteredOpinions = computed(() => {
  let r = opinions.value
  if (opinionFilter.value) r = r.filter(o => o.status === opinionFilter.value)
  if (oppSearch.value) r = r.filter(o => o.content.includes(oppSearch.value))
  return r
})

const countOp = (s) => opinions.value.filter(o => o.status === s).length
const viewpointTag = (v) => ({ 支持: 'success', 反对: 'danger', 建议: 'warning', 中立: 'info' }[v] || 'info')

const wordCloud = ref([
  { word: '数据安全', size: 28, color: '#4f46e5' },
  { word: '处罚力度', size: 24, color: '#6366f1' },
  { word: '上位法',  size: 22, color: '#10b981' },
  { word: '中小企业', size: 20, color: '#f59e0b' },
  { word: '办事流程', size: 22, color: '#f43f5e' },
  { word: '重要数据', size: 26, color: '#8b5cf6' },
  { word: '成本', size: 18, color: '#06b6d4' },
  { word: '程序', size: 18, color: '#4f46e5' },
  { word: '定义', size: 16, color: '#94a3b8' },
  { word: '建议', size: 18, color: '#10b981' },
  { word: '逻辑', size: 14, color: '#f59e0b' }
])

const channelStats = ref([
  { name: 'Web 后台', count: 92, percent: 37, color: '#4f46e5' },
  { name: '微信小程序', count: 78, percent: 31, color: '#10b981' },
  { name: 'H5 公众页', count: 56, percent: 23, color: '#6366f1' },
  { name: '基层联系点', count: 22, percent: 9, color: '#f59e0b' }
])

const categoryOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, type: 'scroll' },
  color: ['#4f46e5', '#6366f1', '#10b981', '#f59e0b', '#f43f5e'],
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    avoidLabelOverlap: true,
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%' },
    data: [
      { value: 86, name: '合理性意见' },
      { value: 52, name: '合法性意见' },
      { value: 41, name: '操作性意见' },
      { value: 36, name: '可行性意见' },
      { value: 33, name: '其他意见' }
    ]
  }]
}))

const onDedup = async (row) => { await dedupOpinions(row.id); ElMessage.success('去重任务已提交') }
const onClassify = async (row) => { await classifyOpinions(row.id); ElMessage.success('AI 归类任务已提交') }
const onReport = (row) => ElMessage.info(`生成意见整理报告：${row.title}`)
const onPublish = (row) => ElMessage.success(`已发布：${row.title}`)
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
  .op-foot {
    margin-top: 8px;
    display: flex;
    gap: 16px;
    align-items: center;
    font-size: 12px;
    color: $text-secondary;
    .op-actions { margin-left: auto; }
  }
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

.channel-list { list-style: none; padding: 0; margin: 0; }
.channel-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  font-size: 13px;
  .channel-bar { flex: 1; height: 8px; background: $bg-page; border-radius: 4px; overflow: hidden; }
  .channel-bar > div { height: 100%; transition: width 0.4s; }
}
</style>
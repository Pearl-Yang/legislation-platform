<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">提交智慧审查</span>
      <span class="subtitle">规则引擎 + AI：红 / 黄 / 蓝 / 灰 四级问题分级</span>
    </div>

    <el-row :gutter="16">
      <!-- 左：选择草案 -->
      <el-col :xs="24" :md="10">
        <el-card>
          <template #header>
            <span class="title">选择待审查的草案</span>
          </template>
          <div class="search-bar">
            <el-input v-model="searchKw" :icon="Search" placeholder="按项目名搜索" clearable @clear="onSearch" @keyup.enter="onSearch" />
          </div>
          <div class="draft-list" v-loading="draftsLoading">
            <el-empty v-if="!draftsLoading && drafts.length === 0" description="暂无可审查草案" :image-size="80" />
            <div v-for="d in drafts"
                 :key="d.id"
                 class="draft-row"
                 :class="{ active: selected?.id === d.id }"
                 @click="selected = d"
            >
              <div class="dr-title">{{ d.projectName || d.draftTitle || `草案 #${d.id}` }} · v{{ d.version }}</div>
              <div class="dr-meta">
                <el-tag size="small" :type="d.generationType === 'AUTO_GENERATED' ? 'primary' : 'info'">
                  {{ d.generationType === 'AUTO_GENERATED' ? 'AI' : '人工' }}
                </el-tag>
                <span class="text-secondary">{{ d.wordCount || d.draftContent?.length || 0 }} 字 · {{ d.updatedAt || formatDate(d.createdAt) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右：触发审查 + 历史 -->
      <el-col :xs="24" :md="14">
        <el-card v-if="selected">
          <template #header>
            <span class="title">审查设置</span>
          </template>
          <el-form label-width="100px">
            <el-form-item label="审查方式">
              <el-radio-group v-model="reviewType">
                <el-radio-button value="AUTO">自动</el-radio-button>
                <el-radio-button value="MANUAL">人工</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="启用规则">
              <el-checkbox-group v-model="enabledRules">
                <el-checkbox v-for="r in rules" :key="r.id" :label="r.id">{{ r.ruleName }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="VideoPlay" :loading="submitting" @click="onSubmit">开始审查</el-button>
              <el-button :icon="Connection" plain @click="onBatchSubmit">一键审查最近 10 条</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="mt-16" v-if="currentReview">
          <template #header>
            <div class="flex-between">
              <span class="title">审查结果 #{{ currentReview.id }}</span>
              <el-tag :type="currentReview.overallPass ? 'success' : 'danger'" size="small">
                {{ currentReview.overallPass ? '通过' : '未通过' }}
              </el-tag>
            </div>
          </template>

          <el-row :gutter="12" class="summary-row">
            <el-col :span="6"><div class="sum sev-red">
              <div class="sum-num">{{ currentIssueStats.RED }}</div>
              <div class="sum-label">严重冲突</div>
            </div></el-col>
            <el-col :span="6"><div class="sum sev-yellow">
              <div class="sum-num">{{ currentIssueStats.YELLOW }}</div>
              <div class="sum-label">需关注</div>
            </div></el-col>
            <el-col :span="6"><div class="sum sev-blue">
              <div class="sum-num">{{ currentIssueStats.BLUE }}</div>
              <div class="sum-label">格式建议</div>
            </div></el-col>
            <el-col :span="6"><div class="sum sev-grey">
              <div class="sum-num">{{ currentIssueStats.GREY }}</div>
              <div class="sum-label">优化提示</div>
            </div></el-col>
          </el-row>

          <el-table :data="currentReview.issues" stripe class="mt-16">
            <el-table-column label="级别" width="80">
              <template #default="{ row }">
                <span class="sev-badge" :class="severityCls(row.severity)">
                  {{ severityLabel(row.severity).slice(0, 1) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="条款" prop="articleIndex" width="120" />
            <el-table-column label="问题类型" width="120">
              <template #default="{ row }">
                {{ row.issueTypeLabel || issueTypeLabel(row.issueType) }}
              </template>
            </el-table-column>
            <el-table-column label="问题描述" prop="description" min-width="240" />
            <el-table-column label="建议" prop="suggestion" min-width="240" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.isResolved" type="success" size="small">已解决</el-tag>
                <el-tag v-else type="warning" size="small">待处理</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default>
                <el-button v-if="!row.isResolved" link type="primary" size="small" @click="onResolve(row)">标记解决</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-empty v-else-if="!selected" description="请先选择要审查的草案" :image-size="120" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, VideoPlay, Connection } from '@element-plus/icons-vue'
import { submitReview, batchReview, listRules, resolveIssue, listDrafts, getReviewRecord } from '@/api/legislation'
import { severityLabel, severityCls, issueTypeLabel } from '@/utils/dict'
import { formatDate } from '@/utils'

const route = useRoute()
const searchKw = ref('')
const selected = ref(null)
const reviewType = ref('AUTO')
const enabledRules = ref([])
const submitting = ref(false)
const draftsLoading = ref(false)
const drafts = ref([])
const rules = ref([])
const currentReview = ref(null)

const currentIssueStats = computed(() => {
  if (!currentReview.value) return { RED: 0, YELLOW: 0, BLUE: 0, GREY: 0 }
  return currentReview.value.issues.reduce((acc, x) => {
    const k = x.severity || 'GREY'
    acc[k] = (acc[k] || 0) + 1
    return acc
  }, { RED: 0, YELLOW: 0, BLUE: 0, GREY: 0 })
})

async function loadDrafts () {
  draftsLoading.value = true
  try {
    // 取最近 N 个项目的草案汇总:简化做法 = 遍历前几个项目,聚合
    const { data: projPage } = await import('@/api/legislation').then(m => m.listProjects({ page: 1, size: 5 }))
    const projects = projPage?.records || []
    const allDrafts = []
    for (const p of projects) {
      try {
        const { data } = await listDrafts(p.id)
        for (const d of (data || [])) {
          allDrafts.push({
            ...d,
            projectName: p.projectName
          })
        }
      } catch (_) {}
    }
    drafts.value = allDrafts.sort((a, b) => (b.id - a.id)).slice(0, 50)
    // 若 query 带 draftId,自动选中
    if (route.query.draftId) {
      const found = drafts.value.find(d => d.id === Number(route.query.draftId))
      if (found) selected.value = found
    }
  } catch (e) {
    // 错误
  } finally {
    draftsLoading.value = false
  }
}

async function loadRules () {
  try {
    const { data } = await listRules()
    rules.value = data || []
    enabledRules.value = rules.value.map(r => r.id)
  } catch (e) {
    ElMessage.error('加载规则失败：' + (e.message || ''))
  }
}

const onSearch = () => {
  // 演示版搜索短语过滤
  // (实际项目中应传参给后端,这里简单本地过滤)
}

const onSubmit = async () => {
  if (!selected.value) return ElMessage.warning('请先选择草案')
  submitting.value = true
  try {
    const { data: recordId } = await submitReview({
      draftId:   selected.value.id,
      reviewType: reviewType.value
    })
    // 异步,轮询直到 DONE
    await pollRecord(recordId)
  } catch (e) {
    ElMessage.error('提交审查失败：' + (e.message || ''))
  } finally {
    submitting.value = false
  }
}

async function pollRecord (recordId, maxTry = 20) {
  for (let i = 0; i < maxTry; i++) {
    try {
      const { data } = await getReviewRecord(recordId)
      const status = data?.record?.status
      if (status === 'DONE' || status === 1) {
        currentReview.value = {
          id: data.record.id,
          overallPass: data.record.overallPass === 1 || data.record.overallPass === true,
          errorCount: data.record.errorCount || 0,
          issues: (data.issues || []).map(iss => ({ ...iss }))
        }
        ElMessage[currentReview.value.overallPass ? 'success' : 'warning'](
          currentReview.value.overallPass ? '审查通过' : `审查未通过,共发现 ${currentReview.value.errorCount} 处红色问题`
        )
        return
      }
      await new Promise(r => setTimeout(r, 600))
    } catch (e) {
      console.warn('[review poll] error:', e?.message)
      await new Promise(r => setTimeout(r, 600))
    }
  }
  ElMessage.warning('审查超时,请到审查记录列表查看')
}

const onBatchSubmit = async () => {
  const ids = drafts.value.slice(0, 10).map(d => d.id)
  if (!ids.length) return ElMessage.warning('暂无可审查草案')
  try {
    await batchReview(ids)
    ElMessage.success(`已发起批量审查,共 ${ids.length} 条`)
  } catch (e) {
    ElMessage.error('批量提交失败：' + (e.message || ''))
  }
}

const onResolve = async (row) => {
  try {
    await resolveIssue(row.id)
    row.isResolved = 1
    ElMessage.success('已标记为已解决')
  } catch (e) {
    ElMessage.error('标记失败：' + (e.message || ''))
  }
}

onMounted(() => {
  loadDrafts()
  loadRules()
})
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 12px; }
.draft-list { display: flex; flex-direction: column; gap: 8px; max-height: 540px; overflow: auto; }
.draft-row {
  border: 1px solid $border-light;
  border-radius: 6px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.18s;
  &:hover { border-color: $primary-light; background: $primary-lighter; }
  &.active { border-color: $primary-color; background: $primary-lighter; }
  .dr-title { font-size: 13px; font-weight: 500; }
  .dr-meta  { display: flex; gap: 8px; align-items: center; margin-top: 4px; font-size: 12px; }
}

.summary-row { margin-top: 8px; }
.sum {
  text-align: center;
  padding: 14px 8px;
  border-radius: 6px;
  color: #fff;
  .sum-num   { font-size: 22px; font-weight: 600; }
  .sum-label { font-size: 12px; opacity: 0.85; }
  &.sev-red    { background: linear-gradient(135deg, #fb7185, #f43f5e); }
  &.sev-yellow { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
  &.sev-blue   { background: linear-gradient(135deg, #67e8f9, #06b6d4); }
  &.sev-grey   { background: linear-gradient(135deg, #cbd5e1, #94a3b8); }
}
</style>
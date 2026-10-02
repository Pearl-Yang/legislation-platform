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
            <el-input v-model="searchKw" :icon="Search" placeholder="按项目名 / 草案标题搜索" />
          </div>
          <div class="draft-list">
            <div v-for="d in drafts" :key="d.id"
              class="draft-row"
              :class="{ active: selected?.id === d.id }"
              @click="selected = d"
            >
              <div class="dr-title">{{ d.projectName }} · v{{ d.version }}</div>
              <div class="dr-meta">
                <el-tag size="small">{{ d.generationType === 'AUTO_GENERATED' ? 'AI' : '人工' }}</el-tag>
                <span class="text-secondary">{{ d.wordCount }} 字 · {{ d.updatedAt }}</span>
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
              <el-button :icon="Connection" plain @click="onBatchSubmit">一键审查最近 10 条草案</el-button>
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
            <el-table-column label="条款" prop="articleIndex" width="100" />
            <el-table-column label="问题类型" prop="issueTypeLabel" width="120" />
            <el-table-column label="问题描述" prop="description" min-width="220" />
            <el-table-column label="建议" prop="suggestion" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.isResolved" type="success" size="small">已解决</el-tag>
                <el-tag v-else type="warning" size="small">待处理</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default="{ row }">
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
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, VideoPlay, Connection } from '@element-plus/icons-vue'
import { submitReview, batchReview, listRules, resolveIssue } from '@/api/legislation'
import { severityLabel, severityCls, issueTypeLabel } from '@/utils/dict'

const searchKw = ref('')
const selected = ref({ id: 1001, projectName: '网络数据安全管理条例', version: 3, generationType: 'AUTO_GENERATED', wordCount: 4832, updatedAt: '2026-10-02 14:32' })
const reviewType = ref('AUTO')
const enabledRules = ref([])
const submitting = ref(false)
const currentReview = ref(null)

const drafts = ref([
  { id: 1001, projectName: '网络数据安全管理条例', version: 3, generationType: 'AUTO_GENERATED', wordCount: 4832, updatedAt: '2026-10-02 14:32' },
  { id: 1002, projectName: '某省医疗保障办法',     version: 1, generationType: 'MANUAL',          wordCount: 2104, updatedAt: '2026-09-28 09:11' },
  { id: 1003, projectName: '网络数据安全管理条例', version: 2, generationType: 'AUTO_GENERATED', wordCount: 4320, updatedAt: '2026-09-26 16:48' }
])

const rules = ref([
  { id: 1, ruleName: '上位法冲突检测',   ruleCode: 'R001_SUPERIOR_CONFLICT' },
  { id: 2, ruleName: '越权立法检测',     ruleCode: 'R002_OVER_POWER' },
  { id: 3, ruleName: '引用失效法条',     ruleCode: 'R003_OUTDATED_REF' },
  { id: 4, ruleName: '条文重复检测',     ruleCode: 'R004_DUPLICATE' },
  { id: 5, ruleName: '格式规范检查',     ruleCode: 'R005_FORMAT' },
  { id: 6, ruleName: '语言冗杂检测',     ruleCode: 'R006_VERBOSE' }
])
enabledRules.value = rules.value.map(r => r.id)

const currentIssueStats = computed(() => {
  if (!currentReview.value) return { RED: 0, YELLOW: 0, BLUE: 0, GREY: 0 }
  return currentReview.value.issues.reduce((acc, x) => ({ ...acc, [x.severity]: (acc[x.severity] || 0) + 1 }), {})
})

const onSubmit = async () => {
  if (!selected.value) return ElMessage.warning('请先选择草案')
  submitting.value = true
  // 模拟返回
  setTimeout(() => {
    currentReview.value = {
      id: 20251003, overallPass: false, errorCount: 2,
      issues: [
        { id: 11, severity: 'RED',    issueType: 'SUPERIOR_CONFLICT', issueTypeLabel: '上位法冲突', articleIndex: '第二十条', description: '本条与《数据安全法》第二十一条存在实质性冲突', suggestion: '建议调整处罚对象范围，仅适用本条例所辖事项', isResolved: false },
        { id: 12, severity: 'RED',    issueType: 'OVER_POWER',       issueTypeLabel: '越权立法',   articleIndex: '第十二条', description: '设定了无权设定的行政许可事项', suggestion: '建议删除该许可设置或调整为本条例上位法依据明确的内容', isResolved: false },
        { id: 13, severity: 'YELLOW', issueType: 'OUTDATED_REF',     issueTypeLabel: '引用失效',   articleIndex: '第十五条', description: '引用的《XX 办法》已于 2024 年 12 月被废止', suggestion: '请核对并替换为新的有效规章', isResolved: false },
        { id: 14, severity: 'YELLOW', issueType: 'DUPLICATE',        issueTypeLabel: '条文重复',   articleIndex: '第二十三条', description: '与《XX 条例》第十一条高度相似，相似度 87%', suggestion: '考虑修改表述或删除', isResolved: true },
        { id: 15, severity: 'BLUE',   issueType: 'FORMAT',           issueTypeLabel: '格式不规范', articleIndex: '第七条', description: '条号与上一条不连续', suggestion: '建议调整条号顺序', isResolved: false },
        { id: 16, severity: 'GREY',   issueType: 'VERBOSE',          issueTypeLabel: '语言冗杂',   articleIndex: '第三条', description: '单条字数 412 字，含较多套话', suggestion: '建议拆分或精简表述', isResolved: false }
      ]
    }
    submitting.value = false
  }, 1200)
}

const onBatchSubmit = async () => {
  await batchReview(drafts.value.map(d => d.id))
  ElMessage.success('已发起批量审查任务')
}

const onResolve = async (row) => {
  await resolveIssue(row.id)
  row.isResolved = true
  ElMessage.success('已标记为已解决')
}
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
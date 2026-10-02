<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">全文检索</span>
      <span class="subtitle">命中标题 / 摘要 / 全文 / 关键词，支持高级筛选</span>
    </div>

    <el-card>
      <el-input v-model="keyword" placeholder="输入关键词（支持词组、短语、模糊匹配）" clearable size="large" @keyup.enter="onSearch">
        <template #prepend><el-icon><Search /></el-icon></template>
        <template #append><el-button type="primary" @click="onSearch">搜索</el-button></template>
      </el-input>

      <div class="filter-row mt-16">
        <span>类型：</span>
          <el-tag v-for="t in types" :key="t.value" :type="filter.type === t.value ? 'primary' : 'info'" effect="plain" class="filter-tag" @click="filter.type = filter.type === t.value ? '' : t.value">
            {{ t.label }} ({{ t.count }})
          </el-tag>
        <span class="ml-16">标签：</span>
          <el-tag v-for="t in tags" :key="t" size="small" :type="filter.tag === t ? 'primary' : 'info'" effect="plain" class="filter-tag" @click="filter.tag = filter.tag === t ? '' : t">{{ t }}</el-tag>
      </div>
    </el-card>

    <el-card class="mt-16">
      <template #header>
        <div class="flex-between">
          <span class="title">检索结果（命中 <strong class="text-primary-color">{{ results.length }}</strong> 条）</span>
          <el-radio-group v-model="sortBy" size="small">
            <el-radio-button value="relevance">相关度</el-radio-button>
            <el-radio-button value="date">发布日期</el-radio-button>
            <el-radio-button value="view">浏览量</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div class="result-list">
        <div v-for="r in results" :key="r.id" class="result-row">
          <div class="r-head">
            <span class="type-badge" :class="projectTypeCls(r.regulationType)">{{ materialTypeLabel(r.materialType) }}</span>
            <a class="r-title" @click="$router.push(`/app/library/detail/${r.id}`)" v-html="highlight(r.title, keyword)"></a>
          </div>
          <div class="r-meta">
            <span><el-icon><OfficeBuilding /></el-icon>{{ r.issuingAuthority }}</span>
            <span><el-icon><Calendar /></el-icon>{{ r.issueDate }}</span>
            <span><el-icon><View /></el-icon>{{ r.viewCount }} 次浏览</span>
            <span v-if="r.effectiveDate"><el-icon><CircleCheck /></el-icon>{{ r.effectiveDate }} 生效</span>
          </div>
          <div class="r-digest" v-html="highlight(r.digest + ' ' + (r.fullTextPreview || ''), keyword)"></div>
          <div class="r-tags">
            <el-tag v-for="t in r.tags" :key="t" size="small" effect="plain">{{ t }}</el-tag>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, OfficeBuilding, Calendar, View, CircleCheck } from '@element-plus/icons-vue'
import { projectTypeCls } from '@/utils/dict'
import { materialTypeLabel } from '@/utils/dict'
import { highlightKeyword as highlight } from '@/utils'

const keyword = ref('数据安全')
const filter = ref({ type: '', tag: '' })
const sortBy = ref('relevance')

const types = [
  { value: 'REGULATION',     label: '法规',   count: 128 },
  { value: 'DRAFT',          label: '草案',   count: 67 },
  { value: 'REPORT',         label: '报告',   count: 42 },
  { value: 'EXPERT_OPINION', label: '专家意见', count: 28 },
  { value: 'CASE',           label: '典型案例', count: 53 }
]
const tags = ['数据安全', '行政许可', '行政处罚', '社会保障', '营商环境', '公共安全']

const results = ref([
  { id: 1, materialType: 'REGULATION', regulationType: 'ADMIN_REGULATION', title: '中华人民共和国数据安全法', issuingAuthority: '全国人大常委会', issueDate: '2021-06-10', effectiveDate: '2021-09-01', viewCount: 1283, digest: '为了规范数据处理活动，保障数据安全，促进数据开发利用，保护个人、组织的合法权益。', fullTextPreview: '国家建立数据分类分级保护制度。', tags: ['数据安全', '上位法'] },
  { id: 2, materialType: 'REGULATION', regulationType: 'LOCAL_RULE', title: '某省数据交易管理办法', issuingAuthority: '某省人民政府', issueDate: '2023-03-01', effectiveDate: '2023-04-01', viewCount: 256, digest: '为规范本省行政区域内的数据交易活动，保障数据安全，促进数据要素市场化配置。', fullTextPreview: '数据处理者应当依法保障数据安全 ...', tags: ['数据安全', '数据要素'] },
  { id: 3, materialType: 'EXPERT_OPINION', regulationType: '', title: '中国数字经济发展与法律保障', issuingAuthority: '中国法学会', issueDate: '2024-11-08', viewCount: 198, digest: '数字经济立法趋势、数据产权保护、跨境数据流动等问题的专家观点。', tags: ['数字经济', '数据安全'] },
  { id: 4, materialType: 'REPORT', regulationType: '', title: '《数据安全法》立法说明会会议纪要', issuingAuthority: '全国人大常委会法工委', issueDate: '2021-05-20', viewCount: 312, digest: '记录《数据安全法》立法过程中的主要讨论议题、专家意见和未决事项。', tags: ['数据安全', '立法说明'] }
])

const onSearch = () => {}
</script>

<style lang="scss" scoped>
.filter-row { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; font-size: 13px; color: $text-secondary; }
.filter-tag { cursor: pointer; }
.ml-16 { margin-left: 16px; }
.mt-16 { margin-top: 16px; }

.result-list { display: flex; flex-direction: column; gap: 12px; }
.result-row { padding: 16px; border-radius: 6px; border: 1px solid $border-light; background: #fff; transition: all 0.18s; &:hover { border-color: $primary-lighter; box-shadow: $shadow-card; } }
.r-head { display: flex; gap: 8px; align-items: center; }
.r-title { font-size: 16px; font-weight: 600; color: $primary-color; }
.r-meta { display: flex; gap: 16px; flex-wrap: wrap; font-size: 12px; color: $text-secondary; margin-top: 6px; }
.r-meta span { display: inline-flex; align-items: center; gap: 4px; }
.r-digest { font-size: 13px; color: $text-regular; line-height: 1.7; margin-top: 8px; }
.r-tags { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px; }
</style>
<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">资料浏览</span>
      <span class="subtitle">法规原文 / 历史草案 / 调研报告 / 专家意见 / 典型案例</span>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="6">
        <el-card class="side-card">
          <template #header>
            <span class="title">分类导航</span>
          </template>
          <div class="nav-list">
            <div
              v-for="c in categories"
              :key="c.value"
              class="nav-item"
              :class="{ active: filter.materialType === c.value }"
              @click="filter.materialType = filter.materialType === c.value ? '' : c.value"
            >
              <el-icon><component :is="c.icon" /></el-icon>
              <span>{{ c.label }}</span>
              <span class="text-secondary">{{ c.count }}</span>
            </div>
          </div>

          <el-divider />

          <h4 class="side-title">热门标签</h4>
          <div class="tag-cloud">
            <el-tag
              v-for="t in tags"
              :key="t.name"
              size="small"
              effect="plain"
              class="tag"
              :class="{ active: filter.tag === t.name }"
              @click="filter.tag = filter.tag === t.name ? '' : t.name"
            >
              {{ t.name }} <span class="text-secondary">({{ t.count }})</span>
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="18">
        <el-card>
          <el-form inline>
            <el-form-item label="关键词">
              <el-input v-model="filter.keyword" placeholder="支持模糊搜索" clearable style="width: 240px" @keyup.enter="onSearch" />
            </el-form-item>
            <el-form-item label="地区">
              <el-select v-model="filter.regionCode" placeholder="全部" clearable style="width: 160px">
                <el-option value="000000" label="全国" />
                <el-option value="110000" label="北京" />
                <el-option value="320000" label="江苏" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search">查询</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="mt-16">
          <div class="material-grid">
            <div v-for="m in list" :key="m.id" class="m-card" @click="$router.push(`/app/library/detail/${m.id}`)">
              <div class="m-cover" :style="{ background: m.cover }">
                <el-icon class="m-icon"><component :is="m.icon" /></el-icon>
                <span class="m-type">{{ materialTypeLabel(m.materialType) }}</span>
              </div>
              <div class="m-body">
                <h3 class="m-title">{{ m.title }}</h3>
                <div class="m-meta">
                  <span>{{ m.issuingAuthority }}</span>
                  <span>{{ m.issueDate }}</span>
                </div>
                <div class="m-digest">{{ m.digest }}</div>
                <div class="m-foot">
                  <el-button :icon="StarFilled" size="small" :type="m.favored ? 'warning' : 'default'" plain @click.stop="onFav(m)">
                    {{ m.favored ? '已收藏' : '收藏' }}
                  </el-button>
                  <div class="m-stats">
                    <span><el-icon><View /></el-icon>{{ m.viewCount }}</span>
                    <span><el-icon><Document /></el-icon>{{ m.referenceCount }}</span>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-if="!list.length" description="暂无资料" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, StarFilled, View, Document, Files, EditPen, DataAnalysis, ChatLineRound, Collection } from '@element-plus/icons-vue'
import { listMaterials, favoriteMaterial, unfavoriteMaterial } from '@/api/legislation'
import { materialTypeLabel } from '@/utils/dict'

const filter = ref({ materialType: '', keyword: '', regionCode: '', tag: '' })

const categories = ref([
  { value: 'REGULATION',     label: '法规',   icon: 'Files',          count: 286 },
  { value: 'DRAFT',          label: '草案',   icon: 'EditPen',        count: 154 },
  { value: 'REPORT',         label: '报告',   icon: 'DataAnalysis',   count: 88 },
  { value: 'EXPERT_OPINION', label: '专家意见', icon: 'ChatLineRound', count: 62 },
  { value: 'CASE',           label: '典型案例', icon: 'Collection',   count: 110 }
])

const tags = ref([
  { name: '数据安全', count: 56 },
  { name: '行政许可', count: 32 },
  { name: '行政处罚', count: 48 },
  { name: '社会保障', count: 24 },
  { name: '营商环境', count: 18 },
  { name: '生态保护', count: 16 },
  { name: '公共安全', count: 22 },
  { name: '市场管理', count: 14 }
])

const list = ref([
  { id: 1, title: '中华人民共和国数据安全法', materialType: 'REGULATION', issuingAuthority: '全国人大常委会', issueDate: '2021-06-10', digest: '为了规范数据处理活动，保障数据安全，促进数据开发利用，保护个人、组织的合法权益，维护国家主权、安全和发展利益。', viewCount: 1283, referenceCount: 64, favored: true,  cover: 'linear-gradient(135deg,#4f46e5,#6366f1)', icon: 'Files' },
  { id: 2, title: '某省数据交易管理办法',     materialType: 'REGULATION', issuingAuthority: '某省人民政府',  issueDate: '2023-03-01', digest: '为规范本省行政区域内的数据交易活动，保障数据安全，促进数据要素市场化配置。',  viewCount: 256, referenceCount: 12, favored: false, cover: 'linear-gradient(135deg,#6366f1,#10b981)', icon: 'Files' },
  { id: 3, title: '网络数据安全管理条例（草案征求意见稿）', materialType: 'DRAFT', issuingAuthority: '国务院', issueDate: '2026-09-15', digest: '为规范网络数据处理活动，保障数据安全与合法权益。',  viewCount: 532, referenceCount: 8, favored: false, cover: 'linear-gradient(135deg,#8b5cf6,#a78bfa)', icon: 'EditPen' },
  { id: 4, title: '《数据安全法》立法说明会会议纪要', materialType: 'REPORT', issuingAuthority: '全国人大常委会法工委', issueDate: '2021-05-20', digest: '记录《数据安全法》立法过程中的主要讨论议题、专家意见和未决事项。',  viewCount: 312, referenceCount: 5, favored: true, cover: 'linear-gradient(135deg,#10b981,#4f46e5)', icon: 'DataAnalysis' },
  { id: 5, title: '中国数字经济发展与法律保障', materialType: 'EXPERT_OPINION', issuingAuthority: '中国法学会', issueDate: '2024-11-08', digest: '数字经济立法趋势、数据产权保护、跨境数据流动等问题的专家观点。',  viewCount: 198, referenceCount: 18, favored: false, cover: 'linear-gradient(135deg,#06b6d4,#6366f1)', icon: 'ChatLineRound' },
  { id: 6, title: '某市某区网约车合规化治理', materialType: 'CASE', issuingAuthority: '某市司法局', issueDate: '2025-06-15', digest: '记录某市在网约车合规化治理中的执法实践、典型案例与制度演进。',  viewCount: 142, referenceCount: 6, favored: false, cover: 'linear-gradient(135deg,#f59e0b,#f43f5e)', icon: 'Collection' }
])

const onSearch = () => {}
const onFav = async (m) => {
  if (m.favored) { await unfavoriteMaterial(m.id); m.favored = false; ElMessage.success('已取消收藏') }
  else            { await favoriteMaterial(m.id); m.favored = true; ElMessage.success('已收藏') }
}
</script>

<style lang="scss" scoped>
.side-card { height: 100%; }

.nav-list { display: flex; flex-direction: column; gap: 4px; }
.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.18s;
  &:hover { background: $bg-page; }
  &.active { background: $primary-lighter; color: $primary-color; font-weight: 500; }
}

.side-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; }

.tag-cloud { display: flex; flex-wrap: wrap; gap: 6px; }
.tag { cursor: pointer; transition: all 0.18s; &:hover { background: $primary-lighter; } }
.tag.active { background: $primary-lighter; border-color: $primary-color; color: $primary-color; }

.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.m-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid $border-light;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.18s;
  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-elevated;
    border-color: $primary-lighter;
  }
}
.m-cover {
  height: 100px;
  position: relative;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.m-icon { font-size: 42px; opacity: 0.85; }
.m-type {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 12px;
  background: rgba(255,255,255,0.25);
  padding: 2px 8px;
  border-radius: 4px;
}
.m-body { padding: 12px 14px; }
.m-title { font-size: 14px; font-weight: 600; line-height: 1.4; height: 40px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.m-meta { display: flex; gap: 8px; font-size: 12px; color: $text-secondary; margin-top: 6px; }
.m-digest { font-size: 12px; color: $text-secondary; margin: 8px 0; line-height: 1.6; height: 38px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.m-foot { display: flex; align-items: center; justify-content: space-between; padding-top: 8px; border-top: 1px dashed $border-light; }
.m-stats { display: flex; gap: 12px; font-size: 12px; color: $text-secondary; }
.m-stats span { display: inline-flex; align-items: center; gap: 4px; }
.mt-16 { margin-top: 16px; }
</style>
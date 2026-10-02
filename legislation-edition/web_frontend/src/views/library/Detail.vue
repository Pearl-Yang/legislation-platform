<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <el-button :icon="ArrowLeft" link @click="$router.back()">返回</el-button>
      <span class="title ml-8">{{ material?.title }}</span>
      <el-tag class="ml-8" type="primary" size="small">{{ materialTypeLabel(material?.materialType) }}</el-tag>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="16">
        <el-card>
          <template #header>
            <div class="flex-between">
              <span class="title">正文</span>
              <div>
                <el-button :icon="StarFilled" :type="material?.favored ? 'warning' : 'default'" plain @click="onFav">
                  {{ material?.favored ? '已收藏' : '收藏' }}
                </el-button>
                <el-button :icon="Download" plain>下载原文</el-button>
                <el-button :icon="Share" plain>分享</el-button>
              </div>
            </div>
          </template>

          <el-descriptions :column="3" border class="mb-16">
            <el-descriptions-item label="发布机关">{{ material?.issuingAuthority }}</el-descriptions-item>
            <el-descriptions-item label="发布日期">{{ material?.issueDate }}</el-descriptions-item>
            <el-descriptions-item label="生效日期">{{ material?.effectiveDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="地区">{{ material?.regionName || '全国' }}</el-descriptions-item>
            <el-descriptions-item label="关键词" :span="2">
              <el-tag v-for="t in material?.keywords" :key="t" size="small" effect="plain" class="mr-4">{{ t }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="摘要" :span="3">{{ material?.digest }}</el-descriptions-item>
          </el-descriptions>

          <h3 class="block-title">全文内容</h3>
          <div class="full-text" v-html="fullTextHtml"></div>
        </el-card>

        <el-card class="mt-16">
          <template #header>
            <span class="title">
              <el-icon><EditPen /></el-icon>
              我的批注（{{ notes.length }}）
            </span>
          </template>
          <div class="note-form">
            <el-input v-model="newNote" type="textarea" :rows="3" placeholder="可高亮原文片段并写下笔记 ..." />
            <el-button type="primary" :icon="Promotion" class="mt-8" @click="onAddNote">添加批注</el-button>
          </div>
          <div class="note-list mt-16">
            <div v-for="n in notes" :key="n.id" class="note-row">
              <div class="note-quote">
                <el-icon><ChatLineSquare /></el-icon>
                {{ n.highlightedText }}
              </div>
              <div class="note-content">{{ n.noteContent }}</div>
              <div class="note-meta">
                <span>{{ n.userName }}</span>
                <span>{{ n.createdAt }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card>
          <template #header>
            <span class="title">
              <el-icon><Aim /></el-icon>
              相关推荐
            </span>
          </template>
          <div class="related-list">
            <div v-for="r in related" :key="r.id" class="rel-row" @click="$router.push(`/app/library/detail/${r.id}`)">
              <el-tag size="small" :type="r.score >= 0.85 ? 'danger' : r.score >= 0.7 ? 'warning' : 'info'" effect="plain">
                相关度 {{ (r.score * 100).toFixed(0) }}%
              </el-tag>
              <div class="rel-title">{{ r.title }}</div>
              <div class="rel-meta text-secondary">{{ r.materialTypeLabel }} · {{ r.issuingAuthority }}</div>
            </div>
          </div>
        </el-card>

        <el-card class="mt-16">
          <template #header>
            <span class="title">引用情况</span>
          </template>
          <el-statistic :value="material?.referenceCount || 0" title="被引用次数" />
          <div class="ref-titles mt-12">
            <div v-for="r in referenceList" :key="r.id" class="ref-title-row">
              <el-icon><Right /></el-icon>
              <span>{{ r.title }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, StarFilled, Download, Share, EditPen, ChatLineSquare, Aim, Right
} from '@element-plus/icons-vue'
import { getMaterial, listNotes, addNote, favoriteMaterial, unfavoriteMaterial, relatedMaterials } from '@/api/legislation'
import { materialTypeLabel } from '@/utils/dict'
import { highlightKeyword } from '@/utils'

const route = useRoute()
const loading = ref(false)
const newNote = ref('')

const material = ref({
  id: 1, title: '中华人民共和国数据安全法', materialType: 'REGULATION',
  issuingAuthority: '全国人大常委会', issueDate: '2021-06-10', effectiveDate: '2021-09-01',
  regionName: '全国', keywords: ['数据安全', '数据分类分级', '数据交易', '法律责任'],
  digest: '为了规范数据处理活动，保障数据安全，促进数据开发利用，保护个人、组织的合法权益，维护国家主权、安全和发展利益，制定本法。',
  fullText: `第一章 总则
第一条 为了规范数据处理活动，保障数据安全，促进数据开发利用，保护个人、组织的合法权益，维护国家主权、安全和发展利益，制定本法。
第二条 在中华人民共和国境内开展数据处理活动及其安全监管，适用本法。
第三条 数据安全保护应当坚持统筹发展与安全的原则。

第二章 数据分类分级
第四条 国家建立数据分类分级保护制度。
第五条 国家数据安全工作协调机制统筹协调有关部门制定重要数据目录。

第三章 数据安全保护义务
第六条 从事数据处理活动应当遵守法律、法规，尊重社会公德和伦理道德。
第七条 从事数据处理活动应当加强风险监测，发现数据安全缺陷、漏洞等风险应当立即采取补救措施。
...`,
  referenceCount: 64, favored: true
})

const notes = ref([
  { id: 1, highlightedText: '国家建立数据分类分级保护制度。', noteContent: '建议在地方立法中配套制定具体的分类分级标准', userName: '立法管理员', createdAt: '2026-09-15 14:32' },
  { id: 2, highlightedText: '从事数据处理活动应当加强风险监测', noteContent: '风险监测的具体频率和报告机制值得在配套规章中明确', userName: '立法管理员', createdAt: '2026-09-20 10:11' }
])

const related = ref([
  { id: 3, title: '中华人民共和国网络安全法', score: 0.92, materialTypeLabel: '法规',   issuingAuthority: '全国人大常委会' },
  { id: 5, title: '中国数字经济发展与法律保障', score: 0.78, materialTypeLabel: '专家意见', issuingAuthority: '中国法学会' },
  { id: 4, title: '《数据安全法》立法说明会会议纪要', score: 0.85, materialTypeLabel: '报告',   issuingAuthority: '全国人大常委会法工委' }
])

const referenceList = ref([
  { id: 2, title: '某省数据交易管理办法' },
  { id: 6, title: '某市网络数据管理细则' },
  { id: 7, title: '某行业数据分类分级指南' }
])

const fullTextHtml = computed(() => {
  if (!material.value?.fullText) return ''
  return material.value.fullText.split('\n').map(line => `<p>${line}</p>`).join('')
})

const onFav = async () => {
  if (material.value.favored) {
    await unfavoriteMaterial(material.value.id)
    material.value.favored = false
    ElMessage.success('已取消收藏')
  } else {
    await favoriteMaterial(material.value.id)
    material.value.favored = true
    ElMessage.success('已收藏')
  }
}

const onAddNote = async () => {
  if (!newNote.value.trim()) return ElMessage.warning('请输入批注内容')
  await addNote(material.value.id, { noteContent: newNote.value, highlightedText: '（用户自选文本片段）' })
  notes.value.unshift({
    id: Date.now(), highlightedText: '（用户自选文本片段）',
    noteContent: newNote.value, userName: '立法管理员', createdAt: new Date().toLocaleString()
  })
  newNote.value = ''
  ElMessage.success('批注已保存')
}
</script>

<style lang="scss" scoped>
.ml-8 { margin-left: 8px; }
.mb-16 { margin-bottom: 16px; }
.mt-8 { margin-top: 8px; }
.mt-12 { margin-top: 12px; }
.mt-16 { margin-top: 16px; }
.block-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }

.full-text {
  background: #fafbfc;
  border-radius: 6px;
  padding: 16px 20px;
  font-size: 14px;
  line-height: 2;
  color: $text-regular;
  :deep(p) { margin: 0; }
}

.note-form { background: $bg-page; padding: 12px; border-radius: 6px; }
.note-list { display: flex; flex-direction: column; gap: 10px; }
.note-row { padding: 12px; border: 1px solid $border-light; border-radius: 6px; }
.note-quote {
  background: #fff7d4;
  border-left: 3px solid $accent-amber;
  padding: 6px 10px;
  font-size: 12px;
  color: $text-regular;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.note-content { font-size: 13px; color: $text-primary; margin-top: 8px; line-height: 1.7; }
.note-meta { font-size: 12px; color: $text-secondary; margin-top: 6px; display: flex; gap: 12px; }

.rel-list { display: flex; flex-direction: column; gap: 8px; }
.rel-row {
  padding: 10px 12px;
  border-radius: 6px;
  background: $bg-page;
  cursor: pointer;
  &:hover { background: $primary-lighter; }
}
.rel-title { font-size: 13px; font-weight: 500; margin-top: 4px; }
.rel-meta { font-size: 12px; margin-top: 2px; }

.ref-titles { display: flex; flex-direction: column; gap: 4px; font-size: 13px; }
.ref-title-row { display: flex; gap: 6px; align-items: center; padding: 4px 0; color: $text-regular; }
.ref-title-row .el-icon { color: $text-secondary; }
.mr-4 { margin-right: 4px; }
</style>
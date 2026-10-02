<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">草案版本列表</span>
      <span class="subtitle">支持版本对比、人工修订、导出 Markdown / Word</span>
    </div>

    <el-card>
      <el-form inline>
        <el-form-item label="项目">
          <el-select v-model="filter.projectId" placeholder="全部项目" clearable style="width: 240px">
            <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.projectName" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16">
      <el-table :data="list" stripe>
        <el-table-column label="项目" min-width="160" prop="projectName" />
        <el-table-column label="生成方式" width="120">
          <template #default="{ row }">
            <el-tag :type="row.generationType === 'AUTO_GENERATED' ? 'primary' : 'success'" size="small">
              {{ row.generationType === 'AUTO_GENERATED' ? 'AI 自动' : '人工' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前版本" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="字数" width="100" prop="wordCount" align="right" />
        <el-table-column label="创建人" width="100" prop="createdBy" />
        <el-table-column label="更新时间" width="160" prop="updatedAt" />
        <el-table-column label="操作" width="320" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onView(row)">查看</el-button>
            <el-button link size="small" @click="onCompare(row)">版本对比</el-button>
            <el-button link size="small" @click="onRevise(row)">修订</el-button>
            <el-button link size="small" @click="onExport(row, 'MARKDOWN')">导出MD</el-button>
            <el-button link size="small" @click="onExport(row, 'DOCX')">导出Word</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情 / 对比 / 修订 抽屉 -->
    <el-drawer v-model="viewVisible" :title="`${active?.projectName} · v${active?.version}`" size="640px">
      <pre class="content-block">{{ active?.content }}</pre>
    </el-drawer>

    <el-drawer v-model="compareVisible" title="版本对比" size="80%">
      <div v-if="compareData" class="compare-result">
        <div class="compare-head">
          <el-tag>v{{ compareData.fromVersion }}</el-tag>
          <el-icon class="ml-8 mr-8"><Right /></el-icon>
          <el-tag type="primary">v{{ compareData.toVersion }}</el-tag>
          <span class="ml-12 text-secondary">共 {{ compareData.changes }} 处差异</span>
        </div>
        <pre class="content-block">{{ compareData.diffText }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Right } from '@element-plus/icons-vue'
import { listDrafts, reviseDraft, exportDraft } from '@/api/legislation'
import { downloadBlob } from '@/utils'

const filter = ref({ projectId: '' })

const projects = ref([
  { id: 1, projectName: '网络数据安全管理条例' },
  { id: 2, projectName: '某省医疗保障办法' }
])

const list = ref([
  { id: 1001, projectName: '网络数据安全管理条例', generationType: 'AUTO_GENERATED', version: 3, wordCount: 4832, updatedAt: '2026-10-02 14:32', createdBy: 1, content: '第一章 总则\n第一条 ...' },
  { id: 1002, projectName: '某省医疗保障办法',     generationType: 'MANUAL',          version: 1, wordCount: 2104, updatedAt: '2026-09-28 09:11', createdBy: 2, content: '第一章 总则 ...' },
  { id: 1003, projectName: '网络数据安全管理条例', generationType: 'AUTO_GENERATED', version: 2, wordCount: 4320, updatedAt: '2026-09-26 16:48', createdBy: 1, content: '第一章 总则 ...' }
])

const viewVisible = ref(false)
const compareVisible = ref(false)
const active = ref(null)
const compareData = ref(null)

const onView = (row) => { active.value = row; viewVisible.value = true }
const onCompare = (row) => {
  compareData.value = {
    fromVersion: row.version - 1,
    toVersion: row.version,
    changes: 42,
    diffText: '--- v2\n+++ v3\n@@ 第一章 总则 @@\n- 第一条 ...（v2 内容）\n+ 第一条 ...（v3 内容）\n\n@@ 第三章 权利义务 @@\n- 第七条 ...\n+ 第七条 ... 增加了 XX 表述 ...'
  }
  compareVisible.value = true
}

const onRevise = async (row) => {
  try {
    const { value: content } = await ElMessage.prompt('请输入修订后内容', '人工修订', { inputType: 'textarea' })
    await reviseDraft(row.id, { content })
    ElMessage.success('已保存新版本（v' + (row.version + 1) + '）')
  } catch (e) {}
}

const onExport = async (row, format) => {
  ElMessage.info(`导出 ${format}：/draft/${row.id}/export?format=${format}（演示版返回提示）`)
}
</script>

<style lang="scss" scoped>
.content-block {
  background: #fafbfc;
  border: 1px solid $border-light;
  border-radius: 6px;
  padding: 16px;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  line-height: 1.9;
  color: $text-regular;
  max-height: 70vh;
  overflow: auto;
  white-space: pre-wrap;
}

.compare-head { display: flex; align-items: center; margin-bottom: 12px; }
.ml-8 { margin-left: 8px; }
.mr-8 { margin-right: 8px; }
.ml-12 { margin-left: 12px; }
</style>
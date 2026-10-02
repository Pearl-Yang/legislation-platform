<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">审查记录</span>
      <span class="subtitle">查看历次审查结果，按问题级别聚合展示</span>
    </div>

    <el-card>
      <el-form inline>
        <el-form-item label="项目">
          <el-select v-model="filter.projectId" placeholder="全部" clearable style="width: 220px">
            <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.projectName" />
          </el-select>
        </el-form-item>
        <el-form-item label="审查方式">
          <el-select v-model="filter.reviewType" placeholder="全部" clearable style="width: 140px">
            <el-option value="AUTO" label="自动" />
            <el-option value="MANUAL" label="人工" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="filter.passed" placeholder="全部" clearable style="width: 140px">
            <el-option :value="true" label="通过" />
            <el-option :value="false" label="未通过" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16">
      <el-table :data="list" stripe>
        <el-table-column label="记录 ID" prop="id" width="100" />
        <el-table-column label="所属草案" prop="draftTitle" min-width="200" />
        <el-table-column label="审查方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.reviewType === 'AUTO' ? 'primary' : 'warning'" size="small">
              {{ row.reviewType === 'AUTO' ? '自动' : '人工' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="问题统计" width="240">
          <template #default="{ row }">
            <span class="sev-badge red mr-4">{{ row.redCount }}</span>
            <span class="sev-badge yellow mr-4">{{ row.yellowCount }}</span>
            <span class="sev-badge blue mr-4">{{ row.blueCount }}</span>
            <span class="sev-badge grey">{{ row.greyCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="整体" width="100">
          <template #default="{ row }">
            <el-tag :type="row.overallPass ? 'success' : 'danger'" size="small">
              {{ row.overallPass ? '通过' : '未通过' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审查人" prop="reviewedBy" width="100" />
        <el-table-column label="完成时间" prop="reviewedAt" width="160" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default>
            <el-button link type="primary" size="small">查看详情</el-button>
            <el-button link size="small">下载报告</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

const filter = ref({ projectId: '', reviewType: '', passed: '' })
const projects = ref([
  { id: 1, projectName: '网络数据安全管理条例' },
  { id: 2, projectName: '某省医疗保障办法' }
])

const list = ref([
  { id: 20251003, draftTitle: '网络数据安全管理条例 v3', reviewType: 'AUTO', redCount: 2, yellowCount: 1, blueCount: 1, greyCount: 1, overallPass: false, reviewedBy: 1, reviewedAt: '2026-10-02 14:35' },
  { id: 20251002, draftTitle: '某省医疗保障办法 v1',     reviewType: 'MANUAL', redCount: 0, yellowCount: 2, blueCount: 3, greyCount: 0, overallPass: true,  reviewedBy: 2, reviewedAt: '2026-09-28 10:11' },
  { id: 20250926, draftTitle: '网络数据安全管理条例 v2', reviewType: 'AUTO', redCount: 3, yellowCount: 2, blueCount: 2, greyCount: 1, overallPass: false, reviewedBy: 1, reviewedAt: '2026-09-26 17:02' }
])
</script>

<style lang="scss" scoped>
.mr-4 { margin-right: 4px; }
</style>
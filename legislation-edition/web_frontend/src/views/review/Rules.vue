<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">审查规则配置</span>
      <span class="subtitle">支持规则的启用 / 禁用 / 编辑 / 新增，引擎运行时按规则条件触发</span>
    </div>

    <el-card>
      <el-form inline>
        <el-form-item label="规则类型">
          <el-select v-model="filter.ruleType" placeholder="全部" clearable style="width: 160px">
            <el-option value="LEGAL"       label="合规" />
            <el-option value="CONSISTENCY" label="一致性" />
            <el-option value="RISK"        label="风险" />
            <el-option value="LANGUAGE"    label="语言" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重级别">
          <el-select v-model="filter.severity" placeholder="全部" clearable style="width: 160px">
            <el-option value="RED" label="严重" />
            <el-option value="YELLOW" label="关注" />
            <el-option value="BLUE" label="格式" />
            <el-option value="GREY" label="优化" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16" v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column label="编码" prop="ruleCode" width="220" />
        <el-table-column label="名称" prop="ruleName" min-width="160" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ ruleTypeLabel(row.ruleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <span class="sev-badge" :class="severityCls(row.severity)">{{ severityLabel(row.severity).slice(0, 1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.isEnabled" :loading="row._saving" @change="onToggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default>
            <el-button link type="primary" size="small" @click="onEdit">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无规则数据" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listRules, updateRule } from '@/api/legislation'
import { severityLabel, severityCls } from '@/utils/dict'
import { formatDate } from '@/utils'

const loading = ref(false)
const filter = ref({ ruleType: '', severity: '' })
const list = ref([])

const ruleTypeLabel = (t) => ({ LEGAL: '合规', CONSISTENCY: '一致性', RISK: '风险', LANGUAGE: '语言' })[t] || t

async function loadRules () {
  loading.value = true
  try {
    const params = {}
    if (filter.value.ruleType) params.ruleType = filter.value.ruleType
    if (filter.value.severity) params.severity = filter.value.severity
    const { data } = await listRules(params)
    list.value = (data || []).map(r => ({ ...r, isEnabled: r.isEnabled === 1 || r.isEnabled === true, _saving: false }))
  } catch (e) {
    ElMessage.error('加载规则失败：' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

const onSearch = () => loadRules()

const onToggle = async (row) => {
  row._saving = true
  try {
    await updateRule(row.id, {
      isEnabled: row.isEnabled ? 1 : 0
    })
    ElMessage.success(`${row.ruleName} 已${row.isEnabled ? '启用' : '禁用'}`)
  } catch (e) {
    row.isEnabled = !row.isEnabled
    ElMessage.error('更新失败：' + (e.message || ''))
  } finally {
    row._saving = false
  }
}

const onEdit = () => {
  ElMessage.info('规则详情只读(本阶段不开放编辑表单)')
}

onMounted(loadRules)
</script>

<style lang="scss" scoped>
</style>
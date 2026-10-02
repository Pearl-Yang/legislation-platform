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
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button type="primary" :icon="Plus" @click="onAdd">新增规则</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16">
      <el-table :data="list" stripe>
        <el-table-column label="编码" prop="ruleCode" width="220" />
        <el-table-column label="名称" prop="ruleName" min-width="160" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.ruleTypeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <span class="sev-badge" :class="severityCls(row.severity)">{{ severityLabel(row.severity).slice(0, 1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updatedAt" width="160" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default>
            <el-button link type="primary" size="small">编辑</el-button>
            <el-button link size="small">复制</el-button>
            <el-button link type="danger" size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="addVisible" title="新增审查规则" width="540px">
      <el-form label-width="100px">
        <el-form-item label="规则编码" required>
          <el-input placeholder="R007_xxx" />
        </el-form-item>
        <el-form-item label="规则名称" required>
          <el-input />
        </el-form-item>
        <el-form-item label="规则类型">
          <el-select placeholder="请选择" style="width: 100%">
            <el-option value="LEGAL" label="合规" />
            <el-option value="CONSISTENCY" label="一致性" />
            <el-option value="RISK" label="风险" />
            <el-option value="LANGUAGE" label="语言" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重级别">
          <el-radio-group>
            <el-radio-button value="RED">严重</el-radio-button>
            <el-radio-button value="YELLOW">关注</el-radio-button>
            <el-radio-button value="BLUE">格式</el-radio-button>
            <el-radio-button value="GREY">优化</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="规则表达式" required>
          <el-input type="textarea" :rows="4" placeholder="JSON 表达式，例如 { 'op': 'CONTAINS', 'target': 'punishment_level', 'against': '> 50000' }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="addVisible = false">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { severityLabel, severityCls } from '@/utils/dict'

const filter = ref({ ruleType: '', severity: '' })
const addVisible = ref(false)

const list = ref([
  { id: 1, ruleCode: 'R001_SUPERIOR_CONFLICT', ruleName: '上位法冲突检测', ruleType: 'LEGAL',       ruleTypeLabel: '合规', severity: 'RED',    enabled: true,  updatedAt: '2026-09-30 12:11' },
  { id: 2, ruleCode: 'R002_OVER_POWER',        ruleName: '越权立法检测',   ruleType: 'LEGAL',       ruleTypeLabel: '合规', severity: 'RED',    enabled: true,  updatedAt: '2026-09-30 12:11' },
  { id: 3, ruleCode: 'R003_OUTDATED_REF',      ruleName: '引用失效法条',   ruleType: 'CONSISTENCY', ruleTypeLabel: '一致性', severity: 'YELLOW', enabled: true,  updatedAt: '2026-09-30 12:11' },
  { id: 4, ruleCode: 'R004_DUPLICATE',         ruleName: '条文重复检测',   ruleType: 'CONSISTENCY', ruleTypeLabel: '一致性', severity: 'YELLOW', enabled: true,  updatedAt: '2026-09-30 12:11' },
  { id: 5, ruleCode: 'R005_FORMAT',            ruleName: '格式规范检查',   ruleType: 'LANGUAGE',    ruleTypeLabel: '语言', severity: 'BLUE',   enabled: true,  updatedAt: '2026-09-30 12:11' },
  { id: 6, ruleCode: 'R006_VERBOSE',           ruleName: '语言冗杂检测',   ruleType: 'LANGUAGE',    ruleTypeLabel: '语言', severity: 'GREY',   enabled: false, updatedAt: '2026-09-30 12:11' },
  { id: 7, ruleCode: 'R007_RISK_KEYWORDS',      ruleName: '高风险关键词',   ruleType: 'RISK',        ruleTypeLabel: '风险', severity: 'RED',    enabled: true,  updatedAt: '2026-09-30 12:11' }
])

const onAdd = () => { addVisible.value = true }
</script>
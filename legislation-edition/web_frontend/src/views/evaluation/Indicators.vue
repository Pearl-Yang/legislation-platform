<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">评估指标库</span>
      <span class="subtitle">合法性 / 落实性 / 满意度 三维度指标，权重与公式可调</span>
      <el-button type="primary" :icon="Plus" class="ml-auto">新增指标</el-button>
    </div>

    <el-card>
      <el-table :data="list" stripe>
        <el-table-column label="指标名称" prop="indicatorName" min-width="220" />
        <el-table-column label="维度" width="100">
          <template #default="{ row }">
            <el-tag size="small" :style="{ background: dimensionColor(row.dimension), color: '#fff', borderColor: dimensionColor(row.dimension) }">
              {{ dimensionLabel(row.dimension) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权重" width="100" align="center">
          <template #default="{ row }">{{ (row.weight * 100).toFixed(0) }}%</template>
        </el-table-column>
        <el-table-column label="单位" prop="unit" width="80" align="center" />
        <el-table-column label="数据来源" prop="dataSource" min-width="160" />
        <el-table-column label="计算公式" prop="formula" min-width="220" show-overflow-tooltip />
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default>
            <el-button link type="primary" size="small">编辑</el-button>
            <el-button link size="small">历史</el-button>
            <el-button link type="danger" size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { dimensionLabel, dimensionColor } from '@/utils/dict'

const list = ref([
  { id: 1, indicatorName: '执法合规率', dimension: 'LEGALITY',     weight: 0.20, unit: '比率', dataSource: '行政执法平台', formula: '合法案件数 / 总案件数 × 100%', enabled: true },
  { id: 2, indicatorName: '处罚合法率', dimension: 'LEGALITY',     weight: 0.15, unit: '比率', dataSource: '行政复议结果',   formula: '维持处罚数 / 复议处罚数 × 100%', enabled: true },
  { id: 3, indicatorName: '执法覆盖率', dimension: 'EXECUTION',    weight: 0.15, unit: '比率', dataSource: '行政执法平台', formula: '已覆盖区域数 / 应覆盖区域数 × 100%', enabled: true },
  { id: 4, indicatorName: '执法案件数量', dimension: 'EXECUTION',  weight: 0.10, unit: '绝对值', dataSource: '行政执法平台', formula: 'COUNT(案件)', enabled: true },
  { id: 5, indicatorName: '执法及时率',   dimension: 'EXECUTION',  weight: 0.10, unit: '比率', dataSource: '案件管理系统', formula: '按时办结数 / 案件总数 × 100%', enabled: true },
  { id: 6, indicatorName: '行政相对人满意度', dimension: 'SATISFACTION', weight: 0.20, unit: '比率', dataSource: '政务服务评价系统', formula: '好评数 / 评价总数 × 100%', enabled: true },
  { id: 7, indicatorName: '投诉处理及时率', dimension: 'SATISFACTION', weight: 0.10, unit: '比率', dataSource: '12345 政务热线', formula: '按时回复数 / 投诉总数 × 100%', enabled: true }
])
</script>

<style lang="scss" scoped>
.ml-auto { margin-left: auto; }
</style>
<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">法规关系图谱</span>
      <span class="subtitle">基于 Neo4j 展示上下位 / 引用 / 替代 / 废止 关系</span>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="16">
        <el-card>
          <template #header>
            <div class="flex-between">
              <span class="title">图谱可视化</span>
              <div>
                <el-select v-model="rootId" placeholder="选择根节点" style="width: 280px" @change="loadGraph">
                  <el-option v-for="r in rootOptions" :key="r.id" :value="r.id" :label="r.regulationName" />
                </el-select>
                <el-select v-model="depth" style="width: 100px; margin-left: 8px" @change="loadGraph">
                  <el-option :value="1" label="深度 1" />
                  <el-option :value="2" label="深度 2" />
                  <el-option :value="3" label="深度 3" />
                </el-select>
                <el-button :icon="Refresh" class="ml-8" @click="loadGraph">刷新</el-button>
              </div>
            </div>
          </template>

          <div ref="chartEl" class="graph-chart"></div>

          <div class="legend mt-12">
            <div class="lg-item"><span class="dot" style="background:#4f46e5"></span>上位法</div>
            <div class="lg-item"><span class="dot" style="background:#6366f1"></span>当前法规</div>
            <div class="lg-item"><span class="dot" style="background:#10b981"></span>下位法</div>
            <div class="lg-item"><span class="dot" style="background:#f59e0b"></span>引用</div>
            <div class="lg-item"><span class="dot" style="background:#f43f5e"></span>替代 / 废止</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card>
          <template #header>
            <span class="title">关系明细</span>
          </template>
          <div class="rel-detail" v-if="activeNode">
            <h3 class="rel-name">{{ activeNode.regulationName }}</h3>
            <el-tag size="small" :type="activeNode.status === 'EFFECTIVE' ? 'success' : 'danger'">
              {{ activeNode.status === 'EFFECTIVE' ? '现行有效' : '已废止' }}
            </el-tag>
            <el-descriptions :column="1" border class="mt-12">
              <el-descriptions-item label="法规类型">{{ activeNode.regulationTypeLabel }}</el-descriptions-item>
              <el-descriptions-item label="发文字号">{{ activeNode.issueNumber }}</el-descriptions-item>
              <el-descriptions-item label="生效日期">{{ activeNode.effectiveDate }}</el-descriptions-item>
              <el-descriptions-item label="被引用">{{ activeNode.refCount }} 次</el-descriptions-item>
              <el-descriptions-item label="下游法规">{{ activeNode.downstreamCount }} 部</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>

        <el-card class="mt-16">
          <template #header>
            <span class="title">联动提醒</span>
          </template>
          <ul class="notice-list">
            <li>
              <el-icon class="warn"><Warning /></el-icon>
              《数据安全法》近期修订，预计将影响 12 部下位法
            </li>
            <li>
              <el-icon class="warn"><Warning /></el-icon>
              《XX 办法》已被废止，建议清理 3 部引用法规
            </li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { Refresh, Warning } from '@element-plus/icons-vue'

const chartEl = ref(null)
let chart = null
const rootId = ref(1)
const depth = ref(2)
const activeNode = ref(null)

const rootOptions = ref([
  { id: 1, regulationName: '中华人民共和国数据安全法' },
  { id: 2, regulationName: '中华人民共和国网络安全法' },
  { id: 3, regulationName: '某省数据交易管理办法' }
])

const loadGraph = () => {
  if (!chart) return
  // 真实场景调用：getRegulationRelations(rootId, depth)
  // 模拟数据：
  const option = {
    tooltip: { formatter: (p) => p.data.label || p.name },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      symbolSize: 56,
      label: { show: true, fontSize: 12, color: '#fff' },
      edgeLabel: { show: true, fontSize: 10, color: '#666', formatter: '{c}' },
      force: { repulsion: 600, edgeLength: 120 },
      data: [
        { id: 'A', name: '数据安全法', label: { show: true }, itemStyle: { color: '#4f46e5' } },
        { id: 'B', name: '网络安全法', label: { show: true }, itemStyle: { color: '#4f46e5' } },
        { id: 'C', name: '某省数据交易办法', label: { show: true }, itemStyle: { color: '#6366f1' } },
        { id: 'D', name: '某市网络数据细则', label: { show: true }, itemStyle: { color: '#10b981' } },
        { id: 'E', name: 'XX 部门信息规范', label: { show: true }, itemStyle: { color: '#10b981' } },
        { id: 'F', name: '某行业数据收集指引', label: { show: true }, itemStyle: { color: '#10b981' } },
        { id: 'G', name: '已废止：网络数据旧版', label: { show: true }, itemStyle: { color: '#f43f5e' } }
      ],
      links: [
        { source: 'A', target: 'C', label: { formatter: '上位法依据' }, lineStyle: { color: '#4f46e5' } },
        { source: 'A', target: 'D', label: { formatter: '上位法依据' }, lineStyle: { color: '#4f46e5' } },
        { source: 'B', target: 'C', label: { formatter: '上位法依据' }, lineStyle: { color: '#4f46e5' } },
        { source: 'C', target: 'E', label: { formatter: '引用' }, lineStyle: { color: '#f59e0b' } },
        { source: 'C', target: 'F', label: { formatter: '引用' }, lineStyle: { color: '#f59e0b' } },
        { source: 'D', target: 'G', label: { formatter: '替代' }, lineStyle: { color: '#f43f5e' } }
      ]
    }]
  }
  chart.setOption(option, true)
}

const resize = () => chart?.resize()

onMounted(() => {
  chart = echarts.init(chartEl.value)
  loadGraph()
  window.addEventListener('resize', resize)

  activeNode.value = {
    regulationName: '某省数据交易管理办法',
    regulationTypeLabel: '地方政府规章',
    issueNumber: '省政府令第 318 号',
    effectiveDate: '2023-03-01',
    status: 'EFFECTIVE',
    refCount: 8,
    downstreamCount: 3
  }
})

onUnmounted(() => {
  chart?.dispose()
  window.removeEventListener('resize', resize)
})
</script>

<style lang="scss" scoped>
.graph-chart { width: 100%; height: 540px; border: 1px dashed $border-light; border-radius: 6px; background: #fafbfc; }
.legend { display: flex; gap: 16px; flex-wrap: wrap; font-size: 13px; color: $text-secondary; }
.lg-item { display: inline-flex; align-items: center; gap: 6px; }
.dot { width: 12px; height: 12px; border-radius: 50%; }
.rel-detail .rel-name { font-size: 16px; font-weight: 600; }
.notice-list { list-style: none; padding: 0; margin: 0; }
.notice-list li { padding: 8px 0; font-size: 13px; display: flex; gap: 6px; align-items: flex-start; border-bottom: 1px dashed $border-light; }
.notice-list .warn { color: $accent-amber; margin-top: 2px; }
.ml-8 { margin-left: 8px; }
.mt-12 { margin-top: 12px; }
</style>
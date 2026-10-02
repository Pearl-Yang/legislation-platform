<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

/**
 * 关系图谱(基于 regulation.relation 表)
 *
 * 用法:
 *   <GraphChart :center-id="42" :depth="2" />
 */
const props = defineProps({
  centerId: { type: Number, default: null },
  depth:    { type: Number, default: 1 },
  height:   { type: String, default: '500px' }
})

const chartRef = ref(null)
let chart = null
let nodes = []
let links = []

const colors = {
  SUPERIOR:   '#4f46e5',
  REFERENCE:  '#10b981',
  SUBSTITUTE: '#f59e0b',
  OBSOLETE:   '#f43f5e'
}

async function loadData() {
  if (!props.centerId) return
  try {
    const res = await request.get(`/regulation/${props.centerId}/relations`)
    const data = res.data || res
    nodes = data.nodes || []
    links = data.edges || data.relations || []
    render()
  } catch (e) {
    console.error('GraphChart 加载失败', e)
  }
}

function render() {
  if (!chart) return
  const opt = {
    tooltip: { formatter: (p) => p.dataType === 'edge' ? `${p.data.relationType}: ${p.data.relatedArticle || ''}` : p.data.name },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      force: { repulsion: 200, edgeLength: 80 },
      label: { show: true, position: 'right' },
      edgeSymbol: ['none', 'arrow'],
      edgeLabel: { fontSize: 10 },
      categories: [
        { name: '中心', itemStyle: { color: '#f43f5e' } },
        { name: '上位法', itemStyle: { color: colors.SUPERIOR } },
        { name: '引用',   itemStyle: { color: colors.REFERENCE } }
      ],
      data: nodes.map(n => ({
        ...n,
        category: n.id === props.centerId ? 0 : (n.category || 1),
        symbolSize: n.id === props.centerId ? 50 : 28
      })),
      links: links.map(l => ({
        ...l,
        lineStyle: { color: colors[l.relationType] || '#94a3b8', curveness: 0.2, width: 2 }
      }))
    }]
  }
  chart.setOption(opt, true)
}

onMounted(async () => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  await loadData()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) { chart.dispose(); chart = null }
})

function resize() { if (chart) chart.resize() }

watch(() => props.centerId, () => loadData())
</script>

<template>
  <BaseChart :option="option" :height="height" />
</template>

<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

/**
 * 时间轴 / 流程图(立法项目节点推进展示)。
 *
 * data: [{ name, status, date?, operator? }]
 *   status: DONE / IN_PROGRESS / PENDING / SKIPPED
 */
const props = defineProps({
  data:  { type: Array,  default: () => [] },
  title: { type: String, default: '' },
  height:{ type: String, default: '180px' }
})

const statusColor = {
  DONE:        '#10b981',
  IN_PROGRESS: '#4f46e5',
  PENDING:     '#94a3b8',
  SKIPPED:     '#cbd5e1'
}

const option = computed(() => ({
  title: props.title ? { text: props.title, left: 'center' } : undefined,
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: props.data.map(d => d.name),
    axisLabel: { interval: 0, rotate: 0 }
  },
  yAxis: { show: false },
  series: [{
    type: 'scatter',
    symbolSize: 36,
    itemStyle: {
      color: (p) => statusColor[props.data[p.dataIndex]?.status] || '#94a3b8',
      borderColor: '#fff',
      borderWidth: 2
    },
    label: {
      show: true,
      formatter: (p) => {
        const s = props.data[p.dataIndex]?.status
        return { DONE: '✓', IN_PROGRESS: '●', PENDING: '○', SKIPPED: '—' }[s] || '?'
      },
      color: '#fff', fontWeight: 'bold'
    },
    data: props.data.map((_, i) => i)
  }]
}))
</script>

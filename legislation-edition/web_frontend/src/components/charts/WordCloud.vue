<template>
  <BaseChart :option="option" :height="height" />
</template>

<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

/**
 * 简易词云(不引入 echarts-wordcloud,用气泡图 + size 模拟)。
 * 业务场景:意见关键词分布、热门标签等。
 *
 * data: [{ name, value }]
 */
const props = defineProps({
  data:  { type: Array,  default: () => [] },
  title: { type: String, default: '' },
  height:{ type: String, default: '320px' }
})

const option = computed(() => {
  const max = Math.max(1, ...props.data.map(d => d.value || 1))
  return {
    title: props.title ? { text: props.title, left: 'center' } : undefined,
    tooltip: { trigger: 'item', formatter: (p) => `${p.data.name}: ${p.data.value}` },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: false,
      animation: false,
      label: { show: true, color: '#fff' },
      force: { repulsion: 80, edgeLength: 30 },
      data: props.data.map(d => ({
        name: d.name,
        value: d.value,
        symbolSize: 20 + (d.value / max) * 60,
        itemStyle: { color: ['#4f46e5','#10b981','#f59e0b','#f43f5e','#8b5cf6'][Math.floor(Math.random() * 5)] },
        label: { fontSize: 12 + Math.floor((d.value / max) * 12) }
      }))
    }]
  }
})
</script>

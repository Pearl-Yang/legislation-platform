<template>
  <BaseChart :option="option" :height="height" />
</template>

<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps({
  /** [{ name, value, max? }] */
  data:  { type: Array,  default: () => [] },
  title: { type: String, default: '' },
  height:{ type: String, default: '320px' }
})

const option = computed(() => {
  const max = (props.data[0] && props.data[0].max) || 100
  return {
    title: props.title ? { text: props.title, left: 'center' } : undefined,
    tooltip: { trigger: 'item' },
    radar: {
      indicator: props.data.map(d => ({ name: d.name, max: d.max || max }))
    },
    series: [{
      type: 'radar',
      data: [{
        value: props.data.map(d => d.value),
        name: '评估',
        areaStyle: { color: 'rgba(79, 70, 229, 0.35)' },
        lineStyle:  { color: '#4f46e5', width: 2 },
        itemStyle:  { color: '#4f46e5' }
      }]
    }]
  }
})
</script>

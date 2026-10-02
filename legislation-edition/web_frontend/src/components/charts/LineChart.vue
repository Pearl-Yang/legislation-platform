<template>
  <BaseChart :option="option" :height="height" />
</template>

<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps({
  /** [{ period, score, date? }] */
  data:  { type: Array,  default: () => [] },
  title: { type: String, default: '' },
  height:{ type: String, default: '320px' }
})

const option = computed(() => ({
  title: props.title ? { text: props.title, left: 'center' } : undefined,
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: props.data.map(d => d.period)
  },
  yAxis: { type: 'value', min: 0, max: 100 },
  series: [{
    name: '综合得分',
    type: 'line',
    data: props.data.map(d => d.score),
    smooth: true,
    lineStyle:  { color: '#4f46e5', width: 3 },
    itemStyle:  { color: '#4f46e5' },
    areaStyle: { color: 'rgba(64,158,255,0.2)' },
    label:     { show: true, formatter: '{c}' }
  }]
}))
</script>

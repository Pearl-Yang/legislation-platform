<template>
  <BaseChart :option="option" :height="height" />
</template>

<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps({
  /** xAxis: ['类别A','类别B',...]   series: [{ name, data: [10,20] }] */
  xAxis:  { type: Array,  default: () => [] },
  series: { type: Array,  default: () => [{ name: '数量', data: [] }] },
  title:  { type: String, default: '' },
  height: { type: String, default: '320px' },
  horizontal: { type: Boolean, default: false }
})

const option = computed(() => ({
  title: props.title ? { text: props.title, left: 'center' } : undefined,
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: props.horizontal
    ? { type: 'value' }
    : { type: 'category', data: props.xAxis },
  yAxis: props.horizontal
    ? { type: 'category', data: props.xAxis }
    : { type: 'value' },
  series: props.series.map(s => ({ type: 'bar', name: s.name, data: s.data }))
}))
</script>

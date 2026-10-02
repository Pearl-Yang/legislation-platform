<template>
  <BaseChart :option="option" :height="height" />
</template>

<script setup>
import { computed } from 'vue'
import BaseChart from './BaseChart.vue'

const props = defineProps({
  /** [{ name, value }] */
  data:   { type: Array,  default: () => [] },
  title:  { type: String, default: '' },
  height: { type: String, default: '320px' },
  type:   { type: String, default: 'pie' } // 'pie' | 'doughnut'
})

const option = computed(() => ({
  title: props.title ? { text: props.title, left: 'center' } : undefined,
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: props.type === 'doughnut' ? ['40%', '70%'] : '70%',
    data: props.data,
    label: { formatter: '{b}: {d}%' }
  }]
}))
</script>

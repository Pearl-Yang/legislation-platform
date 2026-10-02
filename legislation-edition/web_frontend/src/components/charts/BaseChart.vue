<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { useChart } from '@/composables/useChart'

const props = defineProps({
  /** ECharts option 对象 */
  option: { type: Object, required: true },
  /** 高度,默认 320px */
  height: { type: String, default: '320px' },
  /** 是否完全替换 option(默认 merge) */
  notMerge: { type: Boolean, default: false }
})

const { chartRef, setOption } = useChart()

import { watch } from 'vue'
watch(
  () => props.option,
  (val) => {
    if (val) setOption(val, props.notMerge)
  },
  { deep: true, immediate: true }
)
</script>

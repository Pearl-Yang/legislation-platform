<template>
  <div class="trend-chart">
    <svg :viewBox="`0 0 ${w} ${h}`" :width="w" :height="h" preserveAspectRatio="none">
      <!-- 横向网格线 -->
      <g class="grid">
        <line v-for="i in 4" :key="i" :x1="padL" :x2="w - padR"
              :y1="padT + ((h - padT - padB) / 4) * i"
              :y2="padT + ((h - padT - padB) / 4) * i"
              stroke="#f1f5f9" stroke-width="1" />
      </g>
      <!-- X 轴标签 -->
      <g class="x-axis">
        <text v-for="(lab, i) in data.labels" :key="lab"
              :x="xAt(i)" :y="h - 6"
              fill="#94a3b8" font-size="10" text-anchor="middle">{{ lab }}</text>
      </g>
      <!-- 每条线 -->
      <g v-for="s in data.series" :key="s.name">
        <path :d="areaPath(s.data)" :fill="s.color" fill-opacity="0.12" />
        <path :d="linePath(s.data)" :stroke="s.color" stroke-width="2.5" fill="none" stroke-linejoin="round" stroke-linecap="round" />
        <g>
          <circle v-for="(v, i) in s.data" :key="i" :cx="xAt(i)" :cy="yAt(v)" r="3.5"
                  :fill="s.color" stroke="#fff" stroke-width="1.5" />
        </g>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const w = 520
const h = 240
const padL = 28
const padR = 12
const padT = 12
const padB = 26

const allValues = computed(() => props.data.series.flatMap(s => s.data))
const maxV = computed(() => Math.max(...allValues.value, 1) * 1.15)
const n = computed(() => props.data.labels.length)

const xAt = (i) => padL + (i * (w - padL - padR)) / Math.max(n.value - 1, 1)
const yAt = (v) => padT + (h - padT - padB) * (1 - v / maxV.value)

const linePath = (arr) => arr.map((v, i) => `${i === 0 ? 'M' : 'L'}${xAt(i)},${yAt(v)}`).join(' ')
const areaPath = (arr) => {
  const top = arr.map((v, i) => `${i === 0 ? 'M' : 'L'}${xAt(i)},${yAt(v)}`).join(' ')
  const lastX = xAt(arr.length - 1)
  const firstX = xAt(0)
  return `${top} L${lastX},${h - padB} L${firstX},${h - padB} Z`
}
</script>

<style scoped>
.trend-chart { width: 100%; }
.trend-chart svg { width: 100%; height: 240px; display: block; }
</style>

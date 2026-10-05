<template>
  <div class="pie-donut">
    <svg :viewBox="`0 0 ${size} ${size}`" :width="size" :height="size">
      <g :transform="`translate(${size / 2}, ${size / 2})`">
        <path v-for="(seg, i) in segments" :key="i"
              :d="seg.path"
              :fill="seg.color"
              :stroke="'#fff'" stroke-width="2" />
        <circle r="36" fill="#fff" />
        <text y="-2" text-anchor="middle" fill="#0f172a" font-size="18" font-weight="700">{{ total }}</text>
        <text y="14" text-anchor="middle" fill="#94a3b8" font-size="10">法规总数</text>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Array, required: true } // [{ name, value, color }]
})

const size = 200
const r = 80
const ir = 52

const total = computed(() => props.data.reduce((a, b) => a + b.value, 0))

const segments = computed(() => {
  let start = -Math.PI / 2
  return props.data.map(d => {
    const angle = (d.value / total.value) * Math.PI * 2
    const end = start + angle
    const path = arcPath(0, 0, r, ir, start, end)
    const seg = { path, color: d.color, name: d.name, value: d.value }
    start = end
    return seg
  })
})

function arcPath (cx, cy, ro, ri, startA, endA) {
  const large = endA - startA > Math.PI ? 1 : 0
  const x1 = cx + ro * Math.cos(startA)
  const y1 = cy + ro * Math.sin(startA)
  const x2 = cx + ro * Math.cos(endA)
  const y2 = cy + ro * Math.sin(endA)
  const x3 = cx + ri * Math.cos(endA)
  const y3 = cy + ri * Math.sin(endA)
  const x4 = cx + ri * Math.cos(startA)
  const y4 = cy + ri * Math.sin(startA)
  return `M${x1},${y1} A${ro},${ro} 0 ${large} 1 ${x2},${y2} L${x3},${y3} A${ri},${ri} 0 ${large} 0 ${x4},${y4} Z`
}
</script>

<style scoped>
.pie-donut { width: 100%; display: flex; justify-content: center; }
.pie-donut svg { width: 200px; height: 200px; display: block; }
</style>

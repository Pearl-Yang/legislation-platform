<template>
  <div class="graph-view">
    <svg :viewBox="`0 0 ${w} ${h}`" :width="w" :height="h" preserveAspectRatio="xMidYMid meet">
      <g>
        <line v-for="(l, i) in links" :key="i"
              :x1="pos(l.source).x" :y1="pos(l.source).y"
              :x2="pos(l.target).x" :y2="pos(l.target).y"
              stroke="rgba(99,102,241,0.35)" stroke-width="1" />
      </g>
      <g>
        <g v-for="n in nodes" :key="n.id" :transform="`translate(${pos(n.id).x}, ${pos(n.id).y})`">
          <circle r="14" :fill="colorOf(n.group)" :stroke="'#fff'" stroke-width="2" />
          <text y="4" text-anchor="middle" font-size="10" fill="#fff" font-weight="700">{{ short(n.label) }}</text>
        </g>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  nodes: { type: Array, required: true },
  links: { type: Array, required: true }
})

const w = 520
const h = 240

const colorMap = { up: '#4f46e5', cur: '#10b981', down: '#f59e0b' }
const colorOf = (g) => colorMap[g] || '#94a3b8'

// 确定性布局：按 group 分层（Y），按 id 顺序排（X），加点偏移
const positions = computed(() => {
  const m = {}
  const groups = ['up', 'cur', 'down']
  groups.forEach((g, gi) => {
    const list = props.nodes.filter(n => n.group === g)
    const y = 30 + gi * 90
    list.forEach((n, i) => {
      const total = list.length
      const x = 60 + (i * (w - 120)) / Math.max(total - 1, 1)
      m[n.id] = { x, y }
    })
  })
  return m
})

const pos = (id) => positions.value[id] || { x: w / 2, y: h / 2 }
const short = (label) => label.length > 4 ? label.slice(0, 4) : label
</script>

<style scoped>
.graph-view { width: 100%; }
.graph-view svg { width: 100%; height: 240px; display: block; }
</style>

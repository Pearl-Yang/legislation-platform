<template>
  <div class="bar-chart">
    <svg :viewBox="`0 0 ${w} ${h}`" :width="w" :height="h" preserveAspectRatio="none">
      <line :x1="padL" :x2="w - padR" :y1="h - padB" :y2="h - padB" stroke="#e2e8f0" />
      <line v-for="i in 4" :key="i" :x1="padL" :x2="w - padR"
            :y1="h - padB - (i * (h - padT - padB) / 4)"
            :y2="h - padB - (i * (h - padT - padB) / 4)"
            stroke="#f1f5f9" />
      <g v-for="(v, i) in data.data" :key="i">
        <rect :x="xAt(i) - barW / 2" :y="yAt(v)" :width="barW" :height="(h - padB) - yAt(v)"
              :fill="color" rx="4" />
        <text :x="xAt(i)" :y="h - padB + 14" text-anchor="middle" fill="#94a3b8" font-size="10">{{ data.labels[i] }}</text>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Object, required: true }, // { labels, data }
  color: { type: String, default: '#06b6d4' }
})

const w = 480
const h = 220
const padL = 30
const padR = 12
const padT = 10
const padB = 26
const barW = 18

const n = computed(() => props.data.labels.length)
const maxV = computed(() => Math.max(...props.data.data, 1) * 1.15)

const xAt = (i) => padL + (i * (w - padL - padR)) / Math.max(n.value - 1, 1) - barW / 2 + (barW / 2)
const yAt = (v) => h - padB - (v / maxV.value) * (h - padT - padB)
</script>

<style scoped>
.bar-chart { width: 100%; }
.bar-chart svg { width: 100%; height: 220px; display: block; }
</style>

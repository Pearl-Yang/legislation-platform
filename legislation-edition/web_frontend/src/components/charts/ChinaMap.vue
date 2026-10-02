<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

/**
 * 中国省级地图(echarts 内置 china 地图)。
 * 加载 /info/map/regulation 返回 [{name, value, code}]。
 */
const props = defineProps({
  dataUrl:  { type: String, default: '/info/map/regulation' },
  height:   { type: String, default: '500px' }
})

const chartRef = ref(null)
let chart = null

async function load() {
  try {
    const res = await request.get(props.dataUrl)
    const list = (res.data && res.data.data) || (res.data) || []
    render(list)
  } catch (e) {
    console.error('ChinaMap 加载失败', e)
  }
}

function render(list) {
  if (!chart) return
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 部' },
    visualMap: {
      min: 0, max: Math.max(10, ...list.map(d => d.value || 0)),
      left: 'left', top: 'bottom',
      inRange: { color: ['#eef2ff', '#6366f1', '#4f46e5'] },
      text: ['多', '少']
    },
    series: [{
      type: 'map',
      map: 'china',
      roam: true,
      label: { show: true, fontSize: 9 },
      data: list
    }]
  })
}

onMounted(async () => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  // 加载 china 地图(从本地 public 目录,或 unpkg CDN)
  try {
    const resp = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json')
    const geo = await resp.json()
    echarts.registerMap('china', geo)
  } catch (e) {
    console.warn('China 地图加载失败,使用内置空地图:', e)
  }
  await load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) { chart.dispose(); chart = null }
})

function resize() { if (chart) chart.resize() }
watch(() => props.dataUrl, () => load())
</script>

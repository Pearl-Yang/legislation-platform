import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref, shallowRef } from 'vue'

/**
 * ECharts 通用 hook。
 *
 * 用法:
 *   const { chartRef, setOption, dispose, resize } = useChart()
 *   setOption({...})
 *
 * 设计:
 *  - shallowRef 避免 ECharts 实例被深度代理
 *  - 自动监听 window resize
 *  - 卸载时自动 dispose
 */
export function useChart(initialOption = null) {
  const chartRef = ref(null)
  const instance = shallowRef(null)

  function init() {
    if (!chartRef.value) return
    instance.value = echarts.init(chartRef.value)
    if (initialOption) {
      instance.value.setOption(initialOption)
    }
    return instance.value
  }

  function setOption(option, notMerge = false) {
    if (!instance.value) init()
    if (instance.value) instance.value.setOption(option, notMerge)
  }

  function resize() {
    if (instance.value) instance.value.resize()
  }

  function dispose() {
    if (instance.value) {
      instance.value.dispose()
      instance.value = null
    }
  }

  function getInstance() {
    return instance.value
  }

  onMounted(() => {
    init()
    window.addEventListener('resize', resize)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', resize)
    dispose()
  })

  return { chartRef, setOption, resize, dispose, getInstance }
}

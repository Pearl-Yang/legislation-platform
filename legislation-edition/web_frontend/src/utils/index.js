/**
 * 通用工具函数
 */

import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'

/** 日期格式化 */
export const formatDate = (d, fmt = 'YYYY-MM-DD') => dayjs(d).format(fmt)
export const formatDateTime = (d) => dayjs(d).format('YYYY-MM-DD HH:mm')

/** 距今天数 (负数 = 已过期) */
export const daysFromNow = (d) => {
  if (!d) return null
  return dayjs(d).startOf('day').diff(dayjs().startOf('day'), 'day')
}

/** 数字千分位 */
export const formatNumber = (n) => {
  if (n == null) return '-'
  return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/** 文件大小格式化 */
export const formatFileSize = (bytes) => {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

/** 下载 blob 为文件 */
export const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

/**
 * 从 axios blob 响应里自动取服务端给的 Content-Disposition 文件名并触发下载。
 * - response: axios 响应对象（responseType: 'blob'）
 * - fallbackName: 兜底文件名
 */
export const downloadFromResponse = (response, fallbackName) => {
  const blob = response?.data
  if (!blob) {
    ElMessage?.error?.('下载失败：响应为空')
    return
  }
  // 错误流:服务端可能返回 application/json 的 {code,message}
  if (blob.type && blob.type.includes('application/json') && blob.size < 4096) {
    // 转成文本读 message
    const reader = new FileReader()
    reader.onload = () => {
      try {
        const j = JSON.parse(reader.result)
        ElMessage?.error?.(j.message || '导出失败')
      } catch { /* ignore */ }
    }
    reader.readAsText(blob)
    return
  }
  let name = fallbackName
  const dispo = response.headers?.['content-disposition'] || response.headers?.['Content-Disposition']
  if (dispo) {
    const m = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(dispo)
    if (m) {
      try { name = decodeURIComponent(m[1] || m[2] || fallbackName) } catch { name = m[2] || fallbackName }
    }
  }
  downloadBlob(blob, name)
}

/** 高亮关键词 */
export const highlightKeyword = (text, keyword) => {
  if (!keyword) return text
  const reg = new RegExp(keyword, 'gi')
  return text.replace(reg, (m) => `<mark style="background:#fff3a3;color:#000;padding:0 2px;border-radius:2px">${m}</mark>`)
}

/** 防抖 */
export const debounce = (fn, delay = 300) => {
  let timer
  return (...args) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

/** 提取后端 Result 包壳: {code, message, data} */
export const unwrap = (resp) => {
  if (resp == null) return {}
  if (resp.data !== undefined && resp.data !== null && typeof resp.data === 'object') return resp.data
  return resp
}

/** 模拟异步任务轮询 */
export const pollTask = async (fn, interval = 1500, maxTimes = 30) => {
  for (let i = 0; i < maxTimes; i++) {
    const r = await fn()
    if (r && (r.status === 'SUCCESS' || r.status === 'FAILED' || r.status === 'COMPLETED' || r.status === 'DONE')) return r
    await new Promise(res => setTimeout(res, interval))
  }
  throw new Error('轮询超时')
}
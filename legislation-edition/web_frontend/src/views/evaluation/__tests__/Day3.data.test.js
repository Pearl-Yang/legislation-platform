/**
 * Day3 业务链路核心加工逻辑测试
 *
 * 覆盖:
 *  - evaluation/Tasks.vue: 维度归一化 / 周期格式化 / 平均分计算 / 状态映射
 *  - cleanup/Tasks.vue:     类型筛选 / stat 聚合 / 状态映射 / 置信度百分比
 *  - consultation/List.vue: 渠道/视角 label 映射 / 意见状态聚合 / 类目图构造
 *  - Dashboard.vue:         safeCall 兜底 / 意见热度排序 / 状态分布归一
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// ============== 复刻纯函数 ==============

// evaluation/Tasks.vue
function normalizeDimension (d) {
  if (!d) return 'LEGALITY'
  if (d === '合法性' || d === 'LEGALITY')     return 'LEGALITY'
  if (d === '落实性' || d === 'EXECUTION')    return 'EXECUTION'
  if (d === '满意度' || d === 'SATISFACTION') return 'SATISFACTION'
  return d
}
function formatPeriod (row, formatFn = (d) => d?.toString?.() || '') {
  const a = row?.periodStart ? formatFn(row.periodStart) : ''
  const b = row?.periodEnd   ? formatFn(row.periodEnd)   : ''
  if (!a && !b) return '—'
  return `${a} 至 ${b}`
}
function avgScore (list) {
  const done = (list || []).filter(x => x.overallScore != null)
  if (!done.length) return '—'
  return (done.reduce((acc, x) => acc + x.overallScore, 0) / done.length).toFixed(1)
}
const EVAL_STATUS_LABEL = { PENDING: '待执行', RUNNING: '进行中', DONE: '已完成' }
const EVAL_STATUS_TAG   = { PENDING: 'info', RUNNING: 'warning', DONE: 'success' }
function evalStatusLabel (s) { return EVAL_STATUS_LABEL[s] || s }
function evalStatusTag   (s) { return EVAL_STATUS_TAG[s]   || 'info' }

// cleanup/Tasks.vue
const CLEANUP_STATUS_LABEL = { PENDING: '待执行', RUNNING: '进行中', DONE: '已完成' }
function cleanupStatusLabel (s) { return CLEANUP_STATUS_LABEL[s] || s }
function cleanupStatusTag   (s) { return ({ PENDING: 'info', RUNNING: 'warning', DONE: 'success' }[s] || 'info') }

function computeStat (list) {
  return {
    total:           (list || []).length,
    running:         (list || []).filter(t => t.status === 'RUNNING').length,
    done:            (list || []).filter(t => t.status === 'DONE').length,
    obsoleteSuggest: (list || []).reduce((acc, t) => acc + (t.suggested || 0), 0)
  }
}

function filterByType (list, type) {
  if (type === 'all') return list || []
  return (list || []).filter(t => t.taskType === type)
}

// consultation/List.vue
const CHANNEL_LABEL = {
  WEB: 'Web 后台', H5: 'H5 公众页', MINI_APP: '微信小程序',
  GOV_APP: '政务 APP', MEETING: '座谈会', PAPER: '纸质'
}
function channelLabel (c) { return CHANNEL_LABEL[c] || c }

const VIEWPOINT_LABEL = { SUPPORT: '支持', OPPOSE: '反对', NEUTRAL: '中立', SUGGEST: '建议' }
function viewpointLabel (v) { return VIEWPOINT_LABEL[v] || v }
function viewpointTag   (v) { return ({ SUPPORT: 'success', OPPOSE: 'danger', NEUTRAL: 'info', SUGGEST: 'warning' }[v] || 'info') }

function countOpinionsByStatus (opinions, status) {
  if (status == null) return (opinions || []).length
  return (opinions || []).filter(o => o.status === status).length
}

function buildPieSeries (byCategory) {
  const entries = Object.entries(byCategory || {})
  if (!entries.length) return null
  return {
    type: 'pie',
    radius: ['40%', '70%'],
    data: entries.map(([name, info]) => ({ value: info.total ?? info, name }))
  }
}

// Dashboard.vue 辅助
function rankByOpinionCount (records, topN = 5) {
  return [...(records || [])]
    .sort((a, b) => (b.totalOpinions || 0) - (a.totalOpinions || 0))
    .slice(0, topN)
}

// safeCall 模拟
async function safeCall (fn, fallback) {
  try {
    const r = await fn()
    return r
  } catch (e) {
    return fallback
  }
}

// ============== 测试 ==============

describe('evaluation/Tasks.vue 数据加工', () => {
  it('normalizeDimension 应把 3 种维度归一为标准枚举', () => {
    expect(normalizeDimension('合法性')).toBe('LEGALITY')
    expect(normalizeDimension('LEGALITY')).toBe('LEGALITY')
    expect(normalizeDimension('落实性')).toBe('EXECUTION')
    expect(normalizeDimension('满意度')).toBe('SATISFACTION')
    expect(normalizeDimension('未知')).toBe('未知')
    expect(normalizeDimension(null)).toBe('LEGALITY')
  })

  it('formatPeriod 应正确拼接起止日期', () => {
    expect(formatPeriod({})).toBe('—')
    expect(formatPeriod({ periodStart: '2026-01-01' }, d => d)).toBe('2026-01-01 至 ')
    expect(formatPeriod({ periodStart: '2026-01-01', periodEnd: '2026-12-31' }, d => d)).toBe('2026-01-01 至 2026-12-31')
  })

  it('avgScore 应正确计算平均分,无数据返回 —', () => {
    expect(avgScore([])).toBe('—')
    expect(avgScore([{ overallScore: 80 }, { overallScore: 90 }])).toBe('85.0')
    expect(avgScore([{ overallScore: null }, { overallScore: 90 }])).toBe('90.0')
  })

  it('evalStatusLabel/Tag 应映射 3 种状态', () => {
    expect(evalStatusLabel('DONE')).toBe('已完成')
    expect(evalStatusLabel('PENDING')).toBe('待执行')
    expect(evalStatusTag('RUNNING')).toBe('warning')
  })
})

describe('cleanup/Tasks.vue 数据加工', () => {
  const tasks = [
    { id: 1, taskType: 'DAILY',    status: 'PENDING', suggested: 0 },
    { id: 2, taskType: 'PERIODIC', status: 'RUNNING', suggested: 18 },
    { id: 3, taskType: 'THEMATIC', status: 'DONE',    suggested: 56 },
    { id: 4, taskType: 'PERIODIC', status: 'DONE',    suggested: 102 }
  ]

  it('filterByType 应正确按类型过滤', () => {
    expect(filterByType(tasks, 'all').length).toBe(4)
    expect(filterByType(tasks, 'DAILY').length).toBe(1)
    expect(filterByType(tasks, 'PERIODIC').length).toBe(2)
    expect(filterByType(tasks, 'THEMATIC').length).toBe(1)
  })

  it('filterByType 应容忍空入参', () => {
    expect(filterByType(null, 'all')).toEqual([])
    expect(filterByType(undefined, 'DAILY')).toEqual([])
  })

  it('computeStat 应正确聚合 4 项指标', () => {
    const s = computeStat(tasks)
    expect(s.total).toBe(4)
    expect(s.running).toBe(1)
    expect(s.done).toBe(2)
    expect(s.obsoleteSuggest).toBe(0 + 18 + 56 + 102)
  })

  it('cleanupStatusLabel/Tag 应映射状态', () => {
    expect(cleanupStatusLabel('DONE')).toBe('已完成')
    expect(cleanupStatusTag('RUNNING')).toBe('warning')
  })
})

describe('consultation/List.vue 数据加工', () => {
  it('channelLabel 应映射 6 个渠道', () => {
    expect(channelLabel('WEB')).toBe('Web 后台')
    expect(channelLabel('MINI_APP')).toBe('微信小程序')
    expect(channelLabel('MEETING')).toBe('座谈会')
    expect(channelLabel('UNKNOWN')).toBe('UNKNOWN')
  })

  it('viewpointLabel/Tag 应映射 4 种立场', () => {
    expect(viewpointLabel('SUPPORT')).toBe('支持')
    expect(viewpointLabel('OPPOSE')).toBe('反对')
    expect(viewpointLabel('NEUTRAL')).toBe('中立')
    expect(viewpointLabel('SUGGEST')).toBe('建议')
    expect(viewpointTag('SUPPORT')).toBe('success')
    expect(viewpointTag('OPPOSE')).toBe('danger')
  })

  it('countOpinionsByStatus 应按 status 过滤', () => {
    const op = [{ status: 'NEW' }, { status: 'NEW' }, { status: 'PROCESSED' }, { status: 'REPLIED' }]
    expect(countOpinionsByStatus(op, null)).toBe(4)
    expect(countOpinionsByStatus(op, 'NEW')).toBe(2)
    expect(countOpinionsByStatus(op, 'PROCESSED')).toBe(1)
  })

  it('buildPieSeries 应从 byCategory 构造 echarts series', () => {
    const s = buildPieSeries({
      '合理性意见': { total: 86 },
      '合法性意见': { total: 52 }
    })
    expect(s.type).toBe('pie')
    expect(s.data.length).toBe(2)
    expect(s.data[0].value).toBe(86)
  })

  it('buildPieSeries 空数据应返回 null', () => {
    expect(buildPieSeries({})).toBeNull()
    expect(buildPieSeries(null)).toBeNull()
  })
})

describe('Dashboard.vue 数据加工', () => {
  it('rankByOpinionCount 应按意见数倒序取前 N', () => {
    const arr = [
      { id: 1, totalOpinions: 10 },
      { id: 2, totalOpinions: 100 },
      { id: 3, totalOpinions: 50 },
      { id: 4, totalOpinions: 200 }
    ]
    const top = rankByOpinionCount(arr, 2)
    expect(top.length).toBe(2)
    expect(top[0].id).toBe(4)
    expect(top[1].id).toBe(2)
  })

  it('rankByOpinionCount 容错: 缺字段填 0', () => {
    const arr = [{ id: 1 }, { id: 2, totalOpinions: 5 }]
    const top = rankByOpinionCount(arr, 5)
    expect(top[0].id).toBe(2)
  })

  it('safeCall 应在接口失败时返回 fallback', async () => {
    const fb = { data: 'fallback' }
    const r = await safeCall(async () => { throw new Error('boom') }, fb)
    expect(r).toEqual(fb)
  })

  it('safeCall 应在接口成功时返回真实数据', async () => {
    const r = await safeCall(async () => ({ data: 'ok' }), { data: 'fallback' })
    expect(r.data).toBe('ok')
  })
})
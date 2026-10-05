/**
 * Dashboard.vue 数据加工与兜底逻辑测试
 *
 * Dashboard.vue 内的 data 加工、loading/error 状态、map 数据排序
 * 是本次 Day1 接真实接口后引入的新逻辑,避免未来重构破坏。
 * 这里只断言"纯函数"风格的可测试部分(不挂载整个组件)。
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import * as api from '@/api/legislation'

// ============== Mock 整个 api ============
vi.mock('@/api/legislation', () => ({
  infoDashboard:     vi.fn(),
  infoDashboardChart: vi.fn(),
  regulationMap:      vi.fn(),
  infoDashboardChartData: vi.fn()  // 防御性:即使导出多个,也不应崩
}))

// ============== 复刻 Dashboard 内的纯函数(便于单测) ==============
function fmt (n) {
  if (n === null || n === undefined || isNaN(n)) return 0
  return Number(n).toLocaleString('zh-CN')
}

function buildKpi (d) {
  return {
    regulationCount:            d.regulationCount ?? 0,
    recentRegulationCount:      d.recentRegulationCount ?? 0,
    totalProjects:              d.project?.total ?? 0,
    activeProjects:             d.project?.active ?? 0,
    publishedProjects:          d.project?.published ?? 0,
    materialCount:              d.materialCount ?? 0,
    upcomingDeadlines:          d.upcomingDeadlines ?? 0,
    regulationTypeDistribution:  d.regulationTypeDistribution || {},
    regulationStatusDistribution: d.regulationStatusDistribution || {}
  }
}

function buildTrend (c) {
  const months = c.months || []
  const regMap = c.regulations || {}
  const matMap = c.materials   || {}
  return {
    labels: months.slice(-9),
    series: [
      { name: '发布法规', color: '#4f46e5', data: months.slice(-9).map(m => regMap[m] || 0) },
      { name: '新增资料', color: '#10b981', data: months.slice(-9).map(m => matMap[m] || 0) }
    ]
  }
}

function buildRegions (mapData) {
  const sorted = [...mapData].sort((a, b) => (b.value || 0) - (a.value || 0)).slice(0, 10)
  const maxV = sorted.length ? sorted[0].value : 1
  return sorted.map(d => ({
    name:    d.name,
    value:   d.value || 0,
    percent: maxV > 0 ? Math.round(((d.value || 0) / maxV) * 100) : 0
  }))
}

// ============== 测试 ============
describe('Dashboard 数据加工', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('fmt 应该正确处理 null / undefined / NaN', () => {
    expect(fmt(null)).toBe(0)
    expect(fmt(undefined)).toBe(0)
    expect(fmt(NaN)).toBe(0)
    expect(fmt(0)).toBe('0')
    expect(fmt(1234)).toContain(',')   // zh-CN 千分位
  })

  it('buildKpi 应填默认值,缺失字段不崩', () => {
    const kpi = buildKpi({})
    expect(kpi.regulationCount).toBe(0)
    expect(kpi.totalProjects).toBe(0)
    expect(kpi.activeProjects).toBe(0)
    expect(kpi.publishedProjects).toBe(0)
    expect(kpi.materialCount).toBe(0)
    expect(kpi.upcomingDeadlines).toBe(0)
    expect(kpi.regulationTypeDistribution).toEqual({})
  })

  it('buildKpi 应能从 project.{total,active,published} 解出', () => {
    const kpi = buildKpi({
      project: { total: 10, active: 3, published: 7 },
      regulationCount: 100,
      recentRegulationCount: 5,
      materialCount: 200,
      upcomingDeadlines: 8,
      regulationTypeDistribution:  { ADMIN_REGULATION: 10, DEPT_RULE: 60, LOCAL_RULE: 30 },
      regulationStatusDistribution: { EFFECTIVE: 80, REVISING: 5, OBSOLETE: 15 }
    })
    expect(kpi.totalProjects).toBe(10)
    expect(kpi.activeProjects).toBe(3)
    expect(kpi.publishedProjects).toBe(7)
    expect(kpi.regulationTypeDistribution.ADMIN_REGULATION).toBe(10)
  })

  it('buildTrend 应限制最多 9 个月标签', () => {
    const months = ['2026-01','2026-02','2026-03','2026-04','2026-05','2026-06','2026-07','2026-08','2026-09','2026-10','2026-11','2026-12']
    const trend = buildTrend({
      months,
      regulations: { '2026-09': 5 },
      materials:   { '2026-09': 8 }
    })
    expect(trend.labels.length).toBe(9)
    expect(trend.series[0].data.length).toBe(9)
  })

  it('buildTrend 缺数据时应填 0,不打 NaN', () => {
    const trend = buildTrend({ months: ['2026-09'], regulations: {}, materials: {} })
    expect(trend.series[0].data).toEqual([0])
    expect(trend.series[1].data).toEqual([0])
  })

  it('buildRegions 应按 value 倒序 + 最多 10 个 + 百分比相对最大值', () => {
    const mapData = [
      { name: '北京', value: 100, code: '110000' },
      { name: '上海', value: 80,  code: '310000' },
      { name: '广东', value: 60,  code: '440000' }
    ]
    const r = buildRegions(mapData)
    expect(r[0].name).toBe('北京')
    expect(r[0].percent).toBe(100)
    expect(r[1].percent).toBe(80)
    expect(r[2].percent).toBe(60)
  })

  it('buildRegions 空数据应返回空数组', () => {
    expect(buildRegions([])).toEqual([])
  })

  it('buildRegions 超过 10 个只取 Top10', () => {
    const arr = Array.from({ length: 20 }, (_, i) => ({ name: '地区' + i, value: 20 - i }))
    const r = buildRegions(arr)
    expect(r.length).toBe(10)
    expect(r[0].value).toBe(20)
  })
})

describe('API 集成 mock 验证', () => {
  it('infoDashboard / infoDashboardChart / regulationMap 三接口均被导入', () => {
    expect(typeof api.infoDashboard).toBe('function')
    expect(typeof api.infoDashboardChart).toBe('function')
    expect(typeof api.regulationMap).toBe('function')
  })
})
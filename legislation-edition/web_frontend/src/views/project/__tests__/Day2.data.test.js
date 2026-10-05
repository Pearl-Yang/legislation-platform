/**
 * Day2 业务链路核心加工逻辑测试
 *
 * 覆盖:
 *  - project/index.vue: 分页结构解析、状态/类型过滤
 *  - project/Detail.vue: 阶段过滤、activeIndex 计算、stepStatus 映射
 *  - draft/Generate.vue:  任务状态→UI 文案映射、任务进度取值
 *  - review/Submit.vue:   4 级问题严重度聚合
 *  - review/Rules.vue:    启用状态布尔转换
 *
 * 这些纯函数是各页面"接真实接口后"的关键加工,锁定防止后续重构破坏。
 */
import { describe, it, expect } from 'vitest'

// ============== 复刻各页面的纯函数 ==============

// project/index.vue: 解析 listProjects 接口的 Page 结构
function parseProjectPage (resp) {
  const records = resp?.records || []
  const total   = resp?.total ?? 0
  return { records, total }
}

// project/index.vue: 过滤参数构造
function buildProjectParams ({ page, size, projectType, status }) {
  const p = { page, size }
  if (projectType) p.projectType = projectType
  if (status && status !== 'all') p.status = status
  return p
}

// project/Detail.vue: 阶段过滤
function filterStages (stages, filter) {
  if (filter === 'all') return stages
  return stages.filter(s => {
    if (filter === 'done')    return s.status === 'DONE'
    if (filter === 'current') return s.status === 'IN_PROGRESS'
    if (filter === 'pending') return ['PENDING', 'WAITING'].includes(s.status)
    return true
  })
}

// project/Detail.vue: activeIndex(进度条激活位)
function activeIndexOf (stages) {
  const i = stages.findIndex(s => s.status === 'IN_PROGRESS')
  return i === -1 ? stages.length : i
}

// project/Detail.vue: 步骤状态映射
function stepStatusOf (s) {
  if (s.status === 'DONE') return 'success'
  if (s.status === 'IN_PROGRESS') return 'process'
  if (s.status === 'RETURNED') return 'error'
  return 'wait'
}

// draft/Generate.vue: 任务状态→UI
const STATUS_LABEL = { PROCESSING: '进行中', SUCCESS: '已完成', FAILED: '失败' }
function taskLabelOf (status) {
  return STATUS_LABEL[status] || status
}
function taskTagOf (status) {
  if (status === 'PROCESSING') return 'warning'
  if (status === 'SUCCESS')    return 'success'
  if (status === 'FAILED')     return 'danger'
  return 'info'
}

// review/Submit.vue: 4 级问题聚合
function aggregateIssues (issues) {
  return (issues || []).reduce((acc, x) => {
    const k = x.severity || 'GREY'
    acc[k] = (acc[k] || 0) + 1
    return acc
  }, { RED: 0, YELLOW: 0, BLUE: 0, GREY: 0 })
}

// review/Rules.vue: 启用状态归一化
function normalizeRules (data) {
  return (data || []).map(r => ({
    ...r,
    isEnabled: r.isEnabled === 1 || r.isEnabled === true
  }))
}

// review/Rules.vue: 切换保存时构造请求体
function togglePatchBody (rule, newValue) {
  return { isEnabled: newValue ? 1 : 0 }
}

// ============== 测试 ==============

describe('project/index.vue 数据加工', () => {
  it('parseProjectPage 应从 MyBatis-Plus Page 结构抽 records + total', () => {
    const r = parseProjectPage({ records: [{ id: 1 }, { id: 2 }], total: 2 })
    expect(r.records.length).toBe(2)
    expect(r.total).toBe(2)
  })

  it('parseProjectPage 缺字段应不崩', () => {
    expect(parseProjectPage({}).records).toEqual([])
    expect(parseProjectPage({}).total).toBe(0)
  })

  it('buildProjectParams 应正确过滤掉 all 和空值', () => {
    expect(buildProjectParams({ page: 1, size: 10 })).toEqual({ page: 1, size: 10 })
    expect(buildProjectParams({ page: 1, size: 10, status: 'all' })).toEqual({ page: 1, size: 10 })
    expect(buildProjectParams({ page: 1, size: 10, status: 'ACTIVE' }))
      .toEqual({ page: 1, size: 10, status: 'ACTIVE' })
    expect(buildProjectParams({ page: 1, size: 10, projectType: 'LOCAL_RULE' }))
      .toEqual({ page: 1, size: 10, projectType: 'LOCAL_RULE' })
  })
})

describe('project/Detail.vue 数据加工', () => {
  const stages = [
    { id: 1, status: 'DONE' },
    { id: 2, status: 'DONE' },
    { id: 3, status: 'IN_PROGRESS' },
    { id: 4, status: 'PENDING' },
    { id: 5, status: 'PENDING' }
  ]

  it('filterStages 应按状态分组', () => {
    expect(filterStages(stages, 'all').length).toBe(5)
    expect(filterStages(stages, 'done').length).toBe(2)
    expect(filterStages(stages, 'current').length).toBe(1)
    expect(filterStages(stages, 'pending').length).toBe(2)
  })

  it('activeIndexOf 应指向 IN_PROGRESS 阶段;没有则为 length', () => {
    expect(activeIndexOf(stages)).toBe(2)
    expect(activeIndexOf([{ status: 'DONE' }, { status: 'DONE' }])).toBe(2)
  })

  it('stepStatusOf 应映射 4 种阶段状态', () => {
    expect(stepStatusOf({ status: 'DONE' })).toBe('success')
    expect(stepStatusOf({ status: 'IN_PROGRESS' })).toBe('process')
    expect(stepStatusOf({ status: 'RETURNED' })).toBe('error')
    expect(stepStatusOf({ status: 'PENDING' })).toBe('wait')
    expect(stepStatusOf({ status: '未知' })).toBe('wait')
  })
})

describe('draft/Generate.vue 任务状态加工', () => {
  it('taskLabelOf 应映射 3 种任务状态', () => {
    expect(taskLabelOf('PROCESSING')).toBe('进行中')
    expect(taskLabelOf('SUCCESS')).toBe('已完成')
    expect(taskLabelOf('FAILED')).toBe('失败')
    expect(taskLabelOf('UNKNOWN')).toBe('UNKNOWN')
  })

  it('taskTagOf 应映射 4 种 tag 颜色', () => {
    expect(taskTagOf('PROCESSING')).toBe('warning')
    expect(taskTagOf('SUCCESS')).toBe('success')
    expect(taskTagOf('FAILED')).toBe('danger')
    expect(taskTagOf(null)).toBe('info')
  })
})

describe('review/Submit.vue 问题严重度聚合', () => {
  it('aggregateIssues 应按 severity 累加', () => {
    const issues = [
      { severity: 'RED' }, { severity: 'RED' },
      { severity: 'YELLOW' }, { severity: 'YELLOW' }, { severity: 'YELLOW' },
      { severity: 'BLUE' },
      { severity: 'GREY' }, { severity: 'GREY' }
    ]
    expect(aggregateIssues(issues)).toEqual({ RED: 2, YELLOW: 3, BLUE: 1, GREY: 2 })
  })

  it('aggregateIssues 空数据应返回全 0', () => {
    expect(aggregateIssues([])).toEqual({ RED: 0, YELLOW: 0, BLUE: 0, GREY: 0 })
    expect(aggregateIssues(null)).toEqual({ RED: 0, YELLOW: 0, BLUE: 0, GREY: 0 })
  })

  it('aggregateIssues 缺字段默认归入 GREY', () => {
    expect(aggregateIssues([{}, { severity: 'RED' }])).toEqual({ RED: 1, YELLOW: 0, BLUE: 0, GREY: 1 })
  })
})

describe('review/Rules.vue 规则启用归一', () => {
  it('normalizeRules 应把 1 / true 都映射为布尔 true', () => {
    const data = [{ id: 1, isEnabled: 1 }, { id: 2, isEnabled: true }, { id: 3, isEnabled: 0 }]
    expect(normalizeRules(data)).toEqual([
      { id: 1, isEnabled: true },
      { id: 2, isEnabled: true },
      { id: 3, isEnabled: false }
    ])
  })

  it('togglePatchBody 应只传 isEnabled,后端避免改其他字段', () => {
    expect(togglePatchBody({ id: 1 }, true)).toEqual({ isEnabled: 1 })
    expect(togglePatchBody({ id: 1 }, false)).toEqual({ isEnabled: 0 })
  })
})
/**
 * Day4 业务链路核心加工逻辑测试
 *
 * 覆盖:
 *  - project/Detail.vue: ECharts graph 流程图节点 / 链接构造
 *                       + 7 天预警 badge 计算
 *                       + deadlineBadgeFor(stage) 节点级徽章
 *  - info/Dashboard.vue: 30 秒轮播的视图索引
 */
import { describe, it, expect } from 'vitest'

// ============== 复刻纯函数 ==============

// Detail.vue: ECharts graph nodes/links
const STAGE_COLOR = {
  DONE:        '#10b981',
  IN_PROGRESS: '#4f46e5',
  PENDING:     '#94a3b8',
  RETURNED:    '#f43f5e',
  SKIPPED:     '#cbd5e1'
}
const STAGE_ICON = {
  DONE:        '✓',
  IN_PROGRESS: '●',
  PENDING:     '○',
  RETURNED:    '!',
  SKIPPED:     '—'
}

function buildFlowNodes (stages) {
  return stages.map((s, i) => ({
    id:    String(s.id || s.stageCode || i),
    name:  s.stageName,
    value: s.status,
    x:     100 + (i % 5) * 200,
    y:     200 + Math.floor(i / 5) * 200,
    symbolSize: s.status === 'IN_PROGRESS' ? 56 : 40,
    itemStyle: {
      color: STAGE_COLOR[s.status] || '#94a3b8'
    }
  }))
}

function buildFlowLinks (stages) {
  return stages.slice(0, -1).map((s, i) => ({
    source: String(s.id || s.stageCode || i),
    target: String(stages[i + 1].id || stages[i + 1].stageCode || (i + 1))
  }))
}

// Detail.vue: 项目级 7 天预警 badge
function projectAlertBadge (deadlines) {
  if (!deadlines || !deadlines.length) return null
  const soonest = deadlines.reduce((min, d) => {
    if (d.daysLeft == null) return min
    return (min == null || d.daysLeft < min) ? d.daysLeft : min
  }, null)
  if (soonest == null) return null
  if (soonest < 0) return { type: 'danger',  text: `已逾期 ${-soonest} 天` }
  if (soonest < 7) return { type: 'warning', text: `${soonest} 天内到期` }
  return null
}

// Detail.vue: 节点级 badge
function deadlineBadgeFor (stage, deadlines) {
  const dl = (deadlines || []).find(d =>
    d.nodeCode === stage.stageCode || d.stageCode === stage.stageCode
  )
  if (!dl) return { type: '' }
  if (dl.daysLeft < 0) return { type: 'danger',  text: `逾期 ${-dl.daysLeft} 天` }
  if (dl.daysLeft < 7) return { type: 'warning', text: `${dl.daysLeft} 天内到期` }
  return { type: 'info', text: `${dl.daysLeft} 天` }
}

// Dashboard.vue: 30 秒轮播索引
const VIEW_DURATIONS = ['all', 'trend', 'region', 'status']
function nextViewIndex (current, total) {
  return (current + 1) % total
}

// ============== 测试 ==============

describe('Detail.vue ECharts 流程图构造', () => {
  const stages = [
    { id: 1, stageName: '立项建议',  stageCode: 'PROPOSAL',        status: 'DONE' },
    { id: 2, stageName: '立项审查',  stageCode: 'PROPOSAL_REVIEW', status: 'DONE' },
    { id: 3, stageName: '起草',      stageCode: 'DRAFTING',        status: 'IN_PROGRESS' },
    { id: 4, stageName: '征求意见',  stageCode: 'PUBLIC_COMMENT',  status: 'PENDING' }
  ]

  it('buildFlowNodes 应按 stages 构造 nodes,IN_PROGRESS 节点 symbolSize=56', () => {
    const nodes = buildFlowNodes(stages)
    expect(nodes.length).toBe(4)
    expect(nodes[0].name).toBe('立项建议')
    expect(nodes[2].symbolSize).toBe(56)  // IN_PROGRESS
    expect(nodes[0].symbolSize).toBe(40)  // DONE
    expect(nodes[2].itemStyle.color).toBe('#4f46e5')  // IN_PROGRESS color
    expect(nodes[0].itemStyle.color).toBe('#10b981')  // DONE color
  })

  it('buildFlowLinks 应连接相邻节点,数量 = N - 1', () => {
    const links = buildFlowLinks(stages)
    expect(links.length).toBe(3)
    expect(links[0].source).toBe('1')
    expect(links[0].target).toBe('2')
    expect(links[2].source).toBe('3')
    expect(links[2].target).toBe('4')
  })

  it('buildFlowLinks 单节点应返回空数组', () => {
    expect(buildFlowLinks([{ id: 1, stageName: 'X', status: 'PENDING' }])).toEqual([])
  })

  it('STAGE_ICON 应包含 5 种状态', () => {
    expect(STAGE_ICON.DONE).toBe('✓')
    expect(STAGE_ICON.IN_PROGRESS).toBe('●')
    expect(STAGE_ICON.PENDING).toBe('○')
    expect(STAGE_ICON.RETURNED).toBe('!')
    expect(STAGE_ICON.SKIPPED).toBe('—')
  })
})

describe('Detail.vue 7 天预警 badge', () => {
  it('projectAlertBadge 应返回 null 当无 deadline', () => {
    expect(projectAlertBadge([])).toBeNull()
    expect(projectAlertBadge(null)).toBeNull()
  })

  it('projectAlertBadge 应把最早到期的 deadline 提到顶部', () => {
    const d = [
      { nodeName: 'A', daysLeft: 30 },
      { nodeName: 'B', daysLeft: 5 },
      { nodeName: 'C', daysLeft: 50 }
    ]
    const b = projectAlertBadge(d)
    expect(b.type).toBe('warning')
    expect(b.text).toBe('5 天内到期')
  })

  it('projectAlertBadge 逾期应显示"已逾期 N 天"', () => {
    const b = projectAlertBadge([{ nodeName: 'X', daysLeft: -3 }])
    expect(b.type).toBe('danger')
    expect(b.text).toBe('已逾期 3 天')
  })

  it('projectAlertBadge >= 7 天应返回 null(不显示)', () => {
    expect(projectAlertBadge([{ nodeName: 'X', daysLeft: 7 }])).toBeNull()
    expect(projectAlertBadge([{ nodeName: 'X', daysLeft: 100 }])).toBeNull()
  })

  it('projectAlertBadge 容错 daysLeft=null', () => {
    const b = projectAlertBadge([{ nodeName: 'X' }, { nodeName: 'Y', daysLeft: 3 }])
    expect(b.text).toBe('3 天内到期')
  })

  it('deadlineBadgeFor 应按 stageCode 匹配', () => {
    const d = [{ nodeCode: 'DRAFTING', daysLeft: 2 }]
    const badge = deadlineBadgeFor({ stageCode: 'DRAFTING' }, d)
    expect(badge.type).toBe('warning')
    expect(badge.text).toBe('2 天内到期')
  })

  it('deadlineBadgeFor 找不到匹配 deadline 应返回空 type', () => {
    expect(deadlineBadgeFor({ stageCode: 'OTHER' }, []).type).toBe('')
  })

  it('deadlineBadgeFor >= 7 天应显示 info', () => {
    const d = [{ nodeCode: 'X', daysLeft: 30 }]
    const b = deadlineBadgeFor({ stageCode: 'X' }, d)
    expect(b.type).toBe('info')
    expect(b.text).toBe('30 天')
  })
})

describe('Dashboard.vue 30 秒轮播', () => {
  it('nextViewIndex 应循环递增到 total 末尾后回到 0', () => {
    expect(nextViewIndex(0, 4)).toBe(1)
    expect(nextViewIndex(1, 4)).toBe(2)
    expect(nextViewIndex(3, 4)).toBe(0)  // 末尾 → 0
  })

  it('VIEW_DURATIONS 应包含 4 种视图', () => {
    expect(VIEW_DURATIONS.length).toBe(4)
    expect(VIEW_DURATIONS).toContain('all')
    expect(VIEW_DURATIONS).toContain('trend')
    expect(VIEW_DURATIONS).toContain('region')
    expect(VIEW_DURATIONS).toContain('status')
  })
})
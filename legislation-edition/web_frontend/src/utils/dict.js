/**
 * 立法版枚举字典 - 用于前端展示
 */

// 立法项目类型
export const PROJECT_TYPE = {
  ADMIN_REGULATION: { code: 'ADMIN_REGULATION', label: '行政法规',    cls: 'admin' },
  DEPT_RULE:        { code: 'DEPT_RULE',        label: '部门规章',    cls: 'dept'  },
  LOCAL_RULE:       { code: 'LOCAL_RULE',       label: '地方政府规章', cls: 'local' }
}
export const projectTypeOptions = Object.values(PROJECT_TYPE).map(v => ({ value: v.code, label: v.label }))
export const projectTypeLabel = (k) => PROJECT_TYPE[k]?.label || k
export const projectTypeCls   = (k) => PROJECT_TYPE[k]?.cls || ''

// 项目状态
export const PROJECT_STATUS = {
  DRAFT:     { label: '草稿',   tag: 'info' },
  ACTIVE:    { label: '进行中', tag: 'primary' },
  PUBLISHED: { label: '已发布', tag: 'success' },
  OBSOLETE:  { label: '已废止', tag: 'danger' }
}
export const projectStatusLabel = (k) => PROJECT_STATUS[k]?.label || k
export const projectStatusTag   = (k) => PROJECT_STATUS[k]?.tag || 'info'

// 流程节点状态
export const STAGE_STATUS = {
  PENDING:     { label: '待办',     cls: 'pending' },
  IN_PROGRESS: { label: '进行中',   cls: 'in-progress' },
  DONE:        { label: '已完成',   cls: 'done' },
  SKIPPED:     { label: '已跳过',   cls: 'skipped' },
  RETURNED:    { label: '已退回',   cls: 'returned' }
}
export const stageStatusLabel = (k) => STAGE_STATUS[k]?.label || k
export const stageStatusCls   = (k) => STAGE_STATUS[k]?.cls || ''

// 审查严重度（红/黄/蓝/灰）
export const SEVERITY = {
  RED:    { label: '严重冲突', cls: 'red' },
  YELLOW: { label: '需关注',   cls: 'yellow' },
  BLUE:   { label: '格式建议', cls: 'blue' },
  GREY:   { label: '优化提示', cls: 'grey' }
}
export const severityLabel = (k) => SEVERITY[k]?.label || k
export const severityCls   = (k) => SEVERITY[k]?.cls || 'grey'

// 审查问题类型
export const ISSUE_TYPE = {
  SUPERIOR_CONFLICT: '上位法冲突',
  OVER_POWER:       '越权立法',
  OUTDATED_REF:     '引用失效法条',
  DUPLICATE:        '条文重复',
  FORMAT:           '格式不规范',
  VERBOSE:          '语言冗杂'
}
export const issueTypeLabel = (k) => ISSUE_TYPE[k] || k

// 清理任务类型
export const CLEANUP_TYPE = {
  DAILY:    '日常清理',
  PERIODIC: '定期清理',
  THEMATIC: '专项清理'
}
export const cleanupTypeLabel = (k) => CLEANUP_TYPE[k] || k

// 清理建议
export const CLEANUP_SUGGESTION = {
  KEEP:     { label: '保留', tag: 'success' },
  MODIFY:   { label: '修改', tag: 'warning' },
  OBSOLETE: { label: '废止', tag: 'danger' }
}
export const suggestionLabel = (k) => CLEANUP_SUGGESTION[k]?.label || k
export const suggestionTag   = (k) => CLEANUP_SUGGESTION[k]?.tag || 'info'

// 法规效力状态
export const REG_STATUS = {
  EFFECTIVE: { label: '现行有效', tag: 'success' },
  REVISING:  { label: '修订中',   tag: 'warning' },
  OBSOLETE:  { label: '已废止',   tag: 'danger' }
}
export const regStatusLabel = (k) => REG_STATUS[k]?.label || k
export const regStatusTag   = (k) => REG_STATUS[k]?.tag || 'info'

// 资料类型
export const MATERIAL_TYPE = {
  REGULATION:     '法规',
  DRAFT:          '草案',
  REPORT:         '评估报告',
  EXPERT_OPINION: '专家意见',
  CASE:           '典型案例'
}
export const materialTypeLabel = (k) => MATERIAL_TYPE[k] || k

// 评估维度
export const EVAL_DIMENSION = {
  LEGALITY:    { label: '合法性', color: '#4f46e5' },
  EXECUTION:   { label: '落实性', color: '#6366f1' },
  SATISFACTION:{ label: '满意度', color: '#10b981' }
}
export const dimensionLabel = (k) => EVAL_DIMENSION[k]?.label || k
export const dimensionColor = (k) => EVAL_DIMENSION[k]?.color || '#94a3b8'

// 意见处理状态
export const OPINION_STATUS = {
  NEW:       { label: '新提交', tag: 'primary' },
  PROCESSED: { label: '已处理', tag: 'warning' },
  REPLIED:   { label: '已回复', tag: 'success' }
}
export const opinionStatusLabel = (k) => OPINION_STATUS[k]?.label || k
export const opinionStatusTag   = (k) => OPINION_STATUS[k]?.tag || 'info'

// 意见分类（一级）
export const OPINION_CATEGORY_L1 = [
  '合法性意见', '合理性意见', '可行性意见', '操作性意见', '其他意见'
]

// 立法动态分类
export const NEWS_CATEGORY = {
  REGULATION_NEW: '新法规',
  INTERPRET:      '政策解读',
  CASE:           '典型案例',
  LITERATURE:     '学术文献',
  BULLETIN:       '公报简报'
}
export const newsCategoryLabel = (k) => NEWS_CATEGORY[k] || k

// 流程节点 - 行政法规
export const STAGE_TEMPLATE_ADMIN = [
  { code: 'PROPOSAL',        name: '立项建议',         days: 15 },
  { code: 'PROPOSAL_REVIEW', name: '立项审查',         days: 30 },
  { code: 'DRAFTING',        name: '起草',             days: 90 },
  { code: 'PUBLIC_COMMENT',  name: '征求意见',         days: 30 },
  { code: 'EXPERT_REVIEW',   name: '专家论证',         days: 30 },
  { code: 'RISK_ASSESSMENT', name: '社会稳定风险评估', days: 30 },
  { code: 'LEGAL_REVIEW',    name: '法制机构审查',     days: 30 },
  { code: 'DELIBERATION',    name: '审议',             days: 30 },
  { code: 'PUBLICATION',     name: '公布',             days: 15 },
  { code: 'FILING',          name: '备案',             days: 30 },
  { code: 'POST_EVALUATION', name: '立法后评估',       days: 365 }
]

// 流程节点 - 部门规章 / 地方政府规章
export const STAGE_TEMPLATE_RULE = [
  { code: 'PROPOSAL',        name: '立项建议',     days: 15 },
  { code: 'PROPOSAL_REVIEW', name: '立项审查',     days: 30 },
  { code: 'DRAFTING',        name: '起草',         days: 90 },
  { code: 'PUBLIC_COMMENT',  name: '征求意见',     days: 30 },
  { code: 'EXPERT_REVIEW',   name: '专家论证',     days: 30 },
  { code: 'LEGAL_REVIEW',    name: '法制机构审查', days: 30 },
  { code: 'DELIBERATION',    name: '审议',         days: 30 },
  { code: 'PUBLICATION',     name: '公布',         days: 15 },
  { code: 'FILING',          name: '备案',         days: 30 },
  { code: 'POST_EVALUATION', name: '立法后评估',   days: 365 }
]
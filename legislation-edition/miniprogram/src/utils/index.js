/**
 * 小程序端通用工具函数
 */

/** 日期格式化 */
export const formatDate = (d, fmt = 'YYYY-MM-DD') => {
  if (!d) return ''
  const date = typeof d === 'string' || typeof d === 'number' ? new Date(d) : d
  const pad = (n) => (n < 10 ? '0' + n : '' + n)
  return fmt
    .replace('YYYY', date.getFullYear())
    .replace('MM', pad(date.getMonth() + 1))
    .replace('DD', pad(date.getDate()))
    .replace('HH', pad(date.getHours()))
    .replace('mm', pad(date.getMinutes()))
    .replace('ss', pad(date.getSeconds()))
}

/** 距今天数 (负数 = 已过期) */
export const daysFromNow = (d) => {
  if (!d) return null
  const t = typeof d === 'string' ? new Date(d) : d
  return Math.floor((t.getTime() - Date.now()) / 86400000)
}

/** 项目类型 */
export const PROJECT_TYPE_LABEL = {
  ADMIN_REGULATION: { label: '行政法规', cls: 'admin' },
  DEPT_RULE:        { label: '部门规章', cls: 'dept' },
  LOCAL_RULE:       { label: '地方政府规章', cls: 'local' }
}

/** 项目状态 */
export const PROJECT_STATUS = {
  DRAFT:     { label: '草稿',   tag: 'info' },
  ACTIVE:    { label: '进行中', tag: 'primary' },
  PUBLISHED: { label: '已发布', tag: 'success' },
  OBSOLETE:  { label: '已废止', tag: 'danger' }
}

/** 严重度 */
export const SEVERITY = {
  RED:    { label: '严重', cls: 'red' },
  YELLOW: { label: '关注', cls: 'yellow' },
  BLUE:   { label: '格式', cls: 'blue' },
  GREY:   { label: '优化', cls: 'grey' }
}

/** 资料类型 */
export const MATERIAL_TYPE = {
  REGULATION:     '法规',
  DRAFT:          '草案',
  REPORT:         '报告',
  EXPERT_OPINION: '专家意见',
  CASE:           '典型案例'
}

/** 维度 */
export const DIMENSION = {
  LEGALITY:    '合法性',
  EXECUTION:   '落实性',
  SATISFACTION:'满意度'
}

/** 简单的 ajax 请求封装 */
export const request = (options) => {
  const baseURL = 'http://localhost:8083/api'
  return new Promise((resolve, reject) => {
    uni.request({
      url: baseURL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'X-User-Id': uni.getStorageSync('userId') || '1',
        'X-Role':    uni.getStorageSync('userRole') || '系统管理员',
        ...(options.header || {})
      },
      success: (res) => {
        const data = res.data
        if (data && data.code === 200) resolve(data.data)
        else reject(new Error(data?.message || '请求失败'))
      },
      fail: (err) => reject(err)
    })
  })
}
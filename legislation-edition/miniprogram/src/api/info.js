/**
 * 模块八：立法信息展示
 */
import request from './request.js'

export const newsList = (category, page = 1, size = 20) =>
  request({ url: '/info/news', data: { category, page, size } })

export const newsDetail = (id) =>
  request({ url: `/info/news/${id}` })

export const regulationIndex = (domain, regionCode) =>
  request({ url: '/info/regulation-index', data: { domain, regionCode } })

export const policyInterpretations = () =>
  request({ url: '/info/policy-interpretations' })

export const academicLiterature = () =>
  request({ url: '/info/academic-literature' })

export const bulletin = () =>
  request({ url: '/info/bulletin' })

export const dashboard = () =>
  request({ url: '/info/dashboard' })

export const dashboardChart = () =>
  request({ url: '/info/dashboard/chart' })

export const subscribe = (body) =>
  request({ url: '/info/subscription', method: 'POST', data: body })

export const unsubscribe = (id) =>
  request({ url: `/info/subscription/${id}`, method: 'DELETE' })

export const subscriptions = () =>
  request({ url: '/info/subscription/list' })

export const recommend = (materialId) =>
  request({ url: '/info/recommend', data: { materialId } })

export const regulationMap = () =>
  request({ url: '/info/map/regulation' })
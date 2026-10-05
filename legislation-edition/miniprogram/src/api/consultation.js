/**
 * 模块六：意见征集
 */
import request from './request.js'

export const listConsultations = (status, page = 1, size = 20) =>
  request({ url: '/consultation/list', data: { status, page, size } })

export const consultationDetail = (id) =>
  request({ url: `/consultation/${id}` })

export const createConsultation = (body) =>
  request({ url: '/consultation', method: 'POST', data: body })

export const updateConsultation = (id, body) =>
  request({ url: `/consultation/${id}`, method: 'PUT', data: body })

// 提交意见（无需登录）
export const submitOpinion = (id, body) =>
  request({ url: `/consultation/${id}/opinion`, method: 'POST', data: body })

export const listOpinions = (id, page = 1, size = 20, status, category) =>
  request({ url: `/consultation/${id}/opinions`, data: { page, size, status, category } })

export const statistics = (id) =>
  request({ url: `/consultation/${id}/statistics` })

export const wordCloud = (id, topN = 50) =>
  request({ url: `/consultation/${id}/wordcloud`, data: { topN } })

export const classify = (id) =>
  request({ url: `/consultation/${id}/classify`, method: 'POST' })

export const dedup = (id) =>
  request({ url: `/consultation/${id}/dedup`, method: 'POST' })

export const report = (id) =>
  request({ url: `/consultation/${id}/report` })

export const replyOpinion = (opinionId, content, replyBy) =>
  request({ url: `/consultation/opinion/${opinionId}/reply`, method: 'POST', data: { content, replyBy } })
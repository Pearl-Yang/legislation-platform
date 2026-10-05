/**
 * 模块七：立法资料库
 */
import request from './request.js'

export const listMaterials = (params) =>
  request({ url: '/library/material/list', data: params })

export const materialDetail = (id) =>
  request({ url: `/library/material/${id}` })

export const searchMaterials = (keyword, materialType, page = 1, size = 20) =>
  request({ url: '/library/material/search', data: { keyword, materialType, page, size } })

export const favorite = (id) =>
  request({ url: `/library/material/${id}/favorite`, method: 'POST' })

export const unFavorite = (id) =>
  request({ url: `/library/material/${id}/favorite`, method: 'DELETE' })

export const myFavorites = () =>
  request({ url: '/library/material/favorites' })

export const addNote = (id, body) =>
  request({ url: `/library/material/${id}/note`, method: 'POST', data: body })

export const listNotes = (id) =>
  request({ url: `/library/material/${id}/notes` })

export const related = (id) =>
  request({ url: `/library/material/${id}/related` })

export const tags = () =>
  request({ url: '/library/tag/list' })

export const suggest = (keyword, limit = 10) =>
  request({ url: '/library/material/suggest', data: { keyword, limit } })
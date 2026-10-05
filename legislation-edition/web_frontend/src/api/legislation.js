import request from '@/utils/request'

/**
 * 智立法 · 行政立法智能辅助平台
 * API 封装 - 覆盖 OpenAPI 全部 9 个模块
 *
 * 通用响应：{ code: 200, message, data }
 * 后端端口：8083，前端开发用 vite proxy 转发 /api
 */

// =====================================================
// 00  认证
// =====================================================
export const login = (data) => request.post('/auth/login', data)
export const logout = () => request.post('/auth/logout')
export const getCurrentUser = () => request.get('/auth/me')
export const health = () => request.get('/auth/health')

// =====================================================
// 01  立法项目全流程
// =====================================================
export const listProjects = (params) => request.get('/legislative-project/list', { params })
export const getProject   = (id)     => request.get(`/legislative-project/${id}`)
export const createProject = (data)  => request.post('/legislative-project', data)
export const updateProject = (id, data) => request.put(`/legislative-project/${id}`, data)
export const deleteProject = (id)     => request.delete(`/legislative-project/${id}`)
export const advanceProject = (id, params) => request.post(`/legislative-project/${id}/advance`, null, { params })
export const rollbackProject = (id, params) => request.post(`/legislative-project/${id}/rollback`, null, { params })
export const listStages = (id)      => request.get(`/legislative-project/${id}/stages`)
export const currentStage = (id)    => request.get(`/legislative-project/${id}/current-stage`)
export const progress = (id)       => request.get(`/legislative-project/${id}/progress`)
export const deadlines = (id, params) => request.get(`/legislative-project/${id}/deadlines`, { params })
export const projectDashboard = ()   => request.get('/legislative-project/dashboard')
export const upcomingProjects = (params) => request.get('/legislative-project/upcoming', { params })
export const stageTemplates = (type) => request.get('/legislative-project/stage-template/list', { params: { type } })

// =====================================================
// 02  草案生成（异步任务）
// =====================================================
export const generateDraft = (data) => request.post('/draft/generate', data)
export const pollDraftTask = (taskId) => request.get(`/draft/task/${taskId}`)
export const listDrafts    = (projectId) => request.get('/draft/list', { params: { projectId } })
export const getDraft      = (id) => request.get(`/draft/${id}`)
export const listVersions  = (id) => request.get(`/draft/${id}/versions`)
export const reviseDraft   = (id, data) => request.post(`/draft/${id}/revise`, data)
export const exportDraft   = (id, format = 'MARKDOWN') => request.get(`/draft/${id}/export`, { params: { format }, responseType: 'blob' })

// =====================================================
// 03  智慧审查（红/黄/蓝/灰四级）
// =====================================================
export const submitReview  = (data) => request.post('/review/submit', data)
export const batchReview   = (draftIds) => request.post('/review/batch-submit', draftIds)
export const getReviewRecord = (id) => request.get(`/review/record/${id}`)
export const resolveIssue  = (id) => request.post(`/review/issue/${id}/resolve`)
export const listRules     = (params) => request.get('/review/rule-list', { params })
export const createRule    = (data) => request.post('/review/rule', data)
export const updateRule    = (id, data) => request.put(`/review/rule/${id}`, data)
export const deleteRule    = (id) => request.delete(`/review/rule/${id}`)

// =====================================================
// 04  法规清理 + 法规主表
// =====================================================
export const listRegulations = (params) => request.get('/regulation/list', { params })
export const searchRegulations = (params) => request.get('/regulation/search', { params })
export const getRegulationRelations = (id, params) => request.get(`/regulation/${id}/relations`, { params: { id, ...(params || {}) } })

export const listCleanupTasks = (params) => request.get('/cleanup/task/list', { params })
export const createCleanupTask = (data) => request.post('/cleanup/task', data)
export const getCleanupTask = (id) => request.get(`/cleanup/task/${id}`)
export const getCleanupReport = (taskId, params = {}) => request.get(`/cleanup/task/${taskId}/report`, { params, responseType: 'blob' })
export const affectedRegulations = (taskId) => request.get(`/cleanup/task/${taskId}/affected-regulations`)
export const suggestCleanup = (taskId) => request.post(`/cleanup/task/${taskId}/suggest`)
export const decideSuggestion = (id, params) => request.post(`/cleanup/suggestion/${id}/decide`, null, { params })

// =====================================================
// 05  实施评估
// =====================================================
export const listEvaluations  = (params) => request.get('/evaluation/list', { params })
export const createEvaluation = (data) => request.post('/evaluation', data)
export const getEvaluation    = (id) => request.get(`/evaluation/${id}`)
export const getEvaluationChart = (id) => request.get(`/evaluation/${id}/chart-data`)
export const getEvaluationReport = (id, params) => request.get(`/evaluation/${id}/report`, { params, responseType: 'blob' })
export const compareEvaluations = (params) => request.get('/evaluation/compare', { params })
export const listIndicators   = () => request.get('/evaluation/indicator-list')
export const syncEvaluationData = (params) => request.post('/evaluation/data-sync', null, { params })

// =====================================================
// 06  意见征集
// =====================================================
export const listConsultations  = (params) => request.get('/consultation/list', { params })
export const createConsultation = (data)   => request.post('/consultation', data)
export const getConsultation    = (id)     => request.get(`/consultation/${id}`)
export const updateConsultation = (id, data) => request.put(`/consultation/${id}`, data)
export const getConsultationReport = (id, format = 'HTML') => request.get(`/consultation/${id}/report`, { params: { format }, responseType: 'blob' })
export const getStatistics      = (id)     => request.get(`/consultation/${id}/statistics`)
export const listOpinions       = (id, params) => request.get(`/consultation/${id}/opinions`, { params })
export const submitOpinion      = (id, data) => request.post(`/consultation/${id}/opinion`, data)
export const dedupOpinions      = (id)      => request.post(`/consultation/${id}/dedup`)
export const classifyOpinions   = (id)      => request.post(`/consultation/${id}/classify`)
export const getWordcloud       = (id, topN = 50) => request.get(`/consultation/${id}/wordcloud`, { params: { topN } })

// =====================================================
// 07  立法资料库
// =====================================================
export const listMaterials   = (params) => request.get('/library/material/list', { params })
export const searchMaterials = (params) => request.get('/library/material/search', { params })
export const getMaterial     = (id)     => request.get(`/library/material/${id}`)
export const createMaterial  = (data)   => request.post('/library/material', data)
export const updateMaterial  = (id, data) => request.put(`/library/material/${id}`, data)
export const deleteMaterial  = (id)     => request.delete(`/library/material/${id}`)
export const favoriteMaterial = (id)    => request.post(`/library/material/${id}/favorite`)
export const unfavoriteMaterial = (id)  => request.delete(`/library/material/${id}/favorite`)
export const listFavorites    = ()      => request.get('/library/material/favorites')
export const listNotes        = (id)    => request.get(`/library/material/${id}/notes`)
export const addNote          = (id, data) => request.post(`/library/material/${id}/note`, data)
export const relatedMaterials = (id)    => request.get(`/library/material/${id}/related`)
export const batchImportMaterials = (data) => request.post('/library/material/batch-import', data)
export const listTags         = ()      => request.get('/library/tag/list')

// =====================================================
// 08  信息门户
// =====================================================
export const listNews = (params) => request.get('/info/news', { params })
export const getNewsDetail = (id) => request.get(`/info/news/${id}`)
export const regulationIndex = (params) => request.get('/info/regulation-index', { params })
export const policyInterpretations = () => request.get('/info/policy-interpretations')
export const academicLiterature = () => request.get('/info/academic-literature')
export const bulletin = () => request.get('/info/bulletin')
export const infoDashboard = () => request.get('/info/dashboard')
export const infoDashboardChart = () => request.get('/info/dashboard/chart')
export const regulationMap = () => request.get('/info/map/regulation')
export const recommend = (materialId) => request.get('/info/recommend', { params: { materialId } })
export const subscribe = (data) => request.post('/info/subscription', data)
export const unsubscribe = (id) => request.delete(`/info/subscription/${id}`)
export const mySubscriptions = () => request.get('/info/subscription/list')
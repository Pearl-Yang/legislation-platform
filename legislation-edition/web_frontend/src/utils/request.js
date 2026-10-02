import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

let isRedirectingToLogin = false

function redirectToLogin() {
  if (isRedirectingToLogin) return
  isRedirectingToLogin = true
  const userStore = useUserStore()
  userStore.logout()
  ElMessage.error('登录已过期,请重新登录')
  router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  setTimeout(() => { isRedirectingToLogin = false }, 1500)
}

function redirectToForbidden() {
  ElMessage.error('您无权访问该资源')
  router.replace('/403')
}

request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (!data) return response
    if (data.code === 200) return data
    // 401xx = 鉴权失败 -> 跳登录
    if (data.code && data.code >= 40100 && data.code < 40200) {
      redirectToLogin()
      return Promise.reject(new Error(data.message || '未登录'))
    }
    // 403xx = 无权限
    if (data.code && data.code >= 40300 && data.code < 40400) {
      redirectToForbidden()
      return Promise.reject(new Error(data.message || '无权访问'))
    }
    // 其他业务错误
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      redirectToLogin()
    } else if (status === 403) {
      redirectToForbidden()
    } else if (status >= 500) {
      ElMessage.error('服务器异常,请稍后重试')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
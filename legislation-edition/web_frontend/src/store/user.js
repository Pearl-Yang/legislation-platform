import { defineStore } from 'pinia'

const TOKEN_KEY = 'token'
const USER_KEY = 'userInfo'

/**
 * 用户状态 - token + 用户信息
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    role: (s) => s.userInfo?.role || 'USER',
    isAdmin: (s) => (s.userInfo?.role || '') === 'ADMIN'
  },
  actions: {
    setLogin({ token, userInfo }) {
      this.token = token
      this.userInfo = userInfo
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
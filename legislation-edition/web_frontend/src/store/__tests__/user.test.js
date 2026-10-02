import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/store/user.js'

describe('user store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('initial state is empty', () => {
    const u = useUserStore()
    expect(u.token).toBe('')
    expect(u.userInfo).toBeNull()
    expect(u.isLoggedIn).toBe(false)
    expect(u.isAdmin).toBe(false)
    expect(u.role).toBe('USER')
  })

  it('setLogin 持久化 token + userInfo', () => {
    const u = useUserStore()
    u.setLogin({ token: 'jwt-123', userInfo: { id: 1, username: 'admin', role: 'ADMIN' } })
    expect(u.token).toBe('jwt-123')
    expect(u.isLoggedIn).toBe(true)
    expect(u.isAdmin).toBe(true)
    expect(localStorage.getItem('token')).toBe('jwt-123')
    expect(JSON.parse(localStorage.getItem('userInfo'))).toEqual({
      id: 1, username: 'admin', role: 'ADMIN'
    })
  })

  it('logout 清空 token 与 localStorage', () => {
    const u = useUserStore()
    u.setLogin({ token: 't', userInfo: { role: 'USER' } })
    u.logout()
    expect(u.token).toBe('')
    expect(u.isLoggedIn).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
  })
})
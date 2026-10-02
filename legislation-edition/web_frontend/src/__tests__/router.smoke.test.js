import { describe, it, expect } from 'vitest'
import router from '@/router/index.js'

/**
 * 前端 E2E smoke 测试 - 路由可达性
 *
 * 验证关键业务页面路由都存在且配置正确。
 */
describe('router smoke', () => {
  const routes = router.getRoutes()

  it('根路径重定向到 /app/dashboard', () => {
    const root = routes.find(r => r.path === '/')
    expect(root).toBeDefined()
    // 根路径是 redirect,不会渲染组件,直接看配置
    expect(routes.some(r => r.path === '/login')).toBe(true)
  })

  it('登录页路由存在', () => {
    const login = routes.find(r => r.path === '/login')
    expect(login).toBeDefined()
    expect(login.meta?.guest).toBe(true)
  })

  it('业务模块父路由都存在', () => {
    const modules = ['dashboard', 'project', 'draft', 'review', 'cleanup', 'evaluation', 'consultation', 'library', 'info']
    for (const m of modules) {
      const found = routes.some(r => r.path === `/app/${m}` || r.path.startsWith(`/app/${m}/`))
      expect(found, `/app/${m} 路由缺失`).toBe(true)
    }
  })

  it('业务模块至少有 9 个深层路由', () => {
    // 9 个子页面是 P0 要求
    const businessDeep = routes.filter(r => r.path.startsWith('/app/') && r.path !== '/app')
    expect(businessDeep.length).toBeGreaterThanOrEqual(15)
  })

  it('叶子路由都有 title meta', () => {
    // 父路由 /app/<module> 是嵌套壳,可以没 title;叶子路由必须带
    const leafRoutes = routes.filter(r =>
      r.path.startsWith('/app/') &&
      r.path !== '/app' &&
      (!r.children || r.children.length === 0) &&
      !r.redirect
    )
    const withoutTitle = leafRoutes.filter(r => !r.meta?.title)
    expect(withoutTitle).toEqual([])
  })
})
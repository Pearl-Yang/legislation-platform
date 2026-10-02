import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

/**
 * 智立法 - 路由配置
 *
 * 一级路由 /app/<module>
 * 多层路由 /app/<module>/<sub>
 *
 * 鉴权:
 *  - 需登录:meta.auth = true(默认 true)
 *  - 仅管理员:meta.adminOnly = true
 */

const Layout = () => import('@/components/Layout.vue')

const routes = [
  { path: '/', redirect: '/app/dashboard' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/Forbidden.vue'),
    meta: { title: '无权访问', guest: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在', guest: true }
  },

  {
    path: '/app',
    component: Layout,
    redirect: '/app/dashboard',
    children: [
      // ----------------- 工作台 -----------------
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },

      // ----------------- 立法项目 -----------------
      {
        path: 'project',
        name: 'ProjectList',
        component: () => import('@/views/project/index.vue'),
        meta: { title: '立法项目' }
      },
      {
        path: 'project/detail/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/project/Detail.vue'),
        meta: { title: '项目详情' }
      },

      // ----------------- 草案生成 -----------------
      {
        path: 'draft',
        component: () => import('@/views/draft/index.vue'),
        redirect: '/app/draft/generate',
        children: [
          {
            path: 'generate',
            name: 'DraftGenerate',
            component: () => import('@/views/draft/Generate.vue'),
            meta: { title: 'AI 智能生成' }
          },
          {
            path: 'list',
            name: 'DraftList',
            component: () => import('@/views/draft/List.vue'),
            meta: { title: '草案列表' }
          }
        ]
      },

      // ----------------- 智慧审查 -----------------
      {
        path: 'review',
        component: () => import('@/views/review/index.vue'),
        redirect: '/app/review/records',
        children: [
          {
            path: 'submit',
            name: 'ReviewSubmit',
            component: () => import('@/views/review/Submit.vue'),
            meta: { title: '提交审查' }
          },
          {
            path: 'records',
            name: 'ReviewRecords',
            component: () => import('@/views/review/Records.vue'),
            meta: { title: '审查记录' }
          },
          {
            path: 'rules',
            name: 'ReviewRules',
            component: () => import('@/views/review/Rules.vue'),
            meta: { title: '审查规则' }
          }
        ]
      },

      // ----------------- 智能清理 -----------------
      {
        path: 'cleanup',
        component: () => import('@/views/cleanup/index.vue'),
        redirect: '/app/cleanup/tasks',
        children: [
          {
            path: 'tasks',
            name: 'CleanupTasks',
            component: () => import('@/views/cleanup/Tasks.vue'),
            meta: { title: '清理任务' }
          },
          {
            path: 'regulations',
            name: 'Regulations',
            component: () => import('@/views/cleanup/Regulations.vue'),
            meta: { title: '法规主表' }
          },
          {
            path: 'graph',
            name: 'RelationGraph',
            component: () => import('@/views/cleanup/Graph.vue'),
            meta: { title: '关系图谱' }
          }
        ]
      },

      // ----------------- 实施评估 -----------------
      {
        path: 'evaluation',
        component: () => import('@/views/evaluation/index.vue'),
        redirect: '/app/evaluation/tasks',
        children: [
          {
            path: 'tasks',
            name: 'EvaluationTasks',
            component: () => import('@/views/evaluation/Tasks.vue'),
            meta: { title: '评估任务' }
          },
          {
            path: 'indicators',
            name: 'EvaluationIndicators',
            component: () => import('@/views/evaluation/Indicators.vue'),
            meta: { title: '评估指标' }
          },
          {
            path: 'reports',
            name: 'EvaluationReports',
            component: () => import('@/views/evaluation/Reports.vue'),
            meta: { title: '评估报告' }
          }
        ]
      },

      // ----------------- 意见征集 -----------------
      {
        path: 'consultation',
        component: () => import('@/views/consultation/index.vue'),
        redirect: '/app/consultation/list',
        children: [
          {
            path: 'list',
            name: 'ConsultationList',
            component: () => import('@/views/consultation/List.vue'),
            meta: { title: '征集公告' }
          },
          {
            path: 'collect',
            name: 'ConsultationCollect',
            component: () => import('@/views/consultation/Collect.vue'),
            meta: { title: '公众参与' }
          }
        ]
      },

      // ----------------- 资料库 -----------------
      {
        path: 'library',
        component: () => import('@/views/library/index.vue'),
        redirect: '/app/library/list',
        children: [
          {
            path: 'list',
            name: 'LibraryList',
            component: () => import('@/views/library/List.vue'),
            meta: { title: '资料浏览' }
          },
          {
            path: 'search',
            name: 'LibrarySearch',
            component: () => import('@/views/library/Search.vue'),
            meta: { title: '全文检索' }
          },
          {
            path: 'favorites',
            name: 'LibraryFavorites',
            component: () => import('@/views/library/Favorites.vue'),
            meta: { title: '我的收藏' }
          },
          {
            path: 'detail/:id',
            name: 'LibraryDetail',
            component: () => import('@/views/library/Detail.vue'),
            meta: { title: '资料详情' }
          }
        ]
      },

      // ----------------- 信息门户 -----------------
      {
        path: 'info',
        component: () => import('@/views/info/index.vue'),
        redirect: '/app/info/news',
        children: [
          {
            path: 'news',
            name: 'InfoNews',
            component: () => import('@/views/info/News.vue'),
            meta: { title: '立法动态' }
          },
          {
            path: 'interpret',
            name: 'InfoInterpret',
            component: () => import('@/views/info/Interpret.vue'),
            meta: { title: '政策解读' }
          },
          {
            path: 'dashboard',
            name: 'InfoDashboard',
            component: () => import('@/views/info/Dashboard.vue'),
            meta: { title: '数据大屏' }
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token

  // 1. 白名单
  if (to.meta?.guest) return next()

  // 2. 需登录
  if (!token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 3. 角色校验
  if (to.meta?.adminOnly && !userStore.isAdmin) {
    return next('/403')
  }

  // 4. 业务页面正常
  next()
})

export default router
<template>
  <el-container class="layout-root">
    <!-- ============== 侧边栏 ============== -->
    <el-aside :width="sidebarWidth" class="layout-aside">
      <div class="brand" @click="$router.push('/app/dashboard')">
        <div class="brand-logo">智</div>
        <transition name="fade">
          <div v-show="!collapsed" class="brand-text">
            <div class="brand-name">智立法</div>
            <div class="brand-sub">行政立法智能辅助平台</div>
          </div>
        </transition>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="$route.path"
          background-color="transparent"
          text-color="#bfcbd9"
          active-text-color="#fff"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          unique-opened
        >
          <template v-for="(item, idx) in menuItems" :key="idx">
            <!-- 单层菜单 -->
            <el-menu-item
              v-if="!item.children"
              :index="`/app/${item.path}`"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>

            <!-- 多层菜单 -->
            <el-sub-menu v-else :index="`sub-${idx}`">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item
                v-for="sub in item.children"
                :key="sub.path"
                :index="`/app/${item.path}/${sub.path}`"
              >
                <el-icon><component :is="sub.icon" /></el-icon>
                <template #title>{{ sub.title }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>

      <div class="aside-footer" @click="collapsed = !collapsed">
        <el-icon>
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
        <transition name="fade">
          <span v-show="!collapsed">收起菜单</span>
        </transition>
      </div>
    </el-aside>

    <!-- ============== 主体 ============== -->
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/app/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-input
            v-model="searchKeyword"
            placeholder="全局搜索：法规 / 草案 / 资料"
            class="header-search"
            clearable
            @keyup.enter="onGlobalSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <el-tooltip content="意见征集通知">
            <el-badge :value="3" class="header-icon-btn">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </el-tooltip>

          <el-tooltip content="任务中心">
            <el-icon class="header-icon-btn"><Notification /></el-icon>
          </el-tooltip>

          <el-dropdown trigger="click" @command="onUserCmd">
            <div class="user-area">
              <el-avatar :size="32" class="user-avatar">{{ userInitial }}</el-avatar>
              <span class="user-name">{{ userInfo.name }}</span>
              <el-icon><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <div style="display:flex;flex-direction:column">
                    <span style="font-weight:600">{{ userInfo.name }}</span>
                    <span style="font-size:12px;color:#999">{{ userInfo.role }}</span>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const collapsed = ref(false)
const sidebarWidth = computed(() => (collapsed.value ? '64px' : '230px'))

const searchKeyword = ref('')
const onGlobalSearch = () => {
  if (!searchKeyword.value.trim()) return
  router.push({ path: '/app/library', query: { keyword: searchKeyword.value.trim() } })
}

const userInfo = ref({
  name: localStorage.getItem('userName') || '立法管理员',
  role: localStorage.getItem('userRole') || '系统管理员'
})
const userInitial = computed(() => userInfo.value.name.slice(0, 1))

const onUserCmd = (cmd) => {
  if (cmd === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('userName')
    localStorage.removeItem('userRole')
    localStorage.removeItem('userId')
    router.push('/login')
  }
}

// 8 大模块（其中部分模块内分子页）
const menuItems = [
  { path: 'dashboard',  title: '工作台',         icon: 'Odometer' },
  { path: 'project',    title: '立法项目',        icon: 'Files' },
  {
    path: 'draft', title: '草案生成', icon: 'EditPen',
    children: [
      { path: 'generate', title: 'AI 智能生成', icon: 'MagicStick' },
      { path: 'list',     title: '草案列表',     icon: 'List' }
    ]
  },
  {
    path: 'review', title: '智慧审查', icon: 'View',
    children: [
      { path: 'submit',   title: '提交审查',     icon: 'CirclePlus' },
      { path: 'records',  title: '审查记录',     icon: 'Document' },
      { path: 'rules',    title: '审查规则',     icon: 'SetUp' }
    ]
  },
  {
    path: 'cleanup', title: '智能清理', icon: 'Brush',
    children: [
      { path: 'tasks',      title: '清理任务',   icon: 'List' },
      { path: 'regulations', title: '法规主表',  icon: 'Notebook' },
      { path: 'graph',      title: '关系图谱',   icon: 'Share' }
    ]
  },
  {
    path: 'evaluation', title: '实施评估', icon: 'DataAnalysis',
    children: [
      { path: 'tasks',      title: '评估任务',   icon: 'List' },
      { path: 'indicators', title: '评估指标',   icon: 'Histogram' },
      { path: 'reports',    title: '评估报告',   icon: 'DataLine' }
    ]
  },
  {
    path: 'consultation', title: '意见征集', icon: 'ChatDotRound',
    children: [
      { path: 'list',    title: '征集公告',     icon: 'Notification' },
      { path: 'collect', title: '公众参与',     icon: 'EditPen' }
    ]
  },
  {
    path: 'library', title: '立法资料库', icon: 'Files',
    children: [
      { path: 'list',     title: '资料浏览',   icon: 'Reading' },
      { path: 'search',   title: '全文检索',   icon: 'Search' },
      { path: 'favorites', title: '我的收藏', icon: 'Star' }
    ]
  },
  {
    path: 'info', title: '信息门户', icon: 'DataBoard',
    children: [
      { path: 'news',      title: '立法动态',   icon: 'Notification' },
      { path: 'interpret', title: '政策解读',   icon: 'Document' },
      { path: 'dashboard', title: '数据大屏',   icon: 'PieChart' }
    ]
  }
]
</script>

<style lang="scss" scoped>
.layout-root { height: 100vh; }

// ============ 侧边栏 ============
.layout-aside {
  background: linear-gradient(180deg, $bg-sidebar 0%, darken($bg-sidebar, 4%) 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  border-right: 1px solid rgba(255,255,255,0.04);

  .brand {
    height: 64px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 16px;
    cursor: pointer;
    border-bottom: 1px solid rgba(255,255,255,0.06);

    .brand-logo {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: linear-gradient(135deg, $primary-light, $primary-color);
      color: #fff;
      font-size: 18px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(79, 70, 229, 0.45);
    }
    .brand-text {
      color: #fff;
      overflow: hidden;
      .brand-name { font-size: 17px; font-weight: 600; line-height: 1.2; letter-spacing: -0.01em; }
      .brand-sub  { font-size: 11px; color: rgba(255,255,255,0.55); margin-top: 2px; }
    }
  }

  .menu-scroll {
    flex: 1;
    :deep(.el-menu) {
      border-right: none;
      background: transparent;
    }
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      height: 42px;
      line-height: 42px;
      margin: 2px 10px;
      border-radius: 8px;
      &:hover {
        background-color: rgba(255, 255, 255, 0.06) !important;
        color: #fff !important;
      }
    }
    :deep(.el-menu-item.is-active) {
      background: linear-gradient(90deg, rgba(79, 70, 229, 0.95), rgba(99, 102, 241, 0.55)) !important;
      color: #fff !important;
      position: relative;
      box-shadow: 0 4px 12px rgba(79, 70, 229, 0.35);
      &::before {
        content: '';
        position: absolute;
        left: -10px; top: 8px; bottom: 8px;
        width: 3px;
        background: #fff;
        border-radius: 0 2px 2px 0;
      }
    }
    :deep(.el-sub-menu .el-menu-item) {
      padding-left: 48px !important;
      font-size: 13px;
      min-width: 0;
    }
  }

  .aside-footer {
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: rgba(255,255,255,0.55);
    cursor: pointer;
    border-top: 1px solid rgba(255,255,255,0.06);
    font-size: 13px;
    &:hover { background-color: rgba(255,255,255,0.06); color: #fff; }
  }
}

// ============ 头部 ============
.layout-header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(10px);
  -webkit-backdrop-filter: saturate(180%) blur(10px);
  border-bottom: 1px solid $border-light;
  display: flex;
  align-items: center;
  padding: 0 24px;

  .header-left {
    display: flex;
    align-items: center;
    :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
      color: $primary-color;
      font-weight: 500;
    }
  }

  .header-right {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 18px;

    .header-search {
      width: 280px;
      :deep(.el-input__wrapper) {
        border-radius: 999px;
        background: $bg-canvas;
        box-shadow: none;
        &:hover { background: #fff; }
        &.is-focus { background: #fff; box-shadow: 0 0 0 1px $primary-color inset, 0 0 0 3px rgba(79, 70, 229, 0.12); }
      }
    }
    .header-icon-btn {
      font-size: 18px;
      color: $text-secondary;
      cursor: pointer;
      transition: color .15s ease;
      &:hover { color: $primary-color; }
    }
    .user-area {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 4px 10px 4px 4px;
      border-radius: 999px;
      cursor: pointer;
      transition: background .15s ease;
      &:hover { background: $bg-canvas; }
      .user-avatar {
        background: linear-gradient(135deg, $primary-light, $primary-color);
        color: #fff;
        font-weight: 600;
      }
      .user-name {
        font-size: 13px;
        color: $text-regular;
        font-weight: 500;
      }
    }
  }
}

// ============ 主区域 ============
.layout-main {
  background: $bg-page;
  padding: 16px 20px;
  overflow-y: auto;
}

// ============ 路由切换动画 ============
.fade-slide-enter-active, .fade-slide-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.fade-slide-enter-from { opacity: 0; transform: translateY(8px); }
.fade-slide-leave-to   { opacity: 0; transform: translateY(-4px); }

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
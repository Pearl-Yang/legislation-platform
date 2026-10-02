<template>
  <div class="login-page">
    <!-- 左侧：背景图 + 蒙版 + 品牌区 -->
    <div class="login-left">
      <div class="left-bg" :style="{ backgroundImage: `url(${bgUrl})` }"></div>
      <div class="left-mask"></div>

      <div class="left-content">
        <div class="brand-row">
          <div class="brand-logo">智</div>
          <div>
            <div class="brand-title">智立法</div>
            <div class="brand-subtitle">行政立法智能辅助平台</div>
          </div>
        </div>

        <div class="slogan">
          <h2>让行政立法更智能</h2>
          <p>覆盖立项 → 起草 → 审查 → 公布 → 清理 → 评估 全生命周期</p>
        </div>

        <ul class="feature-list">
          <li><el-icon><Check /></el-icon>AI 辅助草案生成与异地参考检索</li>
          <li><el-icon><Check /></el-icon>红/黄/蓝/灰四级智慧审查</li>
          <li><el-icon><Check /></el-icon>法规上下位关系图谱 + 联动清理</li>
          <li><el-icon><Check /></el-icon>多渠道公众意见智能归类</li>
        </ul>
      </div>
    </div>

    <!-- 右侧：白色登录表单 -->
    <div class="login-right">
      <div class="form-wrapper">
        <h3 class="form-title">账号登录</h3>
        <p class="form-tip">支持：账号密码 / 短信验证 / 企业微信</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="onLogin">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <div class="form-extra">
              <el-checkbox v-model="remember">7 天内自动登录</el-checkbox>
              <a class="forget" @click.prevent>忘记密码？</a>
            </div>
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="onLogin">
            登 录
          </el-button>
        </el-form>

        <div class="demo-tip">
          <el-alert type="info" :closable="false" show-icon>
            <template #title>
              演示账号:admin / leader / drafter / reviewer / evaluator(密码统一 123456)
            </template>
          </el-alert>
        </div>
      </div>
    </div>

    <div class="login-footer">© 2026 智立法 · 行政立法智能辅助平台 v0.2 · 由 Cursor / MiniMax-M3 提供技术支持</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import bgUrl from '@/assets/login-bg.png'

const router = useRouter()
const route  = useRoute()

const formRef = ref()
const loading = ref(false)
const remember = ref(true)

const form = reactive({ username: 'admin', password: '123456' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/** 已登录则直接跳过登录页 */
onMounted(() => {
  if (localStorage.getItem('token')) {
    router.replace(route.query.redirect || '/app/dashboard')
  }
})

const onLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await request.post('/auth/login', {
        username: form.username,
        password: form.password
      })
      // res 已经是 { code, message, data: { token, userId, ... } } 格式
      const data = res.data || res
      localStorage.setItem('token',       data.token)
      localStorage.setItem('userId',      String(data.userId))
      localStorage.setItem('userName',    data.displayName || data.username)
      localStorage.setItem('userRole',    data.role || 'ROLE_USER')
      localStorage.setItem('department',  data.department || '')
      ElMessage.success('登录成功')
      router.push(route.query.redirect || '/app/dashboard')
    } catch (err) {
      // request.js 已经 ElMessage.error 过了,这里只关 loading
      console.error('登录失败', err)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  position: relative;
  background: #f8fafc;
}

// ============ 左侧：背景图 + 蒙版 + 品牌文案 ============
.login-left {
  position: relative;
  flex: 1.1;
  min-height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: stretch;
  color: #fff;

  .left-bg {
    position: absolute;
    inset: 0;
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    transform: scale(1.02);          // 防止边缘露出底色
  }
  .left-mask {
    position: absolute;
    inset: 0;
    // 双层蒙版：靛蓝渐变 + 轻微暗化，保证白字可读
    background:
      linear-gradient(135deg, rgba(79, 70, 229, 0.72) 0%, rgba(15, 23, 42, 0.55) 100%),
      rgba(15, 23, 42, 0.25);
  }

  .left-content {
    position: relative;
    z-index: 1;
    padding: 48px 56px;
    display: flex;
    flex-direction: column;
    width: 100%;

    .brand-row {
      display: flex;
      align-items: center;
      gap: 14px;
      .brand-logo {
        width: 48px; height: 48px;
        background: rgba(255,255,255,0.18);
        backdrop-filter: blur(8px);
        border: 1px solid rgba(255,255,255,0.25);
        border-radius: 12px;
        display: flex; align-items: center; justify-content: center;
        font-size: 22px; font-weight: 700;
      }
      .brand-title { font-size: 20px; font-weight: 600; letter-spacing: -0.01em; }
      .brand-subtitle { font-size: 12px; color: rgba(255,255,255,0.7); margin-top: 2px; }
    }

    .slogan {
      margin-top: 96px;
      max-width: 460px;
      h2 {
        font-size: 32px;
        font-weight: 600;
        line-height: 1.35;
        letter-spacing: -0.01em;
      }
      p {
        margin-top: 14px;
        color: rgba(255,255,255,0.78);
        font-size: 14px;
        line-height: 1.8;
      }
    }

    .feature-list {
      list-style: none;
      padding: 0;
      margin-top: auto;
      max-width: 460px;
      li {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 8px 0;
        color: rgba(255,255,255,0.88);
        font-size: 13.5px;
        .el-icon {
          width: 18px; height: 18px;
          display: inline-flex; align-items: center; justify-content: center;
          background: rgba(255,255,255,0.18);
          border-radius: 50%;
          color: #fff;
        }
      }
    }
  }
}

// ============ 右侧：白色登录表单 ============
.login-right {
  width: 520px;
  flex-shrink: 0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 56px;
  box-shadow: -20px 0 40px -20px rgba(15, 23, 42, 0.08);

  .form-wrapper {
    width: 100%;
    max-width: 360px;
  }
  .form-title { font-size: 24px; font-weight: 600; color: $text-primary; letter-spacing: -0.01em; }
  .form-tip   { font-size: 13px; color: $text-secondary; margin-top: 6px; margin-bottom: 28px; }
  .form-extra { display: flex; justify-content: space-between; width: 100%; }
  .forget { font-size: 13px; color: $primary-color; cursor: pointer; &:hover { color: $primary-light; } }
  .login-btn {
    width: 100%;
    margin-top: 8px;
    height: 44px;
    font-size: 15px;
    font-weight: 500;
    letter-spacing: 0.04em;
  }
  .demo-tip  { margin-top: 24px; }
}

// ============ 页脚（横跨在底部，仅小屏可见） ============
.login-footer {
  position: absolute;
  bottom: 16px;
  left: 0; right: 0;
  text-align: center;
  color: rgba(100, 116, 139, 0.85);
  font-size: 12px;
  pointer-events: none;
  z-index: 2;
}

// ============ 响应式：小屏上下排 ============
@media (max-width: 900px) {
  .login-page { flex-direction: column; }
  .login-left { min-height: 240px; flex: 0 0 auto; }
  .login-left .left-content .slogan { margin-top: 32px; }
  .login-left .left-content .slogan h2 { font-size: 22px; }
  .login-left .left-content .feature-list { display: none; }
  .login-right { width: 100%; box-shadow: none; padding: 32px 24px; }
  .login-footer { color: $text-tertiary; }
}
</style>
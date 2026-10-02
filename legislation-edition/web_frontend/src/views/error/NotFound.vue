<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="error-page">
    <div class="err-card">
      <div class="err-code">404</div>
      <h2>页面走丢了</h2>
      <p>您访问的路径不存在,可能被移除或链接错误。</p>
      <div class="err-actions">
        <el-button type="primary" @click="router.push('/app/dashboard')">返回工作台</el-button>
        <el-button @click="router.back()">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f2f5 0%, #e8eaed 100%);
}
.err-card {
  background: #fff;
  border-radius: 12px;
  padding: 48px 64px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}
.err-code {
  font-size: 96px;
  font-weight: 700;
  color: #4f46e5;
  margin-bottom: 16px;
}
.err-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
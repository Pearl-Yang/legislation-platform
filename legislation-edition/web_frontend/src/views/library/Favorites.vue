<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">我的收藏</span>
      <span class="subtitle">个人收藏的立法资料，支持笔记、关联推荐</span>
    </div>

    <el-card>
      <el-empty v-if="!list.length" description="还没有收藏任何资料" />
      <el-row :gutter="16" v-else>
        <el-col v-for="m in list" :key="m.id" :xs="24" :sm="12" :md="8">
          <div class="fav-card" @click="$router.push(`/app/library/detail/${m.id}`)">
            <div class="fav-head">
              <el-tag size="small" type="primary">{{ materialTypeLabel(m.materialType) }}</el-tag>
              <span class="fav-date">{{ m.collectDate }}</span>
            </div>
            <h4 class="fav-title">{{ m.title }}</h4>
            <p class="fav-digest">{{ m.digest }}</p>
            <div class="fav-foot">
              <span><el-icon><View /></el-icon>{{ m.viewCount }}</span>
              <span><el-icon><EditPen /></el-icon>{{ m.noteCount }} 条笔记</span>
              <el-button link size="small" type="danger" @click.stop="onUnfav(m)">取消收藏</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { View, EditPen } from '@element-plus/icons-vue'
import { unfavoriteMaterial } from '@/api/legislation'
import { materialTypeLabel } from '@/utils/dict'

const list = ref([
  { id: 1, materialType: 'REGULATION',     title: '中华人民共和国数据安全法', digest: '为规范数据处理活动，保障数据安全。',  viewCount: 1283, collectDate: '2026-09-01', noteCount: 3 },
  { id: 4, materialType: 'REPORT',         title: '《数据安全法》立法说明会会议纪要', digest: '记录《数据安全法》立法过程中的主要讨论议题。',  viewCount: 312, collectDate: '2026-08-20', noteCount: 1 }
])

const onUnfav = async (m) => {
  await unfavoriteMaterial(m.id)
  list.value = list.value.filter(x => x.id !== m.id)
  ElMessage.success('已取消收藏')
}
</script>

<style lang="scss" scoped>
.fav-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid $border-light;
  padding: 14px 16px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.18s;
  &:hover { border-color: $primary-lighter; box-shadow: $shadow-card; }
}
.fav-head { display: flex; align-items: center; justify-content: space-between; }
.fav-date { font-size: 12px; color: $text-secondary; }
.fav-title { font-size: 15px; font-weight: 600; margin: 8px 0; line-height: 1.4; }
.fav-digest { font-size: 12px; color: $text-secondary; line-height: 1.6; }
.fav-foot { display: flex; gap: 12px; align-items: center; margin-top: 12px; font-size: 12px; color: $text-secondary; }
.fav-foot span { display: inline-flex; align-items: center; gap: 4px; }
.fav-foot .el-button { margin-left: auto; }
</style>
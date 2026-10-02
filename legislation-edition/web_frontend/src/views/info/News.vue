<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">立法动态</span>
      <span class="subtitle">汇集立法新闻、政策解读、典型案例、学术文献、政报公报</span>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="16">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="全部"      name="all" />
            <el-tab-pane label="新法规"   name="REGULATION_NEW" />
            <el-tab-pane label="政策解读" name="INTERPRET" />
            <el-tab-pane label="典型案例" name="CASE" />
            <el-tab-pane label="学术文献" name="LITERATURE" />
            <el-tab-pane label="公报简报" name="BULLETIN" />
          </el-tabs>

          <div class="news-list">
            <div v-for="n in filteredNews" :key="n.id" class="news-row">
              <div class="news-cover" :style="{ background: n.cover }">
                <el-icon class="cover-icon"><component :is="n.icon" /></el-icon>
              </div>
              <div class="news-body">
                <div class="news-head">
                  <el-tag size="small" :type="categoryTag(n.category)">{{ newsCategoryLabel(n.category) }}</el-tag>
                  <a class="news-title" @click="openDetail(n)">{{ n.title }}</a>
                </div>
                <p class="news-summary">{{ n.summary }}</p>
                <div class="news-meta">
                  <span><el-icon><User /></el-icon>{{ n.author }}</span>
                  <span><el-icon><Calendar /></el-icon>{{ n.publishedAt }}</span>
                  <span><el-icon><View /></el-icon>{{ n.viewCount }} 次浏览</span>
                  <span v-if="n.fromAuthority"><el-icon><OfficeBuilding /></el-icon>{{ n.fromAuthority }}</span>
                </div>
              </div>
            </div>
            <el-empty v-if="!filteredNews.length" description="暂无动态" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card>
          <template #header>
            <span class="title">
              <el-icon><Star /></el-icon>
              我的订阅
            </span>
          </template>
          <ul class="sub-list">
            <li v-for="s in subscriptions" :key="s.id">
              <div>
                <el-tag size="small" type="primary">{{ s.scope }}</el-tag>
                <span class="sub-keyword">{{ s.keyword }}</span>
              </div>
              <el-button link size="small" type="danger" @click="onUnsub(s)">取消</el-button>
            </li>
          </ul>
          <el-button :icon="Plus" type="primary" plain class="w-full mt-12" @click="subVisible = true">新增订阅</el-button>
        </el-card>

        <el-card class="mt-16">
          <template #header>
            <span class="title">热门动态</span>
          </template>
          <ol class="hot-list">
            <li v-for="(h, idx) in hotNews" :key="h.id">
              <span class="hot-rank" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
              <a class="hot-title" @click="openDetail(h)">{{ h.title }}</a>
              <span class="hot-view">{{ h.viewCount }}</span>
            </li>
          </ol>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="active?.title" size="60%">
      <div v-if="active" class="detail-content">
        <div class="detail-meta">
          <el-tag size="small" :type="categoryTag(active.category)">{{ newsCategoryLabel(active.category) }}</el-tag>
          <span>{{ active.publishedAt }} · {{ active.author }} · {{ active.viewCount }} 浏览</span>
        </div>
        <div class="detail-body" v-html="active.body"></div>
      </div>
    </el-drawer>

    <!-- 订阅 -->
    <el-dialog v-model="subVisible" title="新增订阅" width="480px">
      <el-form label-width="100px">
        <el-form-item label="订阅范围">
          <el-select v-model="subForm.scope" placeholder="请选择" style="width: 100%">
            <el-option value="DOMAIN"  label="按领域" />
            <el-option value="REGION"  label="按地区" />
            <el-option value="CATEGORY" label="按类别" />
          </el-select>
        </el-form-item>
        <el-form-item label="订阅关键字">
          <el-input v-model="subForm.keyword" placeholder="例：数据安全 / 北京 / 政策解读" />
        </el-form-item>
        <el-form-item label="推送方式">
          <el-checkbox-group v-model="subForm.channels">
            <el-checkbox value="EMAIL">邮件</el-checkbox>
            <el-checkbox value="WECHAT">微信</el-checkbox>
            <el-checkbox value="SMS">短信</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubscribe">订阅</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Star, Plus, User, Calendar, View, OfficeBuilding,
  Document, ChatLineSquare, Collection, Reading, Notification
} from '@element-plus/icons-vue'
import { listNews, subscribe, unsubscribe } from '@/api/legislation'
import { newsCategoryLabel } from '@/utils/dict'

const activeTab = ref('all')
const detailVisible = ref(false)
const subVisible = ref(false)
const active = ref(null)

const subForm = ref({ scope: 'DOMAIN', keyword: '', channels: ['EMAIL'] })

const news = ref([
  { id: 1, title: '国务院公布《网络数据安全管理条例》', category: 'REGULATION_NEW', icon: Document, summary: '2026年10月1日，国务院正式公布《网络数据安全管理条例》，这是我国第一部专门针对网络数据安全的行政法规。', publishedAt: '2026-10-01', author: '新华社', viewCount: 2356, fromAuthority: '国务院', cover: 'linear-gradient(135deg,#4f46e5,#6366f1)', body: '<p>2026年10月1日，国务院正式公布《网络数据安全管理条例》，自2027年1月1日起施行。</p><p>条例共 7 章 56 条，主要内容包括：建立数据分类分级保护制度、明确网络数据处理者的安全保护义务、规范数据跨境流动等。</p>' },
  { id: 2, title: '《数据安全法》专家解读：从立法本意到落地实施', category: 'INTERPRET', icon: Document, summary: '中国法学会副会长就《数据安全法》立法过程、核心条款和实施要点进行权威解读。', publishedAt: '2026-09-28', author: '中国法学会', viewCount: 1543, fromAuthority: '中国法学会', cover: 'linear-gradient(135deg,#6366f1,#10b981)', body: '<p>本文由中国法学会副会长深度解读《数据安全法》。</p>' },
  { id: 3, title: '某市网约车合规化治理典型案例', category: 'CASE', icon: Document, summary: '某市在网约车合规化治理中的执法实践、典型案例与制度演进。', publishedAt: '2026-09-25', author: '某市司法局', viewCount: 928, fromAuthority: '某市司法局', cover: 'linear-gradient(135deg,#10b981,#4f46e5)', body: '<p>本文记录某市网约车合规化治理的实践。</p>' },
  { id: 4, title: '数字经济时代的数据立法趋势研究', category: 'LITERATURE', icon: Document, summary: '学术文献综述：数字经济立法的国际经验与中国路径选择。', publishedAt: '2026-09-22', author: '北京大学法学院', viewCount: 712, fromAuthority: '北京大学', cover: 'linear-gradient(135deg,#8b5cf6,#4f46e5)', body: '<p>本文为数字经济立法的学术文献综述。</p>' },
  { id: 5, title: '国务院公报 2025 年第 18 号', category: 'BULLETIN', icon: Document, summary: '本期公报刊登了《数据安全法》《某省数据交易管理办法》等重要法规。', publishedAt: '2026-09-20', author: '国务院办公厅', viewCount: 643, fromAuthority: '国务院办公厅', cover: 'linear-gradient(135deg,#06b6d4,#6366f1)', body: '<p>本期公报的主要内容。</p>' }
])

const subscriptions = ref([
  { id: 1, scope: 'DOMAIN',   keyword: '数据安全' },
  { id: 2, scope: 'REGION',   keyword: '北京' },
  { id: 3, scope: 'CATEGORY', keyword: '政策解读' }
])

const hotNews = ref([
  { id: 1, title: '国务院公布《网络数据安全管理条例》', viewCount: 2356 },
  { id: 2, title: '《数据安全法》专家解读', viewCount: 1543 },
  { id: 6, title: '司法部发布最新法规清理工作报告', viewCount: 1182 },
  { id: 3, title: '某市网约车合规化治理典型案例', viewCount: 928 },
  { id: 4, title: '数字经济时代的数据立法趋势研究', viewCount: 712 }
])

const filteredNews = computed(() =>
  activeTab.value === 'all' ? news.value : news.value.filter(n => n.category === activeTab.value)
)

const categoryTag = (c) => ({ REGULATION_NEW: 'primary', INTERPRET: 'warning', CASE: 'danger', LITERATURE: 'success', BULLETIN: 'info' }[c] || 'info')

const openDetail = (row) => { active.value = row; detailVisible.value = true }

const onUnsub = async (s) => {
  await unsubscribe(s.id)
  subscriptions.value = subscriptions.value.filter(x => x.id !== s.id)
  ElMessage.success('已取消订阅')
}

const onSubscribe = async () => {
  await subscribe(subForm.value)
  subscriptions.value.unshift({ id: Date.now(), scope: subForm.value.scope, keyword: subForm.value.keyword })
  ElMessage.success('订阅成功')
  subVisible.value = false
}
</script>

<style lang="scss" scoped>
.news-list { display: flex; flex-direction: column; gap: 14px; }
.news-row {
  display: flex;
  gap: 14px;
  padding: 14px;
  border-radius: 6px;
  border: 1px solid $border-light;
  background: #fff;
  cursor: pointer;
  transition: all 0.18s;
  &:hover { box-shadow: $shadow-card; border-color: $primary-lighter; }
}
.news-cover {
  width: 120px;
  height: 88px;
  border-radius: 6px;
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  font-size: 32px;
}
.news-body { flex: 1; min-width: 0; }
.news-head { display: flex; gap: 8px; align-items: center; }
.news-title { font-size: 15px; font-weight: 600; color: $text-primary; &:hover { color: $primary-color; } }
.news-summary { font-size: 13px; color: $text-secondary; margin-top: 6px; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.news-meta { display: flex; gap: 16px; flex-wrap: wrap; font-size: 12px; color: $text-secondary; margin-top: 8px; }
.news-meta span { display: inline-flex; align-items: center; gap: 4px; }

.sub-list { list-style: none; padding: 0; margin: 0; }
.sub-list li { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px dashed $border-light; }
.sub-list li div { display: flex; gap: 8px; align-items: center; }
.sub-keyword { font-size: 13px; }

.w-full { width: 100%; }
.mt-12 { margin-top: 12px; }
.mt-16 { margin-top: 16px; }

.hot-list { list-style: none; padding: 0; margin: 0; counter-reset: hot; }
.hot-list li { display: flex; gap: 8px; align-items: center; padding: 8px 0; font-size: 13px; }
.hot-rank {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: $bg-page;
  color: $text-secondary;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600;
  &.top { background: $primary-color; color: #fff; }
}
.hot-title { flex: 1; color: $text-regular; cursor: pointer; &:hover { color: $primary-color; } }
.hot-view  { font-size: 12px; color: $text-secondary; }

.detail-meta { display: flex; gap: 12px; align-items: center; font-size: 12px; color: $text-secondary; margin-bottom: 16px; }
.detail-body { font-size: 14px; line-height: 1.9; color: $text-regular; }
.detail-body :deep(p) { margin-bottom: 12px; }
</style>
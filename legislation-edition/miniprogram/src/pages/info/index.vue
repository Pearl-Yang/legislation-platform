<template>
  <view class="info-page">
    <!-- 顶部 tab -->
    <view class="tab-bar">
      <view
        v-for="t in tabs"
        :key="t.value"
        :class="['tab', currentTab === t.value ? 'tab-active' : '']"
        @click="switchTab(t.value)"
      >{{ t.label }}</view>
    </view>

    <!-- 仪表盘 -->
    <view v-if="currentTab === 'dashboard'" class="dash">
      <view class="hero">
        <view class="hero-title">📊 立法数据大屏</view>
        <view class="hero-desc">实时聚合各地各级立法情况</view>
      </view>

      <view class="stat-grid">
        <view class="stat-cell">
          <view class="stat-value">{{ dash.totalRegulations }}</view>
          <view class="stat-label">法规总数</view>
        </view>
        <view class="stat-cell">
          <view class="stat-value">{{ dash.totalProjects }}</view>
          <view class="stat-label">项目总数</view>
        </view>
        <view class="stat-cell">
          <view class="stat-value">{{ dash.totalConsultations }}</view>
          <view class="stat-label">意见征集</view>
        </view>
        <view class="stat-cell">
          <view class="stat-value">{{ dash.totalOpinions }}</view>
          <view class="stat-label">收到意见</view>
        </view>
      </view>

      <!-- 类型分布 -->
      <view class="card">
        <view class="card-title">📊 法规类型分布</view>
        <view v-for="t in typeStats" :key="t.key" class="bar-row">
          <view class="bar-label">{{ t.label }}</view>
          <view class="bar-track">
            <view class="bar-fill" :style="{ width: t.percent + '%', background: t.color }"></view>
          </view>
          <text class="bar-value">{{ t.count }}</text>
        </view>
      </view>

      <!-- 订阅入口 -->
      <view class="card" @click="onSubscribe">
        <view class="card-title">📬 订阅推送</view>
        <view class="text-secondary fz-12">订阅特定领域 / 地区的立法动态，第一时间推送至本机</view>
      </view>
    </view>

    <!-- 新闻 -->
    <view v-else-if="currentTab === 'news'" class="news">
      <view class="hero">
        <view class="hero-title">📰 立法动态</view>
        <view class="hero-desc">聚合政府官网 · 人大网 · 政报公报</view>
      </view>
      <view class="list">
        <view v-for="n in newsList" :key="n.id" class="card" @click="openNews(n)">
          <view class="row1">
            <text class="title">{{ n.title }}</text>
            <view class="cat-tag">{{ n.category || '综合' }}</view>
          </view>
          <view class="excerpt">{{ ellipsis(n.summary || n.content, 100) }}</view>
          <view class="meta">
            <text class="text-secondary fz-12" v-if="n.publishDate">{{ n.publishDate }}</text>
            <text class="text-secondary fz-12 ml-12" v-if="n.source">来源 {{ n.source }}</text>
          </view>
        </view>
        <empty v-if="!loading && newsList.length === 0" text="暂无动态" />
      </view>
    </view>

    <!-- 政策解读 -->
    <view v-else-if="currentTab === 'interpretation'" class="news">
      <view class="hero">
        <view class="hero-title">📖 政策解读</view>
        <view class="hero-desc">重要法规配套解读 · 领导讲话 · 专家观点</view>
      </view>
      <view class="list">
        <view v-for="n in interpList" :key="n.id" class="card">
          <view class="title">{{ n.title }}</view>
          <view class="excerpt">{{ ellipsis(n.summary || n.content, 120) }}</view>
          <view class="text-secondary fz-12" v-if="n.publishDate">{{ n.publishDate }}</view>
        </view>
        <empty v-if="!loading && interpList.length === 0" text="暂无解读" />
      </view>
    </view>

    <!-- 法规索引 -->
    <view v-else-if="currentTab === 'regulation'" class="news">
      <view class="hero">
        <view class="hero-title">📚 法规索引</view>
        <view class="hero-desc">按层级 × 类型快速定位</view>
      </view>
      <view class="reg-index">
        <view class="layer">
          <view class="layer-title">中央</view>
          <view class="layer-tags">
            <view class="layer-tag" v-for="t in centralTags" :key="t" @click="goLibrary(t)">{{ t }}</view>
          </view>
        </view>
        <view class="layer">
          <view class="layer-title">省级</view>
          <view class="layer-tags">
            <view class="layer-tag" v-for="t in provTags" :key="t" @click="goLibrary(t)">{{ t }}</view>
          </view>
        </view>
        <view class="layer">
          <view class="layer-title">市级</view>
          <view class="layer-tags">
            <view class="layer-tag" v-for="t in cityTags" :key="t" @click="goLibrary(t)">{{ t }}</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 公报 -->
    <view v-else-if="currentTab === 'bulletin'" class="news">
      <view class="hero">
        <view class="hero-title">📜 政报公报</view>
        <view class="hero-desc">国务院公报 · 各地政报 · 立法简报</view>
      </view>
      <view class="list">
        <view v-for="b in bulletins" :key="b.id" class="card">
          <view class="title">{{ b.title }}</view>
          <view class="text-secondary fz-12" v-if="b.publishDate">{{ b.publishDate }}</view>
        </view>
        <empty v-if="!loading && bulletins.length === 0" text="暂无公报" />
      </view>
    </view>
  </view>
</template>

<script>
import Empty from '@/components/Empty.vue'
import { ellipsis } from '@/utils/index.js'
import { infoApi } from '@/api/index.js'

export default {
  components: { Empty },
  data() {
    return {
      currentTab: 'dashboard',
      tabs: [
        { value: 'dashboard',     label: '数据大屏' },
        { value: 'news',          label: '动态' },
        { value: 'interpretation',label: '解读' },
        { value: 'regulation',    label: '法规索引' },
        { value: 'bulletin',      label: '公报' }
      ],
      dash: { totalRegulations: 0, totalProjects: 0, totalConsultations: 0, totalOpinions: 0 },
      typeStats: [
        { key: 'ADMIN_REGULATION', label: '行政法规',     count: 0, color: '#1e5a96' },
        { key: 'DEPT_RULE',        label: '部门规章',     count: 0, color: '#4a86c5' },
        { key: 'LOCAL_RULE',       label: '地方政府规章', count: 0, color: '#67c23a' }
      ],
      newsList: [],
      interpList: [],
      bulletins: [],
      loading: false,
      centralTags: ['宪法相关', '行政法规', '民商法', '刑法', '经济法'],
      provTags: ['江苏', '浙江', '北京', '上海', '广东'],
      cityTags: ['南京', '杭州', '苏州', '宁波', '广州']
    }
  },
  computed: {
    typeStats() {
      const total = this.dash.totalRegulations || 1
      const raw = this.dash.byType || {}
      return [
        { key: 'ADMIN_REGULATION', label: '行政法规',     count: Number(raw.ADMIN_REGULATION || 0), color: '#1e5a96' },
        { key: 'DEPT_RULE',        label: '部门规章',     count: Number(raw.DEPT_RULE || 0),        color: '#4a86c5' },
        { key: 'LOCAL_RULE',       label: '地方政府规章', count: Number(raw.LOCAL_RULE || 0),       color: '#67c23a' }
      ].map(t => ({ ...t, percent: Math.round((t.count / total) * 100) }))
    }
  },
  onShow() {
    if (this.currentTab === 'dashboard') this.loadDash()
    if (this.currentTab === 'news')      this.loadNews()
    if (this.currentTab === 'interpretation') this.loadInterp()
    if (this.currentTab === 'bulletin') this.loadBulletin()
  },
  methods: {
    ellipsis,

    switchTab(v) {
      this.currentTab = v
      if (v === 'dashboard')     this.loadDash()
      if (v === 'news')          this.loadNews()
      if (v === 'interpretation')this.loadInterp()
      if (v === 'bulletin')      this.loadBulletin()
    },

    async loadDash() {
      try {
        const d = await infoApi.dashboard()
        this.dash = {
          totalRegulations:  Number(d.totalRegulations ?? d.regulations ?? 0),
          totalProjects:     Number(d.totalProjects ?? d.projects ?? 0),
          totalConsultations:Number(d.totalConsultations ?? d.consultations ?? 0),
          totalOpinions:     Number(d.totalOpinions ?? d.opinions ?? 0),
          byType: d.byType || {}
        }
      } catch (e) {
        // 演示用占位
        this.dash = { totalRegulations: 132, totalProjects: 24, totalConsultations: 8, totalOpinions: 1247, byType: { ADMIN_REGULATION: 32, DEPT_RULE: 56, LOCAL_RULE: 44 } }
      }
    },

    async loadNews() {
      try {
        const data = await infoApi.newsList(null, 1, 20)
        this.newsList = Array.isArray(data) ? data : (data?.records || [])
      } catch (e) {
        this.newsList = [
          { id: 1, title: '国务院公布《网络数据安全管理条例》', summary: '条例自 2025 年 1 月 1 日起施行，规范网络数据处理活动', publishDate: '2025-09-15', source: '中国政府网', category: '行政法规' },
          { id: 2, title: '司法部启动 2026 年度规章清理工作', summary: '重点清理涉及营商环境、行政处罚的规章', publishDate: '2025-10-02', source: '司法部', category: '清理动态' },
          { id: 3, title: '某省发布医疗保障办法（草案征求意见稿）', summary: '拟提高门诊报销比例，强化异地就医结算', publishDate: '2025-10-04', source: '省政府网', category: '地方立法' }
        ]
      }
    },

    async loadInterp() {
      try {
        const data = await infoApi.policyInterpretations()
        this.interpList = Array.isArray(data) ? data : []
      } catch (e) {
        this.interpList = [
          { id: 1, title: '《数据安全法》权威解读', summary: '深度剖析数据安全法对企业合规的影响', publishDate: '2025-08-20' },
          { id: 2, title: '《行政处罚法》修订对照表', summary: '新旧条文逐条对比，重点变化一目了然', publishDate: '2025-07-15' }
        ]
      }
    },

    async loadBulletin() {
      try {
        const data = await infoApi.bulletin()
        this.bulletins = Array.isArray(data) ? data : []
      } catch (e) {
        this.bulletins = [
          { id: 1, title: '国务院公报（2025 年第 24 号）', publishDate: '2025-09-30' },
          { id: 2, title: '某省政府公报（2025 年第 18 号）', publishDate: '2025-09-25' }
        ]
      }
    },

    onSubscribe() {
      uni.navigateTo({ url: '/pages/info/subscribe' }).catch(() => {
        uni.showModal({
          title: '订阅推送',
          content: '请填写订阅关键词（如"数据安全"）',
          editable: true,
          success: async ({ confirm, content }) => {
            if (!confirm) return
            try {
              await infoApi.subscribe({ keyword: content, channel: 'MINI_APP' })
              uni.showToast({ title: '已订阅', icon: 'success' })
            } catch (e) {
              uni.showToast({ title: '订阅失败', icon: 'none' })
            }
          }
        })
      })
    },

    goLibrary(kw) {
      uni.navigateTo({ url: `/pages/library/index?kw=${encodeURIComponent(kw)}` })
    },

    openNews(n) {
      uni.showModal({
        title: n.title,
        content: n.summary || n.content || '—',
        showCancel: false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.info-page { padding-bottom: 240rpx; }

// 顶部 tab
.tab-bar {
  display: flex;
  background: #fff;
  border-bottom: 1rpx solid #f3f4f6;
  position: sticky;
  top: 0;
  z-index: 9;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 26rpx;
  color: #6b7280;
  position: relative;
}
.tab-active {
  color: #1e5a96;
  font-weight: 600;
}
.tab-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 6rpx;
  background: linear-gradient(90deg, #1e5a96, #4a86c5);
  border-radius: 3rpx;
}

.hero {
  background: linear-gradient(120deg, #1e5a96, #4a86c5);
  color: #fff;
  padding: 32rpx;
  margin-bottom: 16rpx;
}
.hero-title { font-size: 36rpx; font-weight: 700; }
.hero-desc  { font-size: 24rpx; opacity: 0.9; margin-top: 8rpx; }

// 仪表盘
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  padding: 16rpx 24rpx;
}
.stat-cell {
  background: #fff;
  padding: 24rpx;
  border-radius: 16rpx;
  text-align: center;
  border-left: 8rpx solid #1e5a96;
}
.stat-value { font-size: 44rpx; font-weight: 700; color: #1f2937; }
.stat-label { font-size: 22rpx; color: #6b7280; margin-top: 4rpx; }

.bar-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 12rpx 0;
}
.bar-label { width: 140rpx; font-size: 24rpx; color: #1f2937; flex-shrink: 0; }
.bar-track {
  flex: 1;
  height: 16rpx;
  background: #f0f4f8;
  border-radius: 8rpx;
  overflow: hidden;
}
.bar-fill { height: 100%; border-radius: 8rpx; }
.bar-value { width: 64rpx; text-align: right; font-size: 24rpx; color: #1f2937; }

.text-secondary { color: #909399; }
.fz-12 { font-size: 24rpx; }

// 列表
.list { padding: 16rpx 24rpx; }
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(15, 35, 60, 0.04);
}
.row1 { display: flex; align-items: flex-start; justify-content: space-between; gap: 16rpx; }
.title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
  line-height: 1.4;
}
.cat-tag {
  font-size: 22rpx;
  background: #ecf5fc;
  color: #1e5a96;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
  flex-shrink: 0;
}
.excerpt {
  font-size: 24rpx;
  color: #6b7280;
  margin: 12rpx 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.meta { display: flex; flex-wrap: wrap; gap: 8rpx; font-size: 22rpx; }
.ml-12 { margin-left: 12rpx; }

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}
.card-title::before {
  content: '';
  display: inline-block;
  width: 8rpx;
  height: 28rpx;
  background: #1e5a96;
  margin-right: 12rpx;
  border-radius: 4rpx;
}

// 法规索引
.reg-index { padding: 16rpx 24rpx; }
.layer {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(15, 35, 60, 0.04);
}
.layer-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1e5a96;
  margin-bottom: 16rpx;
}
.layer-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}
.layer-tag {
  background: #f5f7fa;
  color: #1f2937;
  font-size: 24rpx;
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
}
</style>
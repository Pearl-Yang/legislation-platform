<template>
  <view class="graph-page">
    <view class="hero">
      <view class="hero-title">法规关系图谱</view>
      <view class="hero-desc">上下位 · 引用 · 替代 · 废止</view>
    </view>

    <view class="card">
      <view class="form-row">
        <text class="form-key">查看节点 ID</text>
        <view class="search-row">
          <input v-model="rootId" type="number" class="input" placeholder="如：1" />
          <view class="btn" @click="load">查询</view>
        </view>
      </view>
      <view class="form-row">
        <text class="form-key">深度</text>
        <view class="depth-row">
          <view
            v-for="d in [1, 2, 3]"
            :key="d"
            :class="['depth-tag', depth === d ? 'depth-active' : '']"
            @click="depth = d; load()"
          >{{ d }} 层</view>
        </view>
      </view>
    </view>

    <!-- 节点列表（展开式关系图） -->
    <view class="card" v-if="root">
      <view class="card-title">🎯 中心：{{ root.regulationName }}</view>

      <view class="rel-block">
        <view class="rel-row rel-up">
          <text class="rel-tag">↑ 上位法</text>
          <view class="rel-list">
            <view v-for="n in upNodes" :key="'u'+n.id" class="rel-node" @click="goNode(n.id)">
              {{ n.regulationName }}
            </view>
            <view v-if="!upNodes.length" class="text-secondary fz-12">无</view>
          </view>
        </view>

        <view class="rel-divider"></view>

        <view class="rel-row rel-self">
          <view class="rel-node self">{{ root.regulationName }}</view>
        </view>

        <view class="rel-divider"></view>

        <view class="rel-row rel-down">
          <text class="rel-tag">↓ 下位法</text>
          <view class="rel-list">
            <view v-for="n in downNodes" :key="'d'+n.id" class="rel-node" @click="goNode(n.id)">
              {{ n.regulationName }}
            </view>
            <view v-if="!downNodes.length" class="text-secondary fz-12">无</view>
          </view>
        </view>

        <view class="rel-divider"></view>

        <view class="rel-row rel-ref">
          <text class="rel-tag">⟲ 引用</text>
          <view class="rel-list">
            <view v-for="n in refNodes" :key="'r'+n.id" class="rel-node" @click="goNode(n.id)">
              {{ n.regulationName }}
            </view>
            <view v-if="!refNodes.length" class="text-secondary fz-12">无</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 关系边列表 -->
    <view class="card" v-if="edges.length">
      <view class="card-title">🔗 关系明细</view>
      <view v-for="(e, i) in edges" :key="i" class="edge-row">
        <text class="edge-name">{{ nodeName(e.sourceId) }}</text>
        <text :class="['edge-rel', `rel-${(e.relationType||'').toLowerCase()}`]">{{ relLabel(e.relationType) }}</text>
        <text class="edge-name">{{ nodeName(e.targetId) }}</text>
      </view>
    </view>

    <empty v-if="!root" text="请输入法规 ID 并点击查询" />
  </view>
</template>

<script>
import Empty from '@/components/Empty.vue'
import { cleanupApi } from '@/api/index.js'

export default {
  components: { Empty },
  data() {
    return {
      rootId: 1,
      depth: 2,
      root: null,
      nodes: [],
      edges: []
    }
  },
  computed: {
    upNodes()   { return this.nodes.filter(n => this.isUp(n)) },
    downNodes() { return this.nodes.filter(n => this.isDown(n)) },
    refNodes()  { return this.nodes.filter(n => !this.isUp(n) && !this.isDown(n)) }
  },
  methods: {
    relLabel(t) {
      const map = {
        UPPER: '上位',
        LOWER: '下位',
        REFER: '引用',
        REPLACE: '替代',
        ABOLISH: '废止'
      }
      return map[t] || t
    },
    isUp(n) {
      // 与 root 形成 LOWER 关系的，被视为 root 的上位
      return this.edges.some(e => e.sourceId === n.id && e.targetId === this.root.id && e.relationType === 'LOWER')
    },
    isDown(n) {
      return this.edges.some(e => e.sourceId === this.root.id && e.targetId === n.id && e.relationType === 'LOWER')
    },
    nodeName(id) {
      if (this.root && this.root.id === id) return this.root.regulationName
      const n = this.nodes.find(x => x.id === id)
      return n ? n.regulationName : '#' + id
    },

    async load() {
      try {
        const resp = await cleanupApi.regulationRelations(this.rootId, this.depth)
        this.root   = resp?.root || (resp?.nodes || []).find(n => n.id === Number(this.rootId)) || null
        this.nodes = (resp?.nodes || []).filter(n => n.id !== Number(this.rootId))
        this.edges = resp?.edges || []
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
        this.root = null
        this.nodes = []
        this.edges = []
      }
    },

    goNode(id) { this.rootId = id; this.load() }
  }
}
</script>

<style lang="scss" scoped>
.graph-page { padding: 24rpx 24rpx 200rpx; }
.hero {
  background: linear-gradient(120deg, #1e5a96, #4a86c5);
  color: #fff;
  padding: 32rpx;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
}
.hero-title { font-size: 36rpx; font-weight: 700; }
.hero-desc  { font-size: 24rpx; opacity: 0.9; margin-top: 8rpx; }

.form-row  { margin-bottom: 16rpx; }
.form-key  { font-size: 26rpx; color: #1f2937; display: block; margin-bottom: 8rpx; }
.search-row { display: flex; gap: 12rpx; align-items: center; }
.input {
  flex: 1;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}
.btn {
  background: linear-gradient(90deg, #1e5a96, #4a86c5);
  color: #fff;
  padding: 16rpx 32rpx;
  border-radius: 12rpx;
  font-size: 26rpx;
}

.depth-row { display: flex; gap: 12rpx; }
.depth-tag {
  padding: 12rpx 24rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: #6b7280;
}
.depth-active {
  background: #1e5a96;
  color: #fff;
  font-weight: 600;
}

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

.rel-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 0;
}
.rel-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.rel-tag {
  font-size: 24rpx;
  font-weight: 600;
  color: #1e5a96;
  flex-shrink: 0;
}
.rel-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  flex: 1;
}
.rel-node {
  background: #ecf5fc;
  color: #1e5a96;
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
}
.rel-node.self {
  background: linear-gradient(90deg, #1e5a96, #4a86c5);
  color: #fff;
  font-weight: 600;
  font-size: 28rpx;
}
.rel-divider {
  width: 2rpx;
  height: 24rpx;
  background: #d1d5db;
  margin: 8rpx 0;
}
.text-secondary { color: #909399; }
.fz-12 { font-size: 24rpx; }

.edge-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 8rpx;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-child { border-bottom: none; }
}
.edge-name {
  flex: 1;
  font-size: 26rpx;
  color: #1f2937;
  min-width: 0;
}
.edge-rel {
  background: #f0f4f8;
  color: #1e5a96;
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}
</style>
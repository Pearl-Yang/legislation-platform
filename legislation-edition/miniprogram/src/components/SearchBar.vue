<template>
  <view class="filter-bar">
    <view class="search-wrap">
      <view class="search-icon">🔍</view>
      <input
        class="search-input"
        v-model="kw"
        :placeholder="placeholder"
        confirm-type="search"
        @confirm="onSearch"
      />
      <view v-if="kw" class="search-clear" @click="clear">✕</view>
    </view>
    <slot />
  </view>
</template>

<script>
import { debounce } from '@/utils/index.js'

export default {
  name: 'SearchBar',
  props: {
    placeholder: { type: String, default: '搜索关键词' },
    value:      { type: String, default: '' }
  },
  emits: ['input', 'search'],
  data() { return { kw: this.value } },
  watch: {
    value(v) { this.kw = v }
  },
  mounted() {
    this.$emit('input', this.kw)
    this.debouncedEmit = debounce((val) => {
      this.$emit('search', val)
    }, 400)
  },
  methods: {
    onSearch() { this.$emit('search', this.kw) },
    clear() { this.kw = ''; this.$emit('search', '') }
  }
}
</script>

<style lang="scss" scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #f3f4f6;
}
.search-wrap {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-radius: 32rpx;
  padding: 0 24rpx;
  height: 64rpx;
}
.search-icon  { font-size: 24rpx; color: #909399; margin-right: 8rpx; }
.search-input { flex: 1; font-size: 26rpx; color: #1f2937; }
.search-clear {
  font-size: 22rpx;
  color: #909399;
  padding: 0 12rpx;
}
</style>
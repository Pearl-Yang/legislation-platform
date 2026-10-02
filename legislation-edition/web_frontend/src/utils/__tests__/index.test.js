import { describe, it, expect } from 'vitest'
import {
  formatDate,
  formatNumber,
  formatFileSize,
  highlightKeyword,
  daysFromNow,
  unwrap,
  debounce
} from '@/utils/index.js'

describe('utils/index.js', () => {
  it('formatDate 输出 YYYY-MM-DD', () => {
    expect(formatDate('2026-10-03')).toBe('2026-10-03')
  })

  it('formatNumber 千分位', () => {
    expect(formatNumber(1234567)).toBe('1,234,567')
    expect(formatNumber(null)).toBe('-')
    expect(formatNumber(0)).toBe('0')
  })

  it('formatFileSize 单位换算', () => {
    expect(formatFileSize(500)).toBe('500 B')
    expect(formatFileSize(2048)).toBe('2.0 KB')
    expect(formatFileSize(5 * 1024 * 1024)).toBe('5.00 MB')
    expect(formatFileSize(null)).toBe('-')
  })

  it('highlightKeyword 加 <mark>', () => {
    const out = highlightKeyword('中华人民共和国数据安全法', '数据')
    expect(out).toContain('<mark')
    expect(out).toContain('数据')
  })

  it('highlightKeyword 关键词空时返回原文', () => {
    expect(highlightKeyword('hello', '')).toBe('hello')
    expect(highlightKeyword('hello', null)).toBe('hello')
  })

  it('daysFromNow 距今天数', () => {
    expect(daysFromNow(null)).toBeNull()
    expect(typeof daysFromNow('2026-10-03')).toBe('number')
  })

  it('unwrap 兼容 Result 包壳', () => {
    expect(unwrap({ data: { a: 1 } })).toEqual({ a: 1 })
    expect(unwrap({ a: 2 })).toEqual({ a: 2 })
    expect(unwrap(null)).toEqual({})
    expect(unwrap(undefined)).toEqual({})
  })

  it('debounce 多次调用只触发最后一次', async () => {
    let count = 0
    const fn = debounce(() => { count++ }, 50)
    fn(); fn(); fn()
    await new Promise(r => setTimeout(r, 100))
    expect(count).toBe(1)
  })
})
#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试爬虫功能
"""
import sys
from pathlib import Path

# 添加项目根目录到路径
PROJECT_ROOT = Path(__file__).parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from config.settings import config
from spiders.spp_spiders import SppLawCrawler
from core.database import db

print("=" * 60)
print("爬虫测试开始")
print("=" * 60)

# 测试爬取一个页面
crawler = SppLawCrawler()
print(f"目标网站: {config.TARGET_SITES['law']['base_url']}")

# 获取列表页URL
list_url = config.TARGET_SITES['law']['list_urls'][0]
print(f"列表页: {list_url}")

try:
    print("正在爬取列表页...")
    items = crawler.crawl_list_page(
        list_url,
        crawler.site_config['selectors']['list_item']
    )
    print(f"发现 {len(items)} 个链接")
    
    if items:
        print(f"\n第一个链接: {items[0]['title']}")
        print(f"URL: {items[0]['url']}")
        
        print("\n正在爬取详情页...")
        detail_data = crawler.crawl_regulation_detail(items[0]['url'])
        
        if detail_data.get('success'):
            print(f"标题: {detail_data.get('title', '')}")
            print(f"发布部门: {detail_data.get('issue_dept', '')}")
            print(f发布日期: {detail_data.get('issue_date', '')}")
            print(f"分类: {detail_data.get('category', '')}")
            print(f"类型: {detail_data.get('regulation_type', '')}")
            
            # 保存到数据库
            print("\n正在保存到数据库...")
            sql = """
            INSERT INTO legal_regulation 
            (regulation_type, title, document_no, issue_dept, issue_date, 
             content, content_summary, category, keywords, source_url, status)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE 
            content = VALUES(content), 
            update_time = NOW()
            """
            
            params = (
                detail_data.get('regulation_type', ''),
                detail_data.get('title', ''),
                detail_data.get('document_no', ''),
                detail_data.get('issue_dept', ''),
                detail_data.get('issue_date'),
                detail_data.get('content', ''),
                detail_data.get('content_summary', ''),
                detail_data.get('category', ''),
                detail_data.get('keywords', ''),
                detail_data.get('source_url', ''),
                detail_data.get('status', 'ACTIVE')
            )
            
            db.execute(sql, params)
            print("[OK] 数据已保存")
        else:
            print(f"爬取失败: {detail_data.get('error', '未知错误')}")
    else:
        print("未找到任何链接")
        
except Exception as e:
    print(f"错误: {e}")
    import traceback
    traceback.print_exc()

print("\n" + "=" * 60)
print("爬虫测试完成")
print("=" * 60)

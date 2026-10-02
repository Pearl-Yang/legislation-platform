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
print("Crawler Test Started")
print("=" * 60)

# Test crawling a page
crawler = SppLawCrawler()
print(f"Target site: {config.TARGET_SITES['law']['base_url']}")

# Get list page URL
list_url = config.TARGET_SITES['law']['list_urls'][0]
print(f"List page: {list_url}")

try:
    print("Crawling list page...")
    items = crawler.crawl_list_page(
        list_url,
        crawler.site_config['selectors']['list_item']
    )
    print(f"Found {len(items)} links")
    
    if items:
        print(f"\nFirst link: {items[0]['title']}")
        print(f"URL: {items[0]['url']}")
        
        print("\nCrawling detail page...")
        detail_data = crawler.crawl_regulation_detail(items[0]['url'])
        
        if detail_data.get('success'):
            print(f"Title: {detail_data.get('title', '')}")
            print(f"Issue Dept: {detail_data.get('issue_dept', '')}")
            print(f"Issue Date: {detail_data.get('issue_date', '')}")
            print(f"Category: {detail_data.get('category', '')}")
            print(f"Type: {detail_data.get('regulation_type', '')}")
            
            # Save to database
            print("\nSaving to database...")
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
            print("[OK] Data saved")
        else:
            print(f"Crawl failed: {detail_data.get('error', 'unknown error')}")
    else:
        print("No links found")
        
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()

print("\n" + "=" * 60)
print("Crawler Test Completed")
print("=" * 60)

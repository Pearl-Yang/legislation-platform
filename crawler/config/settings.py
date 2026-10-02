"""
爬虫系统配置管理
"""
import os
from dataclasses import dataclass
from typing import List, Dict, Any
from pathlib import Path

@dataclass
class CrawlerConfig:
    """爬虫配置类"""
    
    # 基础配置
    BASE_DIR: Path = Path(__file__).parent.parent
    DATA_DIR: Path = BASE_DIR / "data"
    OUTPUT_DIR: Path = BASE_DIR / "output"
    LOG_DIR: Path = BASE_DIR / "logs"
    
    # 数据库配置
    DB_HOST: str = "localhost"
    DB_PORT: int = 3306
    DB_USER: str = "root"
    DB_PASSWORD: str = "123456"
    DB_NAME: str = "legal_gov"
    DB_CHARSET: str = "utf8mb4"
    
    # 爬虫配置
    USER_AGENT: str = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    REQUEST_TIMEOUT: int = 30
    RETRY_TIMES: int = 3
    RETRY_DELAY: int = 2
    BATCH_SIZE: int = 20
    
    # 目标网站配置
    TARGET_SITES: Dict[str, Dict[str, Any]] = None
    
    def __post_init__(self):
        """初始化后处理"""
        # 创建目录
        for dir_path in [self.DATA_DIR, self.OUTPUT_DIR, self.LOG_DIR]:
            dir_path.mkdir(parents=True, exist_ok=True)
        
        # 初始化目标网站配置
        self.TARGET_SITES = {
            "law": {
                "name": "法律法规（北京市检察院）",
                "base_url": "https://www.bjjc.gov.cn/",
                "list_urls": [
                    "https://www.bjjc.gov.cn/",  # 首页（包含多个栏目）
                ],
                "selectors": {
                    "list_item": ".list-box li a, .news-list li a, .article-list li a, a[href*='/xwzx/'], a[href*='/jcyw/'], a[href*='/jcjy/']",
                    "title": "h1, .title, .article-title",
                    "content": ".content-body, .content, .article-content, .main-content",
                    "publish_date": ".publish-date, .date, .time, [class*='date']",
                    "issue_dept": ".publish-dept, .dept, [class*='dept']",
                    "document_no": ".doc-no, .doc-no"
                },
                "pages_pattern": "index_\\d+\\.html",
                "max_pages": 5,
                "follow_external_links": False,  # 只爬取同域名链接
                "crawl_whole_site": True  # 爬取整个网站
            },
            "case": {
                "name": "典型案例（北京市检察院）",
                "base_url": "https://www.bjjc.gov.cn/",
                "list_urls": [
                    "https://www.bjjc.gov.cn/",  # 首页
                ],
                "selectors": {
                    "list_item": "a[href*='ajjj'], a[href*='zdal'], a[href*='gyss'], .list-box li a, .news-list li a",
                    "title": "h1, .title, .article-title, .case-title",
                    "case_type": ".case-type, .type, [class*='type']",
                    "court": ".court, .court-name, [class*='court']",
                    "facts": ".case-facts, .facts-content, [class*='facts']",
                    "result": ".case-result, .result-content, [class*='result']",
                    "legal_basis": ".legal-basis, .basis-content, [class*='basis']",
                    "publish_date": ".publish-date, .date, .time"
                },
                "pages_pattern": "index_\\d+\\.html",
                "max_pages": 5
            },
            "procedure": {
                "name": "案件流程规范（北京市检察院）",
                "base_url": "https://www.bjjc.gov.cn/",
                "list_urls": [
                    "https://www.bjjc.gov.cn/",
                ],
                "selectors": {
                    "list_item": ".list-box li a, .news-list li a, .article-list li a",
                    "title": "h1, .title, .article-title",
                    "content": ".content-body, .content, .article-content, .main-content",
                    "publish_date": ".publish-date, .date, .time",
                    "publish_dept": ".publish-dept, .dept"
                },
                "pages_pattern": "index_\\d+\\.html",
                "max_pages": 3
            },
            "document": {
                "name": "文书规范（北京市检察院）",
                "base_url": "https://www.bjjc.gov.cn/",
                "list_urls": [
                    "https://www.bjjc.gov.cn/",
                ],
                "selectors": {
                    "list_item": ".list-box li a, .news-list li a, .article-list li a",
                    "title": "h1, .title, .article-title",
                    "content": ".content-body, .content, .article-content"
                },
                "pages_pattern": "index_\\d+\\.html",
                "max_pages": 3
            },
            "news": {
                "name": "新闻资讯（北京市检察院）",
                "base_url": "https://www.bjjc.gov.cn/",
                "list_urls": [
                    "https://www.bjjc.gov.cn/",  # 首页包含新闻
                ],
                "selectors": {
                    "list_item": ".news-list li a, .article-list li a, a[href*='xwzx'], a[href*='xwfbh'], a[href*='mtbd']",
                    "title": "h1, .title, .article-title",
                    "content": ".content-body, .content, .article-content, .main-content",
                    "publish_date": ".publish-date, .date, .time, [class*='date']",
                    "author": ".author, [class*='author']"
                },
                "pages_pattern": "index_\\d+\\.html",
                "max_pages": 10
            },
            "theory": {
                "name": "理论研究（北京市检察院）",
                "base_url": "https://www.bjjc.gov.cn/",
                "list_urls": [
                    "https://www.bjjc.gov.cn/",
                ],
                "selectors": {
                    "list_item": "a[href*='yllw'], .list-box li a, .news-list li a",
                    "title": "h1, .title, .article-title",
                    "content": ".content-body, .content, .article-content, .main-content",
                    "publish_date": ".publish-date, .date, .time"
                },
                "pages_pattern": "index_\\d+\\.html",
                "max_pages": 5
            },
            "publicnotice": {
                "name": "公示公告（北京市检察院）",
                "base_url": "https://www.bjjc.gov.cn/",
                "list_urls": [
                    "https://www.bjjc.gov.cn/",
                ],
                "selectors": {
                    "list_item": "a[href*='gongshigonggao'], .list-box li a, .news-list li a",
                    "title": "h1, .title, .article-title",
                    "content": ".content-body, .content, .article-content, .main-content",
                    "publish_date": ".publish-date, .date, .time"
                },
                "pages_pattern": "index_\\d+\\.html",
                "max_pages": 5
            }
        }
    
    def get_site_config(self, site_key: str) -> Dict[str, Any]:
        """获取指定站点的配置"""
        return self.TARGET_SITES.get(site_key, {})
    
    def get_all_sites(self) -> List[str]:
        """获取所有站点key"""
        return list(self.TARGET_SITES.keys())

# 全局配置实例
config = CrawlerConfig()

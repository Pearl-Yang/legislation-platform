"""
爬虫基类
"""
import requests
import time
from typing import Optional, Dict, Any, List
from bs4 import BeautifulSoup
from dataclasses import dataclass, field
from datetime import datetime
from loguru import logger
from config.settings import config
from fake_useragent import UserAgent


@dataclass
class CrawlResult:
    """爬取结果数据类"""
    url: str
    title: str = ""
    content: str = ""
    data: Dict[str, Any] = field(default_factory=dict)
    crawl_time: datetime = field(default_factory=datetime.now)
    success: bool = True
    error_message: str = ""
    status_code: int = 0
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'url': self.url,
            'title': self.title,
            'content': self.content,
            'data': self.data,
            'crawl_time': self.crawl_time.isoformat(),
            'success': self.success,
            'error_message': self.error_message,
            'status_code': self.status_code
        }


class BaseCrawler:
    """爬虫基类"""
    
    def __init__(self, site_key: str = None):
        self.site_key = site_key
        self.session = requests.Session()
        self.ua = UserAgent()
        self.headers = {
            'User-Agent': config.USER_AGENT,
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
            'Accept-Encoding': 'gzip, deflate',
            'Connection': 'keep-alive',
        }
        self.timeout = config.REQUEST_TIMEOUT
        self.retry_times = config.RETRY_TIMES
        self.retry_delay = config.RETRY_DELAY
        
        # 统计信息
        self.stats = {
            'total': 0,
            'success': 0,
            'failed': 0,
            'skipped': 0
        }
    
    def get_random_ua(self) -> str:
        """获取随机User-Agent"""
        try:
            return self.ua.random
        except:
            return config.USER_AGENT
    
    def get(self, url: str, params: Dict = None, 
            headers: Dict = None, **kwargs) -> requests.Response:
        """发送GET请求（带重试）"""
        merged_headers = {**self.headers, **(headers or {})}
        merged_headers['User-Agent'] = self.get_random_ua()
        
        for attempt in range(self.retry_times):
            try:
                response = self.session.get(
                    url, 
                    params=params, 
                    headers=merged_headers,
                    timeout=self.timeout,
                    **kwargs
                )
                response.raise_for_status()
                response.encoding = response.apparent_encoding or 'utf-8'
                return response
            except requests.RequestException as e:
                logger.warning(f"请求失败 (尝试 {attempt + 1}/{self.retry_times}): {url} - {e}")
                if attempt < self.retry_times - 1:
                    time.sleep(self.retry_delay * (attempt + 1))
                else:
                    logger.error(f"请求最终失败: {url}")
                    raise
    
    def parse_html(self, html: str, parser: str = 'html.parser') -> BeautifulSoup:
        """解析HTML"""
        return BeautifulSoup(html, parser)
    
    def extract_text(self, element) -> str:
        """提取文本内容"""
        if element is None:
            return ""
        return element.get_text(strip=True)
    
    def extract_attr(self, element, attr: str) -> str:
        """提取属性"""
        if element is None:
            return ""
        return element.get(attr, "")
    
    def clean_text(self, text: str) -> str:
        """清理文本"""
        if not text:
            return ""
        # 移除多余空白字符
        text = ' '.join(text.split())
        # 移除特殊字符
        import re
        text = re.sub(r'[\x00-\x1f\x7f-\x9f]', '', text)
        return text.strip()
    
    def extract_by_selector(self, soup: BeautifulSoup, 
                           selector: str, default: str = "") -> str:
        """通过CSS选择器提取文本"""
        element = soup.select_one(selector)
        return self.clean_text(self.extract_text(element)) if element else default
    
    def extract_all_by_selector(self, soup: BeautifulSoup, 
                               selector: str) -> List[str]:
        """通过CSS选择器提取所有匹配项的文本"""
        elements = soup.select(selector)
        return [self.clean_text(self.extract_text(e)) for e in elements if e]
    
    def crawl_page(self, url: str, 
                   extractors: Dict[str, str] = None) -> CrawlResult:
        """爬取单个页面"""
        result = CrawlResult(url=url)
        
        try:
            response = self.get(url)
            result.status_code = response.status_code
            
            soup = self.parse_html(response.text)
            
            # 默认提取标题
            title = self.extract_by_selector(soup, 'title')
            if not title:
                title = self.extract_by_selector(soup, 'h1')
            result.title = title
            
            result.content = response.text
            
            # 执行自定义提取器
            if extractors:
                for key, selector in extractors.items():
                    result.data[key] = self.extract_by_selector(soup, selector)
            
            self.stats['success'] += 1
            logger.info(f"爬取成功: {url} - {result.title[:50]}")
            
        except Exception as e:
            result.success = False
            result.error_message = str(e)
            self.stats['failed'] += 1
            logger.error(f"爬取失败: {url} - {e}")
        
        finally:
            self.stats['total'] += 1
        
        return result
    
    def crawl_list_page(self, url: str, 
                       list_selector: str,
                       link_selector: str = 'a') -> List[Dict[str, Any]]:
        """爬取列表页，提取所有链接"""
        results = []
        
        try:
            response = self.get(url)
            soup = self.parse_html(response.text)
            
            list_items = soup.select(list_selector)
            logger.info(f"在列表页发现 {len(list_items)} 个链接: {url}")
            
            for item in list_items:
                link = item.select_one(link_selector)
                if link:
                    href = self.extract_attr(link, 'href')
                    if href:
                        # 处理相对URL
                        full_url = self.normalize_url(url, href)
                        title = self.extract_text(link)
                        
                        results.append({
                            'title': title,
                            'url': full_url,
                            'list_page': url
                        })
            
        except Exception as e:
            logger.error(f"列表页爬取失败: {url} - {e}")
        
        return results
    
    def normalize_url(self, base_url: str, href: str) -> str:
        """标准化URL"""
        from urllib.parse import urljoin, urlparse
        
        if href.startswith('http'):
            return href
        elif href.startswith('//'):
            parsed = urlparse(base_url)
            return f"{parsed.scheme}:{href}"
        else:
            return urljoin(base_url, href)
    
    def reset_stats(self):
        """重置统计"""
        self.stats = {'total': 0, 'success': 0, 'failed': 0, 'skipped': 0}
    
    def get_stats(self) -> Dict[str, int]:
        """获取统计信息"""
        return self.stats.copy()

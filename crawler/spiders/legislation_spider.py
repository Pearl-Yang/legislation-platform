"""
立法资料爬虫
=============

立法版扩展原料：
  - 中国政府网    https://www.gov.cn/   行政法规
  - 司法部        http://www.moj.gov.cn/ 部门规章
  - 各省/市司法厅局                 地方政府规章

数据落库目标：
  - regulation 表（与 government_edition 共用结构 + 扩展字段）
  - library_material 表

复用：
  - BaseCrawler（HTTP 请求/重试/反爬虫）
  - 数据库写入模块（db.execute_batch / db.insert）

TODO:
  - 各省政府规章抓取需要按省单独配置 source_url（增量添加）
  - 异构页面解析需分省份维护 CSS 选择器
"""
import re
from datetime import datetime
from typing import Dict, Any, List, Optional
from bs4 import BeautifulSoup
from core.base_crawler import BaseCrawler, CrawlResult
from config.settings import config as global_config
from loguru import logger


class LegislationCrawler(BaseCrawler):
    """
    行政立法资料爬虫。

    抓取目标：
        1) 中国政府网  - 行政法规
        2) 司法部官网  - 部门规章
        3) 各省司法厅  - 地方政府规章（按省扩展）
    """

    # 行政法规 / 部门规章 / 地方政府规章 的来源映射
    SOURCES = [
        {
            "key": "national_admin_regulation",
            "name": "行政法规（中央）",
            "regulation_type": "ADMIN_REGULATION",
            "base_url": "https://www.gov.cn/zhengce/index.htm",
            "region_code": "000000",
        },
        {
            "key": "moj_dept_rule",
            "name": "部门规章（司法部）",
            "regulation_type": "DEPT_RULE",
            "base_url": "http://www.moj.gov.cn/",
            "region_code": "000000",
        },
        {
            "key": "local_rule_jiangsu",
            "name": "地方政府规章（江苏省）",
            "regulation_type": "LOCAL_RULE",
            "base_url": "http://sft.jiangsu.gov.cn/",
            "region_code": "320000",
        },
        {
            "key": "local_rule_zhejiang",
            "name": "地方政府规章（浙江省）",
            "regulation_type": "LOCAL_RULE",
            "base_url": "http://sft.zj.gov.cn/",
            "region_code": "330000",
        },
        {
            "key": "local_rule_beijing",
            "name": "地方政府规章（北京市）",
            "regulation_type": "LOCAL_RULE",
            "base_url": "http://sfj.beijing.gov.cn/",
            "region_code": "110000",
        },
    ]

    # 通用列表项 / 详情页选择器（按站点微调）
    DEFAULT_SELECTORS = {
        "list_item":   ".news-list li a, .article-list li a, .law-list li a, a[href*='content'], a[href*='.htm']",
        "title":       "h1, .title, .article-title",
        "content":     ".content-body, .content, .article-content, #content",
        "publish_date":".publish-date, .date, .time",
        "issue_dept":  ".publish-dept, .dept, .source",
        "document_no": ".doc-no, .document-no, .number",
    }

    def __init__(self):
        super().__init__('legislation')
        # 注册源到全局配置
        global_config.TARGET_SITES = global_config.TARGET_SITES or {}
        global_config.TARGET_SITES['legislation'] = {
            "name": "行政立法资料",
            "base_url": "https://www.gov.cn/",
            "list_urls": [s["base_url"] for s in self.SOURCES],
            "selectors": self.DEFAULT_SELECTORS,
            "max_pages": 5,
        }
        self.site_config = global_config.TARGET_SITES['legislation']
        self.db_conn = None  # 由调用方注入

    # ========================================================================
    # 主入口
    # ========================================================================

    def crawl_all_sources(self, db_conn) -> Dict[str, Any]:
        """
        抓取所有来源，落库到 regulation / library_material 表。
        返回本次抓取统计。
        """
        self.db_conn = db_conn
        stats = {
            "total_sources": len(self.SOURCES),
            "success_sources": 0,
            "failed_sources": 0,
            "rows_fetched": 0,
            "rows_inserted": 0,
        }

        for src in self.SOURCES:
            try:
                logger.info(f"[Legislation] 开始抓取: {src['name']} ({src['base_url']})")
                rows = self.crawl_source(src)
                inserted = self._bulk_upsert(rows)
                stats["rows_fetched"] += len(rows)
                stats["rows_inserted"] += inserted
                stats["success_sources"] += 1
            except Exception as e:
                logger.error(f"[Legislation] 抓取 {src['name']} 失败: {e}")
                stats["failed_sources"] += 1

        logger.info(f"[Legislation] 全部抓取完成，统计: {stats}")
        return stats

    def crawl_source(self, source: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        抓取单个来源；返回结构化的 Regulation 字段列表。
        """
        base_url = source["base_url"]
        # 1) 抓列表页
        list_urls = self._discover_list_urls(base_url)
        items = []
        for url in list_urls[: self.site_config.get("max_pages", 5)]:
            result = self.crawl_page(url, self.DEFAULT_SELECTORS)
            if not result.success:
                continue
            items.extend(self._parse_list_page(result, source))
        # 2) 每条进入详情页
        regulations = []
        for item in items[: self.site_config.get("max_pages", 5) * 10]:
            detail = self._fetch_detail(item.get("url"))
            if not detail:
                continue
            merged = {**item, **detail, **source}
            merged["regulation_type"] = source["regulation_type"]
            merged["status"] = "EFFECTIVE"
            merged["is_from_crawler"] = 1
            merged["created_at"] = datetime.now()
            merged["updated_at"] = datetime.now()
            regulations.append(merged)
        return regulations

    # ========================================================================
    # 内部方法
    # ========================================================================

    def _discover_list_urls(self, base_url: str) -> List[str]:
        """从首页发现列表页 URL（简化实现：直接使用首页）"""
        return [base_url]

    def _parse_list_page(self, result: CrawlResult, source: Dict[str, Any]) -> List[Dict[str, Any]]:
        """解析列表页，提取条目标题 + URL"""
        soup = BeautifulSoup(result.content, "html.parser")
        items = []
        selector = self.DEFAULT_SELECTORS["list_item"]
        for a in soup.select(selector):
            href = a.get("href", "")
            title = (a.get_text() or "").strip()
            if not href or not title:
                continue
            # 处理相对路径
            url = self._absolute_url(source["base_url"], href)
            items.append({"title": title, "url": url})
        return items

    def _fetch_detail(self, url: str) -> Optional[Dict[str, Any]]:
        """抓取详情页"""
        try:
            result = self.crawl_page(url, self.DEFAULT_SELECTORS)
            if not result.success:
                return None
            soup = BeautifulSoup(result.content, "html.parser")
            content_el = soup.select_one(self.DEFAULT_SELECTORS["content"])
            full_text = content_el.get_text("\n", strip=True) if content_el else ""
            issue_no = self.extract_by_selector(soup, self.DEFAULT_SELECTORS["document_no"])
            issue_dept = self.extract_by_selector(soup, self.DEFAULT_SELECTORS["issue_dept"])
            issue_date_str = self.extract_by_selector(soup, self.DEFAULT_SELECTORS["publish_date"])
            issue_date = self._parse_date(issue_date_str)
            digest = (full_text[: 280] + "...") if len(full_text) > 280 else full_text
            return {
                "full_text": full_text,
                "digest": digest,
                "issue_number": issue_no,
                "issuing_authority": issue_dept,
                "issue_date": issue_date,
                "source_url": url,
            }
        except Exception as e:
            logger.warning(f"[Legislation] 详情抓取失败 {url}: {e}")
            return None

    def _absolute_url(self, base: str, href: str) -> str:
        """把相对路径拼成绝对 URL"""
        if href.startswith("http"):
            return href
        if href.startswith("//"):
            return "http:" + href
        if href.startswith("/"):
            from urllib.parse import urljoin
            return urljoin(base, href)
        return base.rstrip("/") + "/" + href.lstrip("/")

    def _parse_date(self, date_str: str) -> Optional[datetime]:
        """解析日期字符串"""
        if not date_str:
            return None
        date_str = date_str.strip()
        patterns = [
            r"(\d{4})-(\d{1,2})-(\d{1,2})",
            r"(\d{4})/(\d{1,2})/(\d{1,2})",
            r"(\d{4})\.(\d{1,2})\.(\d{1,2})",
        ]
        for pat in patterns:
            m = re.search(pat, date_str)
            if m:
                try:
                    return datetime(int(m.group(1)), int(m.group(2)), int(m.group(3)))
                except ValueError:
                    return None
        return None

    def _bulk_upsert(self, rows: List[Dict[str, Any]]) -> int:
        """
        把已抓 rows 批量 upsert 到 regulation 表 + library_material 表。
        返回成功插入行数。
        """
        if not rows or not self.db_conn:
            return 0
        inserted = 0
        try:
            cursor = self.db_conn.cursor()
            for r in rows:
                # 1) regulation 表
                sql_reg = """
                    INSERT INTO regulation
                        (regulation_name, regulation_type, issuing_authority,
                         issue_number, issue_date, effective_date, status,
                         full_text, digest, source_url, region_code,
                         is_from_crawler, created_at, updated_at)
                    VALUES
                        (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                        regulation_name = VALUES(regulation_name),
                        updated_at = CURRENT_TIMESTAMP
                """
                cursor.execute(sql_reg, (
                    r.get("title", ""),
                    r.get("regulation_type", "LOCAL_RULE"),
                    r.get("issuing_authority", ""),
                    r.get("issue_number", ""),
                    r.get("issue_date"),
                    None,                          # effective_date
                    r.get("status", "EFFECTIVE"),
                    r.get("full_text", ""),
                    r.get("digest", ""),
                    r.get("source_url", ""),
                    r.get("region_code", ""),
                    r.get("is_from_crawler", 1),
                    r.get("created_at"),
                    r.get("updated_at"),
                ))
                reg_id = cursor.lastrowid

                # 2) library_material 表（作为 REGULATION 类型的资料）
                sql_mat = """
                    INSERT INTO library_material
                        (title, material_type, region_code, issuing_authority,
                         issue_date, full_text, digest, file_url,
                         created_at, updated_at)
                    VALUES
                        (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """
                cursor.execute(sql_mat, (
                    r.get("title", ""),
                    "REGULATION",
                    r.get("region_code", ""),
                    r.get("issuing_authority", ""),
                    r.get("issue_date"),
                    r.get("full_text", ""),
                    r.get("digest", ""),
                    r.get("source_url", ""),
                    r.get("created_at"),
                    r.get("updated_at"),
                ))
                inserted += 1
            self.db_conn.commit()
        except Exception as e:
            logger.error(f"[Legislation] 批量入库失败: {e}")
            self.db_conn.rollback()
        return inserted
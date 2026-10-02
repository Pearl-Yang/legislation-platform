"""
检察院官网爬虫实现
"""
import re
from datetime import datetime
from typing import Dict, Any, Optional
from bs4 import BeautifulSoup
from core.base_crawler import BaseCrawler, CrawlResult
from config.settings import config
from loguru import logger


class SppLawCrawler(BaseCrawler):
    """最高检法律法规爬虫"""
    
    def __init__(self):
        super().__init__('law')
        self.site_config = config.get_site_config('law')
    
    def crawl_regulation_detail(self, url: str) -> Dict[str, Any]:
        """爬取法规详情页"""
        logger.info(f"开始爬取法规详情: {url}")
        
        result = self.crawl_page(url, {
            'title': '.title, h1',
            'content': '.content-body, .content',
            'publish_date': '.publish-date, .date',
            'issue_dept': '.publish-dept, .dept',
            'document_no': '.doc-no, .doc-no'
        })
        
        if not result.success:
            return {'success': False, 'error': result.error_message}
        
        data = self._parse_regulation_content(result)
        data['source_url'] = url
        data['crawl_time'] = datetime.now()
        data['status'] = 'ACTIVE'
        
        logger.info(f"法规解析完成: {data.get('title', '')}")
        return data
    
    def _parse_regulation_content(self, result: CrawlResult) -> Dict[str, Any]:
        """解析法规内容"""
        data = {}
        soup = BeautifulSoup(result.content, 'html.parser')
        
        # 提取标题
        data['title'] = result.data.get('title', '')
        if not data['title']:
            data['title'] = self.extract_by_selector(soup, 'h1')
        
        # 提取发布时间和部门
        date_str = result.data.get('publish_date', '')
        dept_str = result.data.get('issue_dept', '')
        
        if not date_str:
            date_str = self._find_publish_date(soup)
        if not dept_str:
            dept_str = self._find_issue_dept(soup)
        
        data['issue_date'] = self._parse_date(date_str)
        data['issue_dept'] = dept_str
        data['document_no'] = result.data.get('document_no', '')
        
        # 提取正文内容
        content = self._extract_main_content(soup)
        data['content'] = content
        data['content_summary'] = self._generate_summary(content)
        
        # 分类和关键词
        data['category'] = self._classify_regulation(data['title'], content)
        data['keywords'] = self._extract_keywords(content)
        
        # 推断类型
        data['regulation_type'] = self._infer_regulation_type(data['title'])
        
        return data
    
    def _find_publish_date(self, soup: BeautifulSoup) -> str:
        """查找发布时间"""
        # 多种选择器
        selectors = [
            '.publish-date',
            '.date',
            '.time',
            '[class*="date"]',
            '[class*="time"]',
            'span[class*="date"]',
            'div[class*="date"]'
        ]
        
        for selector in selectors:
            element = soup.select_one(selector)
            if element:
                text = self.extract_text(element)
                if text and re.search(r'\d{4}[-年]\d{1,2}[-月]\d{1,2}', text):
                    return text
        
        return ""
    
    def _find_issue_dept(self, soup: BeautifulSoup) -> str:
        """查找发布部门"""
        dept_keywords = ['发布', '颁布', '部', '院', '局', '委员会']
        
        for element in soup.find_all(['span', 'div', 'p']):
            text = self.extract_text(element)
            if any(kw in text for kw in dept_keywords):
                return text
        
        return ""
    
    def _extract_main_content(self, soup: BeautifulSoup) -> str:
        """提取正文内容"""
        # 尝试多种内容区域选择器
        content_selectors = [
            '.content-body',
            '.content',
            '.article-content',
            '.main-content',
            '#content',
            '#mainContent',
            'article',
            '.text-box'
        ]
        
        for selector in content_selectors:
            element = soup.select_one(selector)
            if element:
                # 移除不需要的元素
                for unwanted in element.select('script, style, .share, .nav, footer, aside'):
                    unwanted.decompose()
                return self.clean_text(element.get_text())
        
        # 如果没找到特定区域，返回body的清理版本
        body = soup.find('body')
        if body:
            for unwanted in body.select('script, style, header, footer, nav, aside'):
                unwanted.decompose()
            return self.clean_text(body.get_text())
        
        return ""
    
    def _generate_summary(self, content: str, max_len: int = 500) -> str:
        """生成摘要"""
        if not content:
            return ""
        
        # 简单摘要：取前max_len个字符
        summary = content[:max_len]
        if len(content) > max_len:
            summary += "..."
        
        return summary
    
    def _parse_date(self, date_str: str) -> Optional[datetime]:
        """解析日期字符串"""
        if not date_str:
            return None
        
        date_str = re.sub(r'[年月]', '-', date_str)
        date_str = re.sub(r'[日号]', '', date_str)
        
        patterns = [
            r'(\d{4}-\d{1,2}-\d{1,2})',
            r'(\d{4}/\d{1,2}/\d{1,2})',
            r'(\d{4}\.\d{1,2}\.\d{1,2})',
        ]
        
        for pattern in patterns:
            match = re.search(pattern, date_str)
            if match:
                try:
                    return datetime.strptime(match.group(1), '%Y-%m-%d')
                except:
                    pass
        
        return None
    
    def _classify_regulation(self, title: str, content: str) -> str:
        """分类法规"""
        text = (title + content).lower()
        
        category_map = {
            '刑事诉讼': ['刑事', '刑诉', '犯罪', '逮捕', '起诉'],
            '民事诉讼': ['民事', '民诉', '合同', '侵权', '婚姻'],
            '行政诉讼': ['行政', '政诉', '行政复议', '行政强制'],
            '公益诉讼': ['公益', '环境', '公共利益'],
            '检察监督': ['检察', '监督', '检察建议'],
            '机构设置': ['机构', '编制', '组织法'],
        }
        
        for category, keywords in category_map.items():
            if any(kw in text for kw in keywords):
                return category
        
        return "其他"
    
    def _extract_keywords(self, content: str, max_count: int = 10) -> str:
        """提取关键词（简单实现）"""
        import jieba.analyse as analyse
        
        try:
            keywords = analyse.extract_tags(content, topK=max_count)
            return ','.join(keywords)
        except:
            # 备用：提取重要名词
            words = re.findall(r'[\u4e00-\u9fa5]{2,4}', content)
            return ','.join(words[:max_count])
    
    def _infer_regulation_type(self, title: str) -> str:
        """推断法规类型"""
        title_lower = title.lower()
        
        if '法' in title:
            return 'law'
        elif '条例' in title or '条例' in title:
            return 'regulation'
        elif '规定' in title or '办法' in title or '细则' in title:
            return 'rules'
        elif '解释' in title or '批复' in title:
            return 'interpretation'
        else:
            return 'normative'


class SppCaseCrawler(BaseCrawler):
    """典型案例爬虫"""
    
    def __init__(self):
        super().__init__('case')
        self.site_config = config.get_site_config('case')
    
    def crawl_case_detail(self, url: str) -> Dict[str, Any]:
        """爬取案例详情页"""
        logger.info(f"开始爬取案例详情: {url}")
        
        result = self.crawl_page(url, {
            'title': '.case-title, .title, h1',
            'case_type': '.case-type, .type',
            'court': '.court, .court-name',
            'facts': '.case-facts, .facts-content',
            'result': '.case-result, .result-content',
            'legal_basis': '.legal-basis, .basis-content',
            'publish_date': '.publish-date, .date'
        })
        
        if not result.success:
            return {'success': False, 'error': result.error_message}
        
        data = self._parse_case_content(result)
        data['source_url'] = url
        data['crawl_time'] = datetime.now()
        
        logger.info(f"案例解析完成: {data.get('case_title', '')}")
        return data
    
    def _parse_case_content(self, result: CrawlResult) -> Dict[str, Any]:
        """解析案例内容"""
        data = {}
        soup = BeautifulSoup(result.content, 'html.parser')
        
        # 基本信息
        data['case_title'] = result.data.get('title', '')
        data['case_type'] = result.data.get('case_type', '')
        data['court'] = result.data.get('court', '')
        
        # 提取关键信息
        data['judge_name'] = self._extract_judge(soup)
        data['prosecutor_name'] = self._extract_prosecutor(soup)
        data['defendant'] = self._extract_defendant(soup)
        
        # 时间信息
        date_str = result.data.get('publish_date', '')
        data['publish_date'] = self._parse_date(date_str)
        
        # 内容部分
        data['facts'] = self._clean_content(result.data.get('facts', ''))
        data['trial_process'] = self._extract_trial_process(soup)
        data['judgment_result'] = self._clean_content(result.data.get('result', ''))
        data['legal_basis'] = self._clean_content(result.data.get('legal_basis', ''))
        
        # 提取案由
        data['case_cause'] = self._extract_case_cause(data['facts'])
        
        # 生成标签
        data['tags'] = self._generate_tags(data)
        
        # 生成摘要
        data['case_highlight'] = self._generate_highlight(data)
        data['case_significance'] = self._extract_significance(soup)
        
        return data
    
    def _extract_judge(self, soup: BeautifulSoup) -> str:
        """提取审判长/承办检察官"""
        patterns = [r'审判长[：:]\s*(\S+)', r'承办检察官[：:]\s*(\S+)']
        text = self.clean_text(soup.get_text())
        
        for pattern in patterns:
            match = re.search(pattern, text)
            if match:
                return match.group(1)
        
        return ""
    
    def _extract_prosecutor(self, soup: BeautifulSoup) -> str:
        """提取公诉人"""
        patterns = [r'公诉人[：:]\s*(\S+)', r'检察院[：:]\s*(\S+)']
        text = self.clean_text(soup.get_text())
        
        for pattern in patterns:
            match = re.search(pattern, text)
            if match:
                return match.group(1)
        
        return ""
    
    def _extract_defendant(self, soup: BeautifulSoup) -> str:
        """提取被告人"""
        patterns = [r'被告人[：:]\s*(\S+)', r'被告[：:]\s*(\S+)']
        text = self.clean_text(soup.get_text())
        
        for pattern in patterns:
            match = re.search(pattern, text)
            if match:
                return match.group(1)
        
        return ""
    
    def _extract_trial_process(self, soup: BeautifulSoup) -> str:
        """提取审理过程"""
        # 查找"审理过程"、"庭审经过"等标题
        headings = soup.find_all(['h2', 'h3', 'h4', 'strong', 'p'])
        
        for heading in headings:
            text = self.extract_text(heading)
            if '审理' in text or '庭审' in text or '过程' in text:
                # 获取后续内容
                content = []
                next_elem = heading.find_next_sibling()
                while next_elem and next_elem.name not in ['h2', 'h3', 'h4']:
                    content.append(self.extract_text(next_elem))
                    next_elem = next_elem.find_next_sibling()
                
                if content:
                    return self.clean_text('\n'.join(content))
        
        return ""
    
    def _extract_case_cause(self, facts: str) -> str:
        """提取案由"""
        if not facts:
            return ""
        
        case_keywords = [
            '盗窃', '抢劫', '诈骗', '贪污', '受贿', '滥用职权', 
            '故意伤害', '交通肇事', '非法拘禁', '敲诈勒索',
            '合同纠纷', '侵权', '婚姻家庭', '劳动争议',
            '行政', '复议', '行政处罚', '行政许可'
        ]
        
        for keyword in case_keywords:
            if keyword in facts:
                return keyword
        
        return ""
    
    def _generate_tags(self, data: Dict[str, Any]) -> str:
        """生成标签"""
        tags = []
        
        if data.get('case_type'):
            tags.append(data['case_type'])
        
        if data.get('case_cause'):
            tags.append(data['case_cause'])
        
        # 提取年份
        publish_date = data.get('publish_date')
        if publish_date and isinstance(publish_date, datetime):
            tags.append(str(publish_date.year))
        
        return ','.join(tags)
    
    def _generate_highlight(self, data: Dict[str, Any]) -> str:
        """生成案例亮点"""
        highlights = []
        
        result = data.get('judgment_result', '')
        if '典型' in result or '指导' in result:
            highlights.append('具有典型指导意义')
        
        facts = data.get('facts', '')
        if '新型' in facts or '疑难' in facts:
            highlights.append('新型疑难案件')
        
        return ';'.join(highlights) if highlights else "本案具有参考价值"
    
    def _extract_significance(self, soup: BeautifulSoup) -> str:
        """提取典型意义"""
        # 查找"典型意义"部分
        for heading in soup.find_all(['h2', 'h3', 'h4', 'strong']):
            text = self.extract_text(heading)
            if '典型意义' in text or '指导意义' in text:
                content = []
                next_elem = heading.find_next_sibling()
                while next_elem and next_elem.name not in ['h2', 'h3', 'h4']:
                    content.append(self.extract_text(next_elem))
                    next_elem = next_elem.find_next_sibling()
                
                if content:
                    return self.clean_text('\n'.join(content))
        
        return ""
    
    def _clean_content(self, content: str) -> str:
        """清理内容"""
        if not content:
            return ""
        return self.clean_text(content).replace('\n', ' ').replace('\r', '')


class SppProcedureCrawler(BaseCrawler):
    """案件流程规范爬虫"""
    
    def __init__(self):
        super().__init__('procedure')
        self.site_config = config.get_site_config('procedure')
    
    def crawl_procedure_detail(self, url: str) -> Dict[str, Any]:
        """爬取流程规范详情"""
        logger.info(f"开始爬取流程规范: {url}")
        
        result = self.crawl_page(url)
        
        if not result.success:
            return {'success': False, 'error': result.error_message}
        
        data = self._parse_procedure_content(result)
        data['source_url'] = url
        data['crawl_time'] = datetime.now()
        
        logger.info(f"流程规范解析完成: {data.get('procedure_name', '')}")
        return data
    
    def _parse_procedure_content(self, result: CrawlResult) -> Dict[str, Any]:
        """解析流程规范内容"""
        data = {}
        soup = BeautifulSoup(result.content, 'html.parser')
        
        # 提取标题
        data['procedure_name'] = result.data.get('title', '') or self.extract_by_selector(soup, 'h1')
        
        # 生成流程节点编码
        data['procedure_code'] = self._generate_procedure_code(data['procedure_name'])
        
        # 提取正文
        content = self._extract_main_content(soup)
        data['description'] = content
        
        # 提取法律依据
        data['legal_basis'] = self._extract_legal_basis(soup, content)
        
        # 推断类别和阶段
        data['category'] = self._infer_category(data['procedure_name'], content)
        data['stage'] = self._infer_stage(data['procedure_name'], content)
        
        # 提取时限
        data['time_limit_days'] = self._extract_time_limit(content)
        data['time_limit_basis'] = self._find_time_limit_basis(content)
        
        # 责任主体
        data['responsible_role'] = self._extract_responsible_role(content)
        
        # 关键要点和常见问题
        data['key_points'] = self._extract_key_points(content)
        data['common_problems'] = self._extract_common_problems(content)
        
        # 必需文书
        data['required_documents'] = self._extract_required_documents(content)
        
        return data
    
    def _generate_procedure_code(self, name: str) -> str:
        """生成流程节点编码"""
        code_map = {
            '受理': 'RECEIVE',
            '立案': 'FILING',
            '审查': 'REVIEW',
            '起诉': 'PROSECUTION',
            '审判': 'TRIAL',
            '执行': 'ENFORCE',
            '结案': 'CLOSE',
            '归档': 'ARCHIVE'
        }
        
        for key, code in code_map.items():
            if key in name:
                return f"PROC_{code}"
        
        return f"PROC_{hash(name) % 10000:04d}"
    
    def _infer_category(self, name: str, content: str) -> str:
        """推断案件类别"""
        text = (name + content).lower()
        
        if '刑事' in text or '刑案' in text:
            return 'criminal'
        elif '民事' in text or '民案' in text:
            return 'civil'
        elif '行政' in text:
            return 'administrative'
        elif '检察' in text or '监督' in text:
            return 'procuratorial'
        else:
            return 'general'
    
    def _infer_stage(self, name: str, content: str) -> str:
        """推断所属阶段"""
        text = name + content
        
        stage_map = {
            '受理': 'receiving',
            '立案': 'filing',
            '审查': 'review',
            '起诉': 'prosecution',
            '审判': 'trial',
            '执行': 'execution',
            '结案': 'completion',
            '归档': 'archiving'
        }
        
        for key, stage in stage_map.items():
            if key in text:
                return stage
        
        return 'unknown'
    
    def _extract_main_content(self, soup: BeautifulSoup) -> str:
        """提取正文内容"""
        selectors = [
            '.content-body',
            '.spec-content',
            '.content',
            '#content',
            '.main-content',
            'article'
        ]
        
        for selector in selectors:
            element = soup.select_one(selector)
            if element:
                for unwanted in element.select('script, style, .share, .nav'):
                    unwanted.decompose()
                return self.clean_text(element.get_text())
        
        return self.clean_text(soup.get_text())
    
    def _extract_legal_basis(self, soup: BeautifulSoup, content: str) -> str:
        """提取法律依据"""
        basis_patterns = [
            r'根据[《]?.*?法[》]?[的]?规定',
            r'依据[《]?.*?条例[》]?[的]?规定',
            r'参照.*?第.*?条',
        ]
        
        results = []
        
        # 从内容中提取
        for pattern in basis_patterns:
            matches = re.findall(pattern, content)
            results.extend(matches)
        
        # 从专门的法律依据区域提取
        basis_element = soup.select_one('.legal-basis, .basis, .law-ref')
        if basis_element:
            results.append(self.clean_text(basis_element.get_text()))
        
        return ';'.join(results) if results else ""
    
    def _extract_time_limit(self, content: str) -> int:
        """提取时限（天数）"""
        patterns = [
            r'(\d+)个工作日',
            r'(\d+)日内',
            r'期限为(\d+)天',
            r'(\d+)天',
        ]
        
        for pattern in patterns:
            match = re.search(pattern, content)
            if match:
                return int(match.group(1))
        
        return 0
    
    def _find_time_limit_basis(self, content: str) -> str:
        """查找时限依据"""
        patterns = [
            r'根据.*?第.*?条.*?(\d+)',
            r'《.*?》.*?(\d+)',
        ]
        
        for pattern in patterns:
            match = re.search(pattern, content)
            if match:
                return match.group(0)
        
        return ""
    
    def _extract_responsible_role(self, content: str) -> str:
        """提取责任主体"""
        roles = ['检察官', '法官', '审判长', '书记员', '法院', '检察院']
        
        for role in roles:
            if role in content:
                return role
        
        return "承办人"
    
    def _extract_key_points(self, content: str) -> str:
        """提取关键要点"""
        # 查找编号列表或要点段落
        key_points = []
        
        lines = content.split('\n')
        for line in lines:
            line = line.strip()
            if any(marker in line for marker in ['1.', '2.', '3.', '（1）', '（2）', '①', '②']):
                key_points.append(line)
        
        return '|'.join(key_points[:10]) if key_points else ""
    
    def _extract_common_problems(self, content: str) -> str:
        """提取常见问题"""
        problems = []
        
        # 查找问题、注意事项部分
        sections = re.findall(r'[注意|问题|常见].*?[:：](.*?)(?=\n\n|\Z)', content)
        problems.extend(sections)
        
        return '|'.join(problems[:5]) if problems else ""
    
    def _extract_required_documents(self, content: str) -> str:
        """提取必需文书"""
        doc_keywords = ['起诉书', '判决书', '裁定书', '决定书', '通知书', '笔录', '清单']
        found = [kw for kw in doc_keywords if kw in content]
        
        return ','.join(found) if found else ""

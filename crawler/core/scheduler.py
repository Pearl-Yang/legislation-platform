"""
爬虫任务调度器
"""
import asyncio
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional
from core.database import db
from spiders.spp_spiders import SppLawCrawler, SppCaseCrawler, SppProcedureCrawler
from report.generator import report_generator
from loguru import logger
from config.settings import config
import schedule
import time
import threading


class CrawlerScheduler:
    """爬虫任务调度器"""
    
    def __init__(self):
        self.db = db
        self.running = False
        self.scheduler_thread = None
    
    def execute_all_tasks(self) -> Dict[str, Any]:
        """执行所有爬取任务"""
        logger.info("开始执行全量数据爬取...")
        
        results = {
            'total_tasks': 0,
            'success_tasks': 0,
            'failed_tasks': 0,
            'details': []
        }
        
        # 1. 爬取法律法规
        logger.info("=" * 50)
        logger.info("任务1: 爬取法律法规")
        law_crawler = SppLawCrawler()
        law_sites = config.get_site_config('law')['list_urls']
        
        for site_url in law_sites:
            try:
                logger.info(f"正在爬取: {site_url}")
                list_results = law_crawler.crawl_list_page(
                    site_url,
                    law_crawler.site_config['selectors']['list_item']
                )
                
                for item in list_results[:3]:  # 限制每个列表爬3个详情
                    detail_data = law_crawler.crawl_regulation_detail(item['url'])
                    if detail_data.get('success'):
                        # 保存到数据库
                        self._save_regulation(detail_data)
                        results['success_tasks'] += 1
                    else:
                        results['failed_tasks'] += 1
                    
                    results['total_tasks'] += 1
                    time.sleep(1)  # 礼貌延迟
                
            except Exception as e:
                logger.error(f"法律法规爬取失败: {e}")
                results['failed_tasks'] += 1
        
        # 2. 爬取典型案例
        logger.info("=" * 50)
        logger.info("任务2: 爬取典型案例")
        case_crawler = SppCaseCrawler()
        case_sites = config.get_site_config('case')['list_urls']
        
        for site_url in case_sites:
            try:
                logger.info(f"正在爬取: {site_url}")
                list_results = case_crawler.crawl_list_page(
                    site_url,
                    case_crawler.site_config['selectors']['list_item']
                )
                
                for item in list_results[:3]:  # 限制每个列表爬3个
                    detail_data = case_crawler.crawl_case_detail(item['url'])
                    if detail_data.get('success'):
                        self._save_case(detail_data)
                        results['success_tasks'] += 1
                    else:
                        results['failed_tasks'] += 1
                    
                    results['total_tasks'] += 1
                    time.sleep(1)
                
            except Exception as e:
                logger.error(f"典型案例爬取失败: {e}")
                results['failed_tasks'] += 1
        
        # 3. 爬取流程规范
        logger.info("=" * 50)
        logger.info("任务3: 爬取案件流程规范")
        proc_crawler = SppProcedureCrawler()
        proc_sites = config.get_site_config('procedure')['list_urls']
        
        for site_url in proc_sites:
            try:
                logger.info(f"正在爬取: {site_url}")
                list_results = proc_crawler.crawl_list_page(
                    site_url,
                    proc_crawler.site_config['selectors']['list_item']
                )
                
                for item in list_results[:2]:  # 限制数量
                    detail_data = proc_crawler.crawl_procedure_detail(item['url'])
                    if detail_data.get('success'):
                        self._save_procedure(detail_data)
                        results['success_tasks'] += 1
                    else:
                        results['failed_tasks'] += 1
                    
                    results['total_tasks'] += 1
                    time.sleep(1)
                
            except Exception as e:
                logger.error(f"流程规范爬取失败: {e}")
                results['failed_tasks'] += 1
        
        # 记录任务日志
        self._log_task('COMPREHENSIVE', results)
        
        # 4. 生成报告
        logger.info("=" * 50)
        logger.info("任务4: 生成需求调研报告")
        try:
            report_files = report_generator.generate_full_report()
            results['details'].append({
                'task': 'report_generation',
                'success': True,
                'files': report_files
            })
            results['success_tasks'] += 1
        except Exception as e:
            logger.error(f"报告生成失败: {e}")
            results['failed_tasks'] += 1
        
        results['total_tasks'] += 1
        
        logger.info("=" * 50)
        logger.info(f"爬取任务完成: 总计 {results['total_tasks']}, "
                   f"成功 {results['success_tasks']}, "
                   f"失败 {results['failed_tasks']}")
        
        return results
    
    def execute_single_task(self, task_type: str, source_name: str = None) -> Dict[str, Any]:
        """执行单个任务"""
        logger.info(f"执行单个任务: {task_type} - {source_name}")
        
        result = {
            'task_type': task_type,
            'source': source_name,
            'success': False,
            'message': '',
            'stats': {}
        }
        
        try:
            if task_type == 'regulation':
                crawler = SppLawCrawler()
                urls = [source_name] if source_name else config.get_site_config('law')['list_urls']
                count = self._crawl_regulations(crawler, urls)
                result['stats']['count'] = count
                
            elif task_type == 'case':
                crawler = SppCaseCrawler()
                urls = [source_name] if source_name else config.get_site_config('case')['list_urls']
                count = self._crawl_cases(crawler, urls)
                result['stats']['count'] = count
                
            elif task_type == 'procedure':
                crawler = SppProcedureCrawler()
                urls = [source_name] if source_name else config.get_site_config('procedure')['list_urls']
                count = self._crawl_procedures(crawler, urls)
                result['stats']['count'] = count
            
            result['success'] = True
            result['message'] = '任务执行成功'
            
        except Exception as e:
            result['success'] = False
            result['message'] = str(e)
            logger.error(f"任务执行失败: {e}")
        
        return result
    
    def _crawl_regulations(self, crawler: SppLawCrawler, urls: List[str]) -> int:
        """爬取法律法规"""
        count = 0
        for url in urls[:2]:  # 限制
            items = crawler.crawl_list_page(
                url,
                crawler.site_config['selectors']['list_item']
            )
            for item in items[:3]:
                data = crawler.crawl_regulation_detail(item['url'])
                if data.get('success'):
                    self._save_regulation(data)
                    count += 1
                time.sleep(1)
        return count
    
    def _crawl_cases(self, crawler: SppCaseCrawler, urls: List[str]) -> int:
        """爬取典型案例"""
        count = 0
        for url in urls[:2]:
            items = crawler.crawl_list_page(
                url,
                crawler.site_config['selectors']['list_item']
            )
            for item in items[:3]:
                data = crawler.crawl_case_detail(item['url'])
                if data.get('success'):
                    self._save_case(data)
                    count += 1
                time.sleep(1)
        return count
    
    def _crawl_procedures(self, crawler: SppProcedureCrawler, urls: List[str]) -> int:
        """爬取流程规范"""
        count = 0
        for url in urls[:1]:
            items = crawler.crawl_list_page(
                url,
                crawler.site_config['selectors']['list_item']
            )
            for item in items[:2]:
                data = crawler.crawl_procedure_detail(item['url'])
                if data.get('success'):
                    self._save_procedure(data)
                    count += 1
                time.sleep(1)
        return count
    
    def _save_regulation(self, data: Dict[str, Any]):
        """保存法律法规到数据库"""
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
            data.get('regulation_type', ''),
            data.get('title', ''),
            data.get('document_no', ''),
            data.get('issue_dept', ''),
            data.get('issue_date'),
            data.get('content', ''),
            data.get('content_summary', ''),
            data.get('category', ''),
            data.get('keywords', ''),
            data.get('source_url', ''),
            data.get('status', 'ACTIVE')
        )
        
        self.db.execute(sql, params)
        logger.debug(f"保存法规: {data.get('title', '')}")
    
    def _save_case(self, data: Dict[str, Any]):
        """保存案例到数据库"""
        sql = """
        INSERT INTO typical_case 
        (case_no, case_title, case_type, case_category, court, 
         trial_court, judge_name, prosecutor_name, defendant,
         publish_date, case_cause, facts, trial_process, 
         judgment_result, legal_basis, case_highlight, 
         case_significance, source_url, source_dept, tags)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE 
        facts = VALUES(facts),
        update_time = NOW()
        """
        
        params = (
            data.get('case_no', ''),
            data.get('case_title', ''),
            data.get('case_type', ''),
            data.get('case_category', ''),
            data.get('court', ''),
            data.get('trial_court', ''),
            data.get('judge_name', ''),
            data.get('prosecutor_name', ''),
            data.get('defendant', ''),
            data.get('publish_date'),
            data.get('case_cause', ''),
            data.get('facts', ''),
            data.get('trial_process', ''),
            data.get('judgment_result', ''),
            data.get('legal_basis', ''),
            data.get('case_highlight', ''),
            data.get('case_significance', ''),
            data.get('source_url', ''),
            data.get('source_dept', ''),
            data.get('tags', '')
        )
        
        self.db.execute(sql, params)
        logger.debug(f"保存案例: {data.get('case_title', '')}")
    
    def _save_procedure(self, data: Dict[str, Any]):
        """保存流程规范到数据库"""
        sql = """
        INSERT INTO case_procedure_spec 
        (procedure_code, procedure_name, category, stage, order_index,
         description, legal_basis, required_documents, time_limit_days,
         time_limit_basis, responsible_role, key_points, 
         common_problems, quality_requirements, source_url)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE 
        description = VALUES(description),
        update_time = NOW()
        """
        
        params = (
            data.get('procedure_code', ''),
            data.get('procedure_name', ''),
            data.get('category', ''),
            data.get('stage', ''),
            data.get('order_index', 0),
            data.get('description', ''),
            data.get('legal_basis', ''),
            data.get('required_documents', ''),
            data.get('time_limit_days', 0),
            data.get('time_limit_basis', ''),
            data.get('responsible_role', ''),
            data.get('key_points', ''),
            data.get('common_problems', ''),
            data.get('quality_requirements', ''),
            data.get('source_url', '')
        )
        
        self.db.execute(sql, params)
        logger.debug(f"保存流程规范: {data.get('procedure_name', '')}")
    
    def _log_task(self, task_type: str, results: Dict[str, Any]):
        """记录任务日志"""
        sql = """
        INSERT INTO crawl_task_log 
        (task_name, task_type, status, start_time, end_time,
         total_count, success_count, fail_count, crawler_version)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        
        now = datetime.now()
        params = (
            f"{task_type}_{now.strftime('%Y%m%d_%H%M%S')}",
            task_type,
            'SUCCESS' if results['failed_tasks'] == 0 else 'PARTIAL',
            now - timedelta(seconds=300),  # 估算开始时间
            now,
            results['total_tasks'],
            results['success_tasks'],
            results['failed_tasks'],
            '1.0.0'
        )
        
        self.db.execute(sql, params)
    
    def start_scheduled_tasks(self):
        """启动定时任务"""
        logger.info("启动定时爬虫任务调度器...")
        
        # 配置定时任务
        # 每天凌晨2点执行法律法规爬取
        schedule.every().day.at("02:00").do(
            self.execute_single_task, 'regulation', None
        )
        
        # 每周日凌晨执行典型案例爬取
        schedule.every().sunday.at("03:00").do(
            self.execute_single_task, 'case', None
        )
        
        # 每月初执行流程规范爬取
        schedule.every().month.do(
            self.execute_single_task, 'procedure', None
        )
        
        self.running = True
        self.scheduler_thread = threading.Thread(
            target=self._run_scheduler,
            daemon=True
        )
        self.scheduler_thread.start()
        logger.info("定时任务调度器已启动")
    
    def _run_scheduler(self):
        """运行调度器"""
        while self.running:
            schedule.run_pending()
            time.sleep(60)  # 每分钟检查
    
    def stop(self):
        """停止调度器"""
        self.running = False
        if self.scheduler_thread:
            self.scheduler_thread.join()
        logger.info("定时任务调度器已停止")


# 全局调度器实例
scheduler = CrawlerScheduler()

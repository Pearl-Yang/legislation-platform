"""
主程序 - 爬虫系统入口
"""
import sys
import argparse
from pathlib import Path
from loguru import logger
from core.database import db
from core.scheduler import scheduler
from spiders.spp_spiders import SppLawCrawler, SppCaseCrawler, SppProcedureCrawler
from spiders.legislation_spider import LegislationCrawler
from report.generator import report_generator
from config.settings import config

# 添加项目根目录到路径
PROJECT_ROOT = Path(__file__).parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

# 配置日志
logger.remove()
logger.add(
    PROJECT_ROOT / "logs" / "crawler_{time}.log",
    rotation="500 MB",
    encoding="utf-8",
    level="INFO",
    format="{time:YYYY-MM-DD HH:mm:ss} | {level} | {message}"
)
logger.add(sys.stdout, level="INFO", format="<green>{time:HH:mm:ss}</green> | <level>{level: <8}</level> | <level>{message}</level>")


def test_database_connection():
    """测试数据库连接"""
    logger.info("测试数据库连接...")
    if db.test_connection():
        logger.success("✅ 数据库连接正常")
        return True
    else:
        logger.error("❌ 数据库连接失败，请检查配置")
        return False


def init_database():
    """初始化数据库表"""
    logger.info("初始化数据库表结构...")
    
    sql_file = PROJECT_ROOT / "government_edition" / "backend" / "sql" / "crawl_data_schema.sql"
    
    if not sql_file.exists():
        logger.error(f"SQL文件不存在: {sql_file}")
        return False
    
    try:
        with open(sql_file, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        
        # 分割SQL语句
        statements = [s.strip() for s in sql_content.split(';') if s.strip()]
        
        executed = 0
        for stmt in statements:
            try:
                if stmt and not stmt.startswith('--'):
                    db.execute(stmt)
                    executed += 1
            except Exception as e:
                logger.warning(f"SQL执行警告: {str(e)[:100]}")
        
        logger.success(f"✅ 数据库初始化完成，执行了 {executed} 条SQL")
        return True
        
    except Exception as e:
        logger.error(f"数据库初始化失败: {e}")
        return False


def run_full_crawl():
    """执行完整爬取任务"""
    logger.info("=" * 60)
    logger.info("开始执行完整数据爬取任务")
    logger.info("=" * 60)
    
    results = scheduler.execute_all_tasks()
    
    logger.info("=" * 60)
    logger.info("爬取任务完成")
    logger.info(f"总计: {results['total_tasks']}")
    logger.info(f"成功: {results['success_tasks']}")
    logger.info(f"失败: {results['failed_tasks']}")
    logger.info("=" * 60)
    
    # 输出报告文件路径
    logger.info("\n报告文件已生成到 output/ 目录:")
    details = results.get('details', {})
    if isinstance(details, dict):
        report_gen = details.get('report_generation', {})
        if isinstance(report_gen, dict):
            files = report_gen.get('files', {})
            for task_type, filepath in files.items():
                logger.info(f"  - {task_type}: {filepath}")
        else:
            logger.info(f"  报告详情: {report_gen}")
    else:
        logger.info(f"  爬取详情: {details}")
    
    return results


def run_single_crawl(site_type: str, url: str = None):
    """执行单个爬取任务"""
    logger.info(f"执行单个爬取任务: {site_type}")
    
    if site_type == 'law':
        crawler = SppLawCrawler()
        urls = [url] if url else config.TARGET_SITES['law']['list_urls']
        count = scheduler._crawl_regulations(crawler, urls)
        logger.info(f"完成: 爬取 {count} 条法律法规")
        
    elif site_type == 'case':
        crawler = SppCaseCrawler()
        urls = [url] if url else config.TARGET_SITES['case']['list_urls']
        count = scheduler._crawl_cases(crawler, urls)
        logger.info(f"完成: 爬取 {count} 个典型案例")
        
    elif site_type == 'procedure':
        crawler = SppProcedureCrawler()
        urls = [url] if url else config.TARGET_SITES['procedure']['list_urls']
        count = scheduler._crawl_procedures(crawler, urls)
        logger.info(f"完成: 爬取 {count} 个流程规范")
    
    else:
        logger.error(f"不支持的站点类型: {site_type}")
        return False
    
    return True


def generate_reports():
    """仅生成报告"""
    logger.info("生成需求调研报告...")
    
    try:
        files = report_generator.generate_full_report()
        
        logger.info("报告生成完成:")
        for name, path in files.items():
            logger.info(f"  - {name}: {path}")
        
        # 生成Markdown报告
        md_path = report_generator.generate_markdown_report()
        logger.info(f"  - Markdown报告: {md_path}")
        
        return True
    except Exception as e:
        logger.error(f"报告生成失败: {e}")
        return False


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="检察院官网数据爬取系统")
    parser.add_argument('--init-db', action='store_true', help='初始化数据库表')
    parser.add_argument('--crawl', action='store_true', help='执行完整爬取')
    parser.add_argument('--crawl-single', metavar='TYPE', help='爬取单个类型: law/case/procedure/legislation')
    parser.add_argument('--crawl-legislation', action='store_true', help='一次性爬取所有行政立法资料')
    parser.add_argument('--url', metavar='URL', help='指定爬取URL（与--crawl-single配合使用）')
    parser.add_argument('--report', action='store_true', help='生成需求调研报告')
    parser.add_argument('--daemon', action='store_true', help='启动后台定时任务')
    parser.add_argument('--test-db', action='store_true', help='测试数据库连接')
    
    args = parser.parse_args()
    
    logger.info("=" * 60)
    logger.info("青朗法治平台 - 检察院数据爬取系统")
    logger.info("=" * 60)
    
    # 测试数据库连接
    if not test_database_connection():
        logger.error("请先启动MySQL服务并检查配置")
        sys.exit(1)
    
    # 初始化数据库
    if args.init_db:
        if not init_database():
            sys.exit(1)
    
    # 执行爬取
    if args.crawl:
        run_full_crawl()
    
    # 单个爬取
    if args.crawl_single:
        run_single_crawl(args.crawl_single, args.url)

    # 立法版新增：一次性抓取所有行政立法资料
    if args.crawl_legislation:
        logger.info('启动立法资料抓取 ...')
        crawler = LegislationCrawler()
        crawler.crawl_all_sources(db.connection)
    
    # 生成报告
    if args.report:
        generate_reports()
    
    # 启动后台任务
    if args.daemon:
        logger.info("启动后台定时任务调度器...")
        scheduler.start_scheduled_tasks()
        try:
            while True:
                time.sleep(3600)
        except KeyboardInterrupt:
            logger.info("收到退出信号，停止调度器...")
            scheduler.stop()
    
    # 如果没有任何参数，显示帮助
    if len(sys.argv) == 1:
        parser.print_help()
        logger.info("\n示例用法:")
        logger.info("  python main.py --init-db          # 初始化数据库")
        logger.info("  python main.py --crawl           # 执行完整爬取")
        logger.info("  python main.py --crawl-single law  # 只爬取法律法规")
        logger.info("  python main.py --report          # 生成报告")
        logger.info("  python main.py --daemon          # 启动后台定时任务")


if __name__ == "__main__":
    main()

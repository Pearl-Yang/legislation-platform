"""
数据库连接和操作模块
"""
import pymysql
import pandas as pd
from typing import Optional, Dict, List, Any
from contextlib import contextmanager
from datetime import datetime
import json
from loguru import logger
from config.settings import config


class DatabaseManager:
    """数据库管理类"""
    
    def __init__(self):
        self.connection_params = {
            'host': config.DB_HOST,
            'port': config.DB_PORT,
            'user': config.DB_USER,
            'password': config.DB_PASSWORD,
            'database': config.DB_NAME,
            'charset': config.DB_CHARSET,
            'cursorclass': pymysql.cursors.DictCursor
        }
    
    @contextmanager
    def get_connection(self):
        """获取数据库连接（上下文管理器）"""
        conn = None
        try:
            conn = pymysql.connect(**self.connection_params)
            yield conn
        except Exception as e:
            logger.error(f"数据库连接失败: {e}")
            raise
        finally:
            if conn:
                conn.close()
    
    @contextmanager
    def get_cursor(self):
        """获取游标"""
        with self.get_connection() as conn:
            cursor = conn.cursor()
            try:
                yield cursor
                conn.commit()
            except Exception as e:
                conn.rollback()
                logger.error(f"数据库操作失败: {e}")
                raise
            finally:
                cursor.close()
    
    def execute(self, sql: str, params: tuple = None) -> int:
        """执行SQL语句"""
        with self.get_cursor() as cursor:
            return cursor.execute(sql, params)
    
    def fetch_one(self, sql: str, params: tuple = None) -> Optional[Dict]:
        """查询单条记录"""
        with self.get_cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchone()
    
    def fetch_all(self, sql: str, params: tuple = None) -> List[Dict]:
        """查询所有记录"""
        with self.get_cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchall()
    
    def insert(self, table: str, data: Dict[str, Any]) -> int:
        """插入数据"""
        columns = ', '.join(data.keys())
        placeholders = ', '.join(['%s'] * len(data))
        sql = f"INSERT INTO {table} ({columns}) VALUES ({placeholders})"
        
        with self.get_cursor() as cursor:
            cursor.execute(sql, tuple(data.values()))
            return cursor.lastrowid
    
    def upsert(self, table: str, data: Dict[str, Any], 
               unique_keys: List[str]) -> int:
        """插入或更新数据"""
        columns = list(data.keys())
        placeholders = ', '.join(['%s'] * len(columns))
        updates = ', '.join([f"{col}=VALUES({col})" for col in columns 
                            if col not in unique_keys])
        
        sql = f"""
            INSERT INTO {table} ({', '.join(columns)})
            VALUES ({placeholders})
            ON DUPLICATE KEY UPDATE {updates}
        """
        
        with self.get_cursor() as cursor:
            cursor.execute(sql, tuple(data.values()))
            return cursor.rowcount
    
    def batch_insert(self, table: str, data_list: List[Dict[str, Any]]) -> int:
        """批量插入"""
        if not data_list:
            return 0
        
        columns = list(data_list[0].keys())
        placeholders = ', '.join(['%s'] * len(columns))
        sql = f"INSERT INTO {table} ({', '.join(columns)}) VALUES ({placeholders})"
        
        with self.get_connection() as conn:
            cursor = conn.cursor()
            try:
                affected = 0
                for data in data_list:
                    values = [data.get(col) for col in columns]
                    cursor.execute(sql, values)
                    affected += 1
                conn.commit()
                return affected
            except Exception as e:
                conn.rollback()
                logger.error(f"批量插入失败: {e}")
                raise
    
    def to_dataframe(self, sql: str, params: tuple = None) -> pd.DataFrame:
        """查询结果转DataFrame"""
        with self.get_cursor() as cursor:
            cursor.execute(sql, params)
            return pd.DataFrame(cursor.fetchall())
    
    def test_connection(self) -> bool:
        """测试连接"""
        try:
            result = self.fetch_one("SELECT 1")
            return result is not None
        except Exception as e:
            logger.error(f"连接测试失败: {e}")
            return False


# 全局数据库管理器
db = DatabaseManager()


class DataExporter:
    """数据导出器"""
    
    def __init__(self):
        self.output_dir = config.OUTPUT_DIR
    
    def export_to_excel(self, data: List[Dict], filename: str, 
                       sheet_name: str = "Sheet1") -> str:
        """导出为Excel文件"""
        if not data:
            logger.warning("没有数据可导出")
            return ""
        
        df = pd.DataFrame(data)
        filepath = self.output_dir / f"{filename}.xlsx"
        
        with pd.ExcelWriter(filepath, engine='openpyxl') as writer:
            df.to_excel(writer, sheet_name=sheet_name, index=False)
        
        logger.info(f"Excel文件已导出: {filepath}")
        return str(filepath)
    
    def export_to_csv(self, data: List[Dict], filename: str) -> str:
        """导出为CSV文件"""
        if not data:
            return ""
        
        df = pd.DataFrame(data)
        filepath = self.output_dir / f"{filename}.csv"
        df.to_csv(filepath, index=False, encoding='utf-8-sig')
        
        logger.info(f"CSV文件已导出: {filepath}")
        return str(filepath)
    
    def export_multi_sheets_excel(self, sheets: Dict[str, List[Dict]], 
                                 filename: str) -> str:
        """导出多Sheet的Excel文件"""
        filepath = self.output_dir / f"{filename}.xlsx"
        
        with pd.ExcelWriter(filepath, engine='xlsxwriter') as writer:
            for sheet_name, data in sheets.items():
                if data:
                    df = pd.DataFrame(data)
                    df.to_excel(writer, sheet_name=sheet_name[:31], index=False)
        
        logger.info(f"多Sheet Excel文件已导出: {filepath}")
        return str(filepath)


# 全局数据导出器
exporter = DataExporter()

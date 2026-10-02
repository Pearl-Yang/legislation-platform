from pathlib import Path
from core.database import db
import pymysql

PROJECT_ROOT = Path(__file__).parent.parent
sql_file = PROJECT_ROOT / "government_edition" / "backend" / "sql" / "crawl_data_schema.sql"

print("Reading SQL file:", sql_file)
with open(sql_file, 'r', encoding='utf-8') as f:
    sql_content = f.read()

# 使用pymysql直接执行整个SQL文件
from config.settings import config
conn = pymysql.connect(
    host=config.DB_HOST,
    port=config.DB_PORT,
    user=config.DB_USER,
    password=config.DB_PASSWORD,
    database=config.DB_NAME,
    charset='utf8mb4'
)

try:
    with conn.cursor() as cursor:
        # 按分号分割，但跳过注释行
        statements = []
        current_stmt = []
        for line in sql_content.split('\n'):
            line = line.strip()
            if line.startswith('--'):
                continue
            if line:
                current_stmt.append(line)
            if line.endswith(';'):
                stmt = ' '.join(current_stmt)
                if 'CREATE' in stmt.upper() or 'INSERT' in stmt.upper():
                    statements.append(stmt.rstrip(';'))
                current_stmt = []
        
        print(f"Found {len(statements)} statements to execute")
        
        for i, stmt in enumerate(statements, 1):
            try:
                cursor.execute(stmt)
                print(f"[{i}] OK")
            except Exception as e:
                print(f"[{i}] WARNING: {str(e)[:100]}")
        
        conn.commit()
        print("\nDatabase initialization complete!")
        
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()
finally:
    conn.close()

from pathlib import Path
from core.database import db
import traceback

PROJECT_ROOT = Path(__file__).parent.parent
sql_file = PROJECT_ROOT / "government_edition" / "backend" / "sql" / "crawl_data_schema.sql"

print("Reading SQL file:", sql_file)
with open(sql_file, 'r', encoding='utf-8') as f:
    sql_content = f.read()

# 分割SQL语句
statements = [s.strip() for s in sql_content.split(';') if s.strip()]
print(f"Total statements: {len(statements)}")

# 测试执行前几条
for i, stmt in enumerate(statements[:5]):
    print(f"\nStatement {i+1}:")
    print(stmt[:200])
    try:
        db.execute(stmt)
        print("  [OK] Success")
    except Exception as e:
        print(f"  [ERROR] {str(e)[:200]}")
        traceback.print_exc()

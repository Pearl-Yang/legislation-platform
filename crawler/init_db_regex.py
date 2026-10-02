from pathlib import Path
from core.database import db
import re

PROJECT_ROOT = Path(__file__).parent.parent
sql_file = PROJECT_ROOT / "government_edition" / "backend" / "sql" / "crawl_data_schema.sql"

print("Reading SQL file:", sql_file)
with open(sql_file, 'r', encoding='utf-8') as f:
    sql_content = f.read()

# 用正则表达式匹配CREATE TABLE语句
create_pattern = r'CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+`[^`]+`\s+\([^;]+\);'
create_statements = re.findall(create_pattern, sql_content, re.IGNORECASE | re.DOTALL)

print(f"Found {len(create_statements)} CREATE TABLE statements")

for i, stmt in enumerate(create_statements, 1):
    try:
        db.execute(stmt)
        print(f"[{i}] OK - {stmt[:80]}")
    except Exception as e:
        print(f"[{i}] ERROR: {str(e)[:100]}")

print("\nDatabase initialization finished!")

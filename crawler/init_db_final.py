from pathlib import Path
from core.database import db

PROJECT_ROOT = Path(__file__).parent.parent
sql_file = PROJECT_ROOT / "government_edition" / "backend" / "sql" / "crawl_data_schema.sql"

print("Reading SQL file:", sql_file)
with open(sql_file, 'r', encoding='utf-8') as f:
    sql_content = f.read()

# 直接使用分号分割，保留完整的CREATE语句
raw_statements = sql_content.split(';')
print(f"Raw statements: {len(raw_statements)}")

executed = 0
for i, stmt in enumerate(raw_statements):
    stmt = stmt.strip()
    # 跳过注释和空语句
    if stmt and not stmt.startswith('--') and 'CREATE' in stmt.upper():
        try:
            db.execute(stmt + ';')  # 添加分号
            executed += 1
            print(f"Statement {executed}: OK - {stmt[:80]}")
        except Exception as e:
            print(f"Statement {i+1}: Warning - {str(e)[:100]}")

print(f"\nDatabase initialization complete! Executed {executed} CREATE statements.")

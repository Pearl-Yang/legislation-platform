from pathlib import Path
from core.database import db

PROJECT_ROOT = Path(__file__).parent.parent
sql_file = PROJECT_ROOT / "government_edition" / "backend" / "sql" / "crawl_data_schema.sql"

print("Reading SQL file:", sql_file)
with open(sql_file, 'r', encoding='utf-8') as f:
    sql_content = f.read()

# 分割SQL语句
statements = [s.strip() for s in sql_content.split(';') if s.strip()]
print(f"Total statements: {len(statements)}")

# 执行所有语句
executed = 0
for i, stmt in enumerate(statements, 1):
    if stmt and not stmt.startswith('--'):
        try:
            db.execute(stmt)
            executed += 1
            print(f"Statement {i}: OK")
        except Exception as e:
            print(f"Statement {i}: Warning - {str(e)[:100]}")

print(f"\nDatabase initialized successfully! Executed {executed} statements.")

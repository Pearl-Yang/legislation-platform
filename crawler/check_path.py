from pathlib import Path
PROJECT_ROOT = Path(__file__).parent.parent
print("PROJECT_ROOT:", PROJECT_ROOT)
sql_file = PROJECT_ROOT / "government_edition" / "backend" / "sql" / "crawl_data_schema.sql"
print("SQL file path:", sql_file)
print("Exists:", sql_file.exists())

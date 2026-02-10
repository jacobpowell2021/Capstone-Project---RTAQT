import os
import time
import pyodbc

# Read the same env vars used by predictionCodeHTTPTrigger.py
server = os.getenv("DB_SERVER", "capstone-database.database.windows.net")
database = os.getenv("DB_NAME", "capstoneSampleDatabase")
username = os.getenv("DB_USER", "sqlServerAdmin")
password = os.getenv("DB_PASS", "1wLbroSwEgIxUtr")
driver = os.getenv("DB_DRIVER", "ODBC Driver 18 for SQL Server")

conn_str = (
    f"DRIVER={driver};SERVER=tcp:{server},1433;DATABASE={database};UID={username};PWD={password};"
    f"Encrypt=yes;TrustServerCertificate=no;Connection Timeout=15;"
)

TRIALS = 5

print("Using connection string (masked):")
print(conn_str.replace(password, '***'))

results = []
for i in range(TRIALS):
    start = time.perf_counter()
    try:
        cn = pyodbc.connect(conn_str, timeout=15)
        connect_time = time.perf_counter() - start
        # quick query
        qstart = time.perf_counter()
        cur = cn.cursor()
        cur.execute("SELECT 1")
        _ = cur.fetchone()
        query_time = time.perf_counter() - qstart
        cn.close()
        print(f"Trial {i+1}: connect {connect_time:.3f}s, query {query_time:.3f}s")
        results.append((connect_time, query_time, None))
    except Exception as e:
        elapsed = time.perf_counter() - start
        print(f"Trial {i+1}: FAILED after {elapsed:.3f}s: {e}")
        results.append((None, None, str(e)))

# Summary
connects = [r[0] for r in results if r[0] is not None]
queries = [r[1] for r in results if r[1] is not None]

print("\nSummary:")
if connects:
    print(f"Connect min/median/max: {min(connects):.3f}/{sorted(connects)[len(connects)//2]:.3f}/{max(connects):.3f} s")
else:
    print("No successful connects")

if queries:
    print(f"Query min/median/max: {min(queries):.3f}/{sorted(queries)[len(queries)//2]:.3f}/{max(queries):.3f} s")
else:
    print("No successful queries")

failed = [r[2] for r in results if r[2] is not None]
if failed:
    print("Failures:")
    for f in failed:
        print(" -", f)

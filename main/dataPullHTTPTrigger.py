"""Predict future sensor readings and return them as JSON via HTTP trigger.

This file was modernized to improve readability and error handling. It reads
database connection parameters from environment variables with sensible
fallbacks. The main logic is wrapped in a `http_main()` function and the DB
connection is cleaned up in a finally block.
"""

import os
import logging
import sys
from typing import List, Tuple, Optional, Dict, Any

import pyodbc
import json


# Azure Functions HTTP types are optional; avoid hard dependency so file can be run locally
try:
    import azure.functions as func
except Exception:
    func = None

# Configure logging to output to stdout for visibility in various environments (e.g., Azure Functions)
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


# Database connection parameters from environment variables with fallbacks to defaults
def get_db_config() -> Tuple[str, str, str, str, str]:
    """Return database connection parameters.

    Values are read from environment variables with the original values as
    fallbacks to preserve prior behaviour.
    """
    server = os.getenv("DB_SERVER", "capstone-database.database.windows.net")  # e.g. "myserver.database.windows.net"
    database = os.getenv("DB_NAME", "capstoneSampleDatabase")  # e.g. "myDataBase"
    username = os.getenv("DB_USER", "sqlServerAdmin")  # e.g. "mylogin"
    password = os.getenv("DB_PASS", "1wLbroSwEgIxUtr")  # e.g. "mypassword"
    driver = os.getenv("DB_DRIVER", "ODBC Driver 18 for SQL Server")  # e.g. "ODBC Driver 18 for SQL Server"
    return server, database, username, password, driver


def _connect() -> Tuple[Optional[pyodbc.Connection], Optional[pyodbc.Cursor], Optional[Exception]]:
    """Open a DB connection and cursor; return (conn, cursor, error)."""
    server, database, username, password, driver = get_db_config()
    conn_str = (
        f"DRIVER={driver};SERVER=tcp:{server},1433;DATABASE={database};UID={username};PWD={password};"
        f"Encrypt=yes;TrustServerCertificate=no"
    )
    try:
        conn = pyodbc.connect(conn_str, timeout=10)
        cursor = conn.cursor()
        return conn, cursor, None
    except Exception as ex:
        logger.exception("DB connection failed: %s", ex)
        return None, None, ex


def fetch_all_rows_as_dicts(cursor: pyodbc.Cursor, table_name: str) -> List[Dict[str, Any]]:
    """Fetch all rows from `table_name` and return as list of dicts.

    NOTE: SQL tables have no implicit ordering. We fetch all rows and slice the last N
    in Python to avoid depending on a timestamp column. This assumes the DB returns
    rows in insertion order (common but not guaranteed). If strict ordering is required,
    modify the query to ORDER BY an appropriate timestamp/ID column.
    """
    # For the sensor table we explicitly select only the columns we care about
    # to avoid unsupported SQL types (for example datetimeoffset) being
    # returned by the driver. The sensor table columns of interest are:
    # Temperature, Humidity, FlammableGases, TVOC, CO, BatteryLife
    if table_name.lower() == "iotdatatablefromsensor":
        query = (
            "SELECT Temperature, Humidity, FlammableGases, TVOC, CO, BatteryLife "
            "FROM iotdatatablefromsensor;"
        )
    else:
        query = f"SELECT * FROM {table_name};"

    cursor.execute(query)
    cols = [col[0] for col in cursor.description]
    rows = cursor.fetchall()
    result = [dict(zip(cols, row)) for row in rows]
    return result


def get_last_rows() -> Tuple[int, Dict[str, Any]]:
    """Connect to DB, fetch last 48 and last 96 rows, and return a response dict.

    Returns (status_code, body_dict).
    """
    conn, cursor, err = _connect()
    if err is not None:
        return 500, {"error": "failed to connect to database", "details": str(err)}

    try:
        if cursor is None:
            return 500, {"error": "database cursor not available"}
        sensor_rows = fetch_all_rows_as_dicts(cursor, "iotdatatablefromsensor")
        predictive_rows = fetch_all_rows_as_dicts(cursor, "iotdatatablepredictive")

        # Last 48 rows for historical (last 12 hours at 15-min intervals)
        recent = sensor_rows[-48:] if len(sensor_rows) >= 48 else sensor_rows
        # Each row may contain extra columns; create a trimmed view containing
        # only the sensor fields + BatteryLife, and explicitly exclude enqueuedTime.
        trimmed_historical = []
        for r in recent:
            # r is a dict mapping column name -> value
            trimmed = {
                "Temperature": r.get("Temperature"),
                "Humidity": r.get("Humidity"),
                "FlammableGases": r.get("FlammableGases"),
                "TVOC": r.get("TVOC"),
                "CO": r.get("CO"),
                # include BatteryLife per your request; if missing, result will be None
                "BatteryLife": r.get("BatteryLife"),
            }
            trimmed_historical.append(trimmed)
        # Last 96 rows for today's forecast
        last_96 = predictive_rows[-96:] if len(predictive_rows) >= 96 else predictive_rows

        body = {
            "historical_last_48": trimmed_historical,
            "predictive_last_96": last_96
        }
        return 200, body

    except Exception as ex:
        logger.exception("Failed to fetch/serialize rows: %s", ex)
        return 500, {"error": "failed to fetch or process rows", "details": str(ex)}

    finally:
        try:
            if cursor is not None:
                cursor.close()
        except Exception:
            logger.debug("Error closing cursor", exc_info=True)
        try:
            if conn is not None:
                conn.close()
        except Exception:
            logger.debug("Error closing connection", exc_info=True)


def http_main(req) -> Tuple[int, Dict[str, Any]]:
    """HTTP-friendly entry point. Ignores request body; always returns last rows.

    If `req` is an `azure.functions.HttpRequest`, it's accepted directly. For local
    testing, pass a dict or call `get_last_rows()` directly.
    """
    return get_last_rows()


if __name__ == "__main__":
    # CLI debug runner: call get_last_rows and pretty-print JSON
    status, resp = get_last_rows()
    print("Status:", status)
    print(json.dumps(resp, indent=2, default=str))
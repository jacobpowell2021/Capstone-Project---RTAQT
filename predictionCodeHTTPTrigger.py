"""Predict future sensor readings and insert them into the predictive table.

This file was modernized to improve readability and error handling. It reads
database connection parameters from environment variables with sensible
fallbacks. The main logic is wrapped in a `main()` function and the DB
connection is cleaned up in a finally block.
"""

import os
import logging
import sys
from typing import List, Tuple

import pyodbc
import numpy as np
from statsmodels.tsa.arima.model import ARIMA

# Configure logging to output to stdout for visibility in various environments (e.g., Azure Functions)
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


# Database connection parameters from environment variables with fallbacks to defaults
def get_db_config() -> Tuple[str, str, str, str, str]:
    """Return database connection parameters.

    Values are read from environment variables with the original values as
    fallbacks to preserve prior behaviour.
    """
    server = os.getenv("DB_SERVER", "capstone-database.database.windows.net") # e.g. "myserver.database.windows.net"
    database = os.getenv("DB_NAME", "capstoneSampleDatabase")                 # e.g. "myDataBase"
    username = os.getenv("DB_USER", "sqlServerAdmin")                         # e.g. "mylogin"
    password = os.getenv("DB_PASS", "1wLbroSwEgIxUtr")                        # e.g. "mypassword"
    driver = os.getenv("DB_DRIVER", "ODBC Driver 18 for SQL Server")          # e.g. "ODBC Driver 18 for SQL Server"
    return server, database, username, password, driver

# Fetches sensor data from the SQL database from the specified table, i.e. iotdatatablefromsensor
def fetch_sensor_data(cursor) -> List[Tuple[float, float, float, float, float]]:
    """Fetch sensor rows from `iotdatatablefromsensor`.

    Returns a list of 5-tuples: (Temperature, Humidity, FlammableGases, TVOC, CO)
    """
    cursor.execute(
        "SELECT Temperature, Humidity, FlammableGases, TVOC, CO FROM iotdatatablefromsensor;"
    )
    rows = cursor.fetchall()
    return rows

# Converts fetched rows into numpy arrays for modeling
def series_from_rows(rows) -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """Convert fetched rows into numpy arrays for modeling."""
    if not rows:
        return (np.array([]), np.array([]), np.array([]), np.array([]), np.array([]))

    temps, hums, flams, tvocs, cos = zip(*rows)
    return (np.array(temps, dtype=float), np.array(hums, dtype=float), np.array(flams, dtype=float),
            np.array(tvocs, dtype=float), np.array(cos, dtype=float))

# Fits an ARIMA(1,1,1) model and forecasts future values
def fit_forecast(series: np.ndarray, steps: int) -> np.ndarray:
    """Fit an ARIMA(1,1,1) model and forecast `steps` ahead.

    Returns a 1-D numpy array of forecasts. Raises a ValueError if the
    series is too short for modeling.
    """
    if series.size < 3:
        raise ValueError("series too short for ARIMA modeling")

    model = ARIMA(series, order=(1, 1, 1))
    fitted = model.fit()
    forecast = fitted.forecast(steps=steps)
    return np.asarray(forecast, dtype=float)

# Main execution logic: Connects to DB, fetches data, models, and inserts forecasts into the predictive table
def main():
    server, database, username, password, driver = get_db_config()

    conn_str = (
        f"DRIVER={driver};SERVER=tcp:{server},1433;DATABASE={database};UID={username};PWD={password};"
        f"Encrypt=yes;TrustServerCertificate=no"
    )

    conn = None
    cursor = None
    try:
        logger.info("Connecting to database %s on %s", database, server)
        conn = pyodbc.connect(conn_str, timeout=10)
        cursor = conn.cursor()

        rows = fetch_sensor_data(cursor)
        if not rows:
            logger.warning("No sensor data returned from iotdatatablefromsensor. Exiting.")
            return

        (temperature_data, humidity_data, flammable_data, tvoc_data, co_data) = series_from_rows(rows)

        forecast_steps = 10

        forecasts = {}
        series_map = {
            "temperature": temperature_data,
            "humidity": humidity_data,
            "flammable": flammable_data,
            "tvoc": tvoc_data,
            "co": co_data,
        }
        # Fit and forecast each series
        for name, series in series_map.items():
            try:
                logger.info("Fitting ARIMA for %s (n=%d)", name, series.size)
                forecasts[name] = fit_forecast(series, forecast_steps)
            except Exception as ex:
                logger.exception("Failed to model %s: %s", name, ex)
                # Fill with NaNs so downstream code remains consistent
                forecasts[name] = np.full((forecast_steps,), np.nan)

        # Prepare rows for insert
        insert_query = (
            "INSERT INTO iotdatatablepredictive (Temperature, Humidity, FlammableGases, TVOC, CO)"
            " VALUES (?, ?, ?, ?, ?);"
        )

        params = []
        # Create a list of tuples for each forecasted step
        for i in range(forecast_steps):
            params.append((
                float(forecasts["temperature"][i]) if not np.isnan(forecasts["temperature"][i]) else None,
                float(forecasts["humidity"][i]) if not np.isnan(forecasts["humidity"][i]) else None,
                float(forecasts["flammable"][i]) if not np.isnan(forecasts["flammable"][i]) else None,
                float(forecasts["tvoc"][i]) if not np.isnan(forecasts["tvoc"][i]) else None,
                float(forecasts["co"][i]) if not np.isnan(forecasts["co"][i]) else None,
            ))
        
        # Bulk insert forecasted rows
        logger.info("Inserting %d predictive rows into iotdatatablepredictive", len(params))
        cursor.executemany(insert_query, params)
        conn.commit()
        logger.info("Insert complete and committed.")

    except Exception:
        logger.exception("Unexpected error occurred")
        raise
    finally:
        if cursor is not None:
            try:
                cursor.close()
            except Exception:
                logger.debug("Error closing cursor", exc_info=True)
        if conn is not None:
            try:
                conn.close()
            except Exception:
                logger.debug("Error closing connection", exc_info=True)

# Entry point for the script
if __name__ == "__main__":
    main()
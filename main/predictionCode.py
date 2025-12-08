"""Predict future sensor readings and insert them into the predictive table.

This file was modernized to improve readability and error handling. It reads
database connection parameters from environment variables with sensible
fallbacks. The main logic is wrapped in a `main()` function and the DB
connection is cleaned up in a finally block.
"""

import os
import logging
import sys
from typing import List, Tuple, Any, cast

import pyodbc
import numpy as np
from concurrent.futures import ThreadPoolExecutor, as_completed
# Import ARIMA locally inside fit_forecast to avoid static analysis issues
# caused by inspecting the statsmodels package at module-import time.
import time
import threading

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
    # Order by the event enqueue time so the series is in chronological order.
    # We do not select the timestamp column itself to avoid returning unsupported
    # SQL types to the driver; ORDER BY is sufficient to ensure correct order.
    # Table schema includes `enqueuedTime`; order by it to ensure chronological series.
    cursor.execute(
        "SELECT Temperature, Humidity, FlammableGases, TVOC, CO "
        "FROM iotdatatablefromsensor "
        "ORDER BY enqueuedTime ASC;"
    )
    rows = cursor.fetchall()
    # Defensive: in case the table now contains extra columns, only keep the first
    # five sensor columns (Temperature, Humidity, FlammableGases, TVOC, CO).
    # Each row returned by pyodbc is indexable.
    trimmed = [ (r[0], r[1], r[2], r[3], r[4]) for r in rows ]
    return trimmed

# Converts fetched rows into numpy arrays for modeling
def series_from_rows(rows) -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """Convert fetched rows into numpy arrays for modeling."""
    if not rows:
        return (np.array([]), np.array([]), np.array([]), np.array([]), np.array([]))

    temps, hums, flams, tvocs, cos = zip(*rows)
    return (np.array(temps, dtype=float), np.array(hums, dtype=float), np.array(flams, dtype=float),
            np.array(tvocs, dtype=float), np.array(cos, dtype=float))

# Fits a forecast model and returns forecasts
def fit_forecast(series: np.ndarray, steps: int) -> np.ndarray:
    """Compatibility wrapper: simple fast fallback used by misc callers.

    The heavy SARIMAX fitting is now handled by the batch fitter which
    persists fitted model objects to disk. The HTTP path will load those
    objects and perform a fast state update + forecast without re-fitting.
    """
    if series.size <= 0:
        return np.zeros((steps,))
    last = float(series[-1])
    return np.full((steps,), last)


def _fit_and_save_ses(series: np.ndarray, name: str, models_dir: str = "models") -> None:
    """Fit an SES (Exponential Smoothing) model for `series` and save the fitted results object.

    The fitted results object is pickled to `models/<name>_ses.pkl`.
    """
    import pickle
    from statsmodels.tsa.holtwinters import ExponentialSmoothing

    if series.size < 10:
        logger.warning("Not enough data to fit SES for %s (n=%d)", name, series.size)
        return

    os.makedirs(models_dir, exist_ok=True)
    
    start = time.time()
    logger.info("Fitting SES model for %s", name)
    try:
        ses_model = ExponentialSmoothing(series, seasonal_periods=96, seasonal="add", trend=None, initialization_method="estimated")
        ses_fitted = ses_model.fit(optimized=True)
        elapsed = time.time() - start
        logger.info("SES fitted for %s in %.2fs; saving to disk", name, elapsed)
        
        path = os.path.join(models_dir, f"{name}_ses.pkl")
        with open(path, "wb") as fh:
            pickle.dump(ses_fitted, fh)
        logger.info("Saved SES model for %s to %s", name, path)
    except Exception:
        logger.exception("Failed to fit/save SES model for %s", name)

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

        # Diagnostic logging: show how many rows we fetched and a small sample.
        try:
            logger.info("Fetched %d sensor rows from iotdatatablefromsensor", len(rows))
            if len(rows) > 0:
                logger.debug("Sample sensor row (first): %s", rows[0])
        except Exception:
            logger.debug("Unable to log sample rows", exc_info=True)

        (temperature_data, humidity_data, flammable_data, tvoc_data, co_data) = series_from_rows(rows)

        # Compute 1-day forecast (96 steps: 96 steps/day * 1 day)
        # Run batch frequently (e.g., every 15 min) to keep forecast fresh; replaces old cache rows with new
        forecast_steps = 96  # 1-day ahead forecast
        logger.info("Computing %d-step forecast (%.1f days)", forecast_steps, forecast_steps / 96.0)

        series_map = {
            "temperature": temperature_data,
            "humidity": humidity_data,
            "flammable": flammable_data,
            "tvoc": tvoc_data,
            "co": co_data,
        }

        # Generate 96-step forecasts (next day) using SES
        logger.info("Generating 96-step forecasts using SES")
        forecasts = {}
        
        from statsmodels.tsa.holtwinters import ExponentialSmoothing
        for name, series in series_map.items():
            try:
                if series.size < 10:
                    logger.warning("Not enough data for %s; using last value", name)
                    forecasts[name] = [float(series[-1])] * forecast_steps if series.size > 0 else [0.0] * forecast_steps
                else:
                    model = ExponentialSmoothing(series, seasonal_periods=96, seasonal="add", trend=None, initialization_method="estimated")
                    fitted = model.fit(optimized=True)
                    preds = fitted.forecast(forecast_steps)
                    forecasts[name] = [float(x) for x in preds]
                    logger.info("Generated %d-step forecast for %s", len(forecasts[name]), name)
            except Exception:
                logger.exception("Failed to forecast %s; using last value", name)
                forecasts[name] = [float(series[-1])] * forecast_steps if series.size > 0 else [0.0] * forecast_steps

        # Clear old data and insert new forecasts into iotdatatablepredictive
        logger.info("Clearing old forecasts and inserting %d new rows into iotdatatablepredictive", forecast_steps)
        try:
            cursor.execute("DELETE FROM iotdatatablepredictive")
            conn.commit()
            logger.info("Cleared old forecast data")
            
            # Insert 96 rows (one per time step)
            for i in range(forecast_steps):
                temp = forecasts["temperature"][i]
                hum = forecasts["humidity"][i]
                flam = forecasts["flammable"][i]
                tvoc = forecasts["tvoc"][i]
                co = forecasts["co"][i]
                
                cursor.execute(
                    "INSERT INTO iotdatatablepredictive (Temperature, Humidity, FlammableGases, TVOC, CO) "
                    "VALUES (?, ?, ?, ?, ?)",
                    temp, hum, flam, tvoc, co
                )
            
            conn.commit()
            logger.info("Successfully inserted %d forecast rows into iotdatatablepredictive", forecast_steps)
        except Exception:
            logger.exception("Failed to write forecasts to database")
            conn.rollback()
            raise

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
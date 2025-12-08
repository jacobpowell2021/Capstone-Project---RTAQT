"""HTTP endpoint to return SARIMAX forecasts using pre-fitted models.

This endpoint loads pre-fitted SARIMAX results objects saved by the
batch job (`predictionCode.py`) into `models/<series>_sarimax.pkl`,
applies any new sensor observations to the fitted state (without
re-fitting parameters), and returns a forecast for the requested
number of days. If a fitted model is not available or updating fails,
the endpoint falls back to a cheap last-value predictor for that
series to guarantee a timely response.
"""

import os
import logging
import sys
from typing import List, Tuple, Optional, Any
import json
import math
import numpy as np
import threading
import pyodbc

# Azure Functions HTTP types are optional; avoid hard dependency
try:
    import azure.functions as func
except Exception:
    func = None

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


def get_db_config() -> Tuple[str, str, str, str, str]:
    """Return database connection parameters from environment variables."""
    server = os.getenv("DB_SERVER", "capstone-database.database.windows.net")
    database = os.getenv("DB_NAME", "capstoneSampleDatabase")
    username = os.getenv("DB_USER", "sqlServerAdmin")
    password = os.getenv("DB_PASS", "1wLbroSwEgIxUtr")
    driver = os.getenv("DB_DRIVER", "ODBC Driver 18 for SQL Server")
    return server, database, username, password, driver


def _make_conn_str() -> str:
    """Build pyodbc connection string."""
    server, database, username, password, driver = get_db_config()
    return (
        f"DRIVER={driver};SERVER=tcp:{server},1433;DATABASE={database};"
        f"UID={username};PWD={password};Encrypt=yes;TrustServerCertificate=no;Connection Timeout=15;"
    )


# Thread-local connection reuse
_thread_local = threading.local()

def _get_thread_conn():
    """Return a per-thread pyodbc connection. Creates one if missing or broken."""
    conn = getattr(_thread_local, "conn", None)
    if conn is not None:
        try:
            cur = conn.cursor()
            cur.execute("SELECT 1")
            cur.fetchone()
            cur.close()
            return conn
        except Exception:
            try:
                conn.close()
            except Exception:
                pass
            conn = None
    
    # Create a new connection
    try:
        conn = pyodbc.connect(_make_conn_str(), timeout=15)
        _thread_local.conn = conn
        return conn
    except Exception as e:
        logger.error("Failed to create database connection: %s", e)
        raise


# Warm main thread connection at module import
try:
    _get_thread_conn()
    logger.info("Warmed main thread DB connection at import")
except Exception:
    logger.warning("Failed to warm main thread connection at import", exc_info=True)


def get_latest_forecast_time(cursor) -> Optional[str]:
    """Return the most recent ForecastComputedAt timestamp from the cache."""
    try:
        cursor.execute("SELECT MAX(ForecastComputedAt) FROM iotdatatable_forecast_cache;")
        row = cursor.fetchone()
        if row and row[0] is not None:
            return row[0]
    except Exception as e:
        logger.warning("Failed to get latest forecast time: %s", e)
    return None


def fetch_sensor_rows(cursor) -> list:
    """Fetch sensor rows from `iotdatatablefromsensor` in chronological order.

    Returns a list of rows where each row is indexable and starts with
    Temperature, Humidity, FlammableGases, TVOC, CO (matching the batch job).
    """
    cursor.execute(
        "SELECT Temperature, Humidity, FlammableGases, TVOC, CO "
        "FROM iotdatatablefromsensor "
        "ORDER BY enqueuedTime ASC;"
    )
    return cursor.fetchall()


def _load_fitted_model(name: str, models_dir: str = "models"):
    """Load a pickled SARIMAXResults object for `name` if present.

    Returns the unpickled fitted results object or None on failure.
    """
    import pickle
    path = os.path.join(models_dir, f"{name}_sarimax.pkl")
    if not os.path.exists(path):
        logger.info("No pre-fitted model found for %s at %s", name, path)
        return None
    try:
        with open(path, "rb") as fh:
            fitted = pickle.load(fh)
        logger.info("Loaded fitted model for %s from %s", name, path)
        return fitted
    except Exception:
        logger.exception("Failed to load fitted model for %s", name)
        return None


def _fit_sarimax_with_timeout(series, timeout: float = 10.0):
    """Attempt to fit a SARIMAX model within `timeout` seconds.

    Returns the fitted results on success, or None on timeout/error.
    """
    import warnings
    try:
        from statsmodels.tsa.statespace.sarimax import SARIMAX
        from statsmodels.tools.sm_exceptions import ConvergenceWarning
    except Exception:
        logger.exception("statsmodels not available for SARIMAX fit")
        return None

    if series.size < 10:
        logger.warning("Not enough data to fit SARIMAX (n=%d)", series.size)
        return None

    model = SARIMAX(series, order=(0, 1, 1), seasonal_order=(0, 1, 1, 96),
                    enforce_stationarity=False, enforce_invertibility=False)

    def _do_fit():
        with warnings.catch_warnings(record=True) as w:
            warnings.simplefilter("always")
            res = model.fit()
            for warn in w:
                if issubclass(getattr(warn, "category", type(None)), ConvergenceWarning):
                    logger.warning("SARIMAX convergence warning: %s", warn.message)
            return res

    from concurrent.futures import ThreadPoolExecutor
    try:
        with ThreadPoolExecutor(max_workers=1) as ex:
            fut = ex.submit(_do_fit)
            try:
                fitted = fut.result(timeout=timeout)
                return fitted
            except Exception:
                logger.warning("SARIMAX fit did not complete within %.1fs", timeout)
                return None
    except Exception:
        logger.exception("Error while running SARIMAX fit thread")
        return None


def _forecast_with_fitted(fitted, current_series, steps: int):
    """Produce a forecast using a pickled fitted SARIMAXResults object.

    The function will attempt to append new observations to the fitted
    results (without re-fitting) to update the state, then call
    `get_forecast(steps=...)`. If the fitted object doesn't support
    appending/updating, or any step fails, an Exception is raised.
    """
    # Determine original sample size
    orig_n = None
    try:
        # Many SARIMAXResults expose `nobs` or `data.endog`
        orig_n = int(getattr(fitted, "nobs", None) or len(getattr(fitted, "data").endog))
    except Exception:
        orig_n = None

    # If there are additional observations beyond the fitted sample,
    # append them to the results object (refit=False) which updates the
    # filtered state without re-estimating parameters. If no new
    # observations exist, we can use the fitted object directly.
    new_obs = None
    if orig_n is not None and current_series.size > orig_n:
        new_obs = current_series[orig_n:]

    # Use append if available
    try:
        if new_obs is not None and hasattr(fitted, "append"):
            updated = fitted.append(new_obs, refit=False)
            res = updated.get_forecast(steps=steps)
        else:
            res = fitted.get_forecast(steps=steps)

        # Extract predicted mean as a 1-d numpy array or list
        pm = getattr(res, "predicted_mean", None)
        if pm is None:
            # Some older versions return a numpy array directly from predict
            pm = res
        return list(map(float, pm))
    except Exception:
        logger.exception("Failed to forecast with fitted model")
        raise


def _forecast_with_ses(series, steps: int):
    """Fast exponential smoothing (Holt-Winters) forecast as a fallback.

    Uses additive seasonality with seasonal_periods=96 (daily seasonality
    for 15-minute intervals). This is much faster than SARIMAX and often
    preserves daily seasonal shape.
    """
    try:
        # Import locally to avoid heavy import at module load
        from statsmodels.tsa.holtwinters import ExponentialSmoothing
        if series.size <= 0:
            return [0.0] * steps

        # Use additive seasonal with no explicit trend to keep it simple
        model = ExponentialSmoothing(series, seasonal_periods=96, seasonal="add", trend=None, initialization_method="estimated")
        fitted = model.fit(optimized=True)
        preds = fitted.forecast(steps)
        return [float(x) for x in preds]
    except Exception:
        logger.exception("SES forecasting failed")
        raise


def main(days: float) -> Tuple[int, dict]:
    """HTTP endpoint to return cached forecasts.
    
    Args:
        days: Number of days to forecast (1, 2, 3, etc.)
    
    Returns:
        (status_code, response_json)
    """
    import time
    start_time = time.time()
    
    if days is None:
        return 400, {"error": "missing 'days' parameter"}
    
    if not isinstance(days, (int, float)) or days <= 0:
        return 400, {"error": "'days' must be a positive number"}
    
    days_int = int(days)
    if days_int != days:
        return 400, {"error": "'days' must be an integer"}
    
    # Determine number of steps (96 steps per day at 15-minute intervals)
    steps = days_int * 96

    # Connect and fetch sensor rows
    cursor = None
    conn = None
    try:
        conn = _get_thread_conn()
        cursor = conn.cursor()
        rows = fetch_sensor_rows(cursor)
    except Exception:
        logger.exception("Failed to fetch sensor rows")
        return 503, {"error": "failed to fetch sensor data"}
    finally:
        if cursor is not None:
            try:
                cursor.close()
            except Exception:
                pass

    if not rows:
        return 503, {"error": "no sensor data available"}

    # Convert rows to numpy arrays per series
    try:
        temps, hums, flams, tvocs, cos = zip(*rows)
    except Exception:
        logger.exception("Sensor rows have unexpected shape")
        return 500, {"error": "unexpected sensor row format"}

    series_map = {
        "temperature": np.array(temps, dtype=float),
        "humidity": np.array(hums, dtype=float),
        "flammable": np.array(flams, dtype=float),
        "tvoc": np.array(tvocs, dtype=float),
        "co": np.array(cos, dtype=float),
    }

    forecasts = {}
    model_info = {}

    for name, series in series_map.items():
        # Use SES forecasting directly
        logger.info("Using SES forecasting for %s", name)
        try:
            preds = _forecast_with_ses(series, steps)
            if len(preds) < steps:
                preds = preds + [float(series[-1])] * (steps - len(preds))
            forecasts[name] = preds[:steps]
            model_info[name] = {"ses_used": True}
        except Exception:
            logger.exception("SES forecasting failed for %s; using last-value", name)
            if series.size <= 0:
                forecasts[name] = [0.0] * steps
            else:
                forecasts[name] = [float(series[-1])] * steps
            model_info[name] = {"ses_used": False}

    elapsed = time.time() - start_time
    logger.info("HTTP request completed in %.3fs", elapsed)

    # Return only the last 96 rows of the generated forecast
    per_day = 96
    sliced_forecasts = {}
    for name, values in forecasts.items():
        # Get the last 96 steps from the forecast
        if not values:
            sliced = [0.0] * per_day
        elif len(values) < per_day:
            # Pad if needed
            pad = [values[-1]] * (per_day - len(values)) if values else [0.0] * per_day
            sliced = values + pad
        else:
            # Take last 96 steps
            sliced = values[-per_day:]
        sliced_forecasts[name] = sliced

    response = {
        "days_requested": days_int,
        "interval_minutes": 15,
        "returned_steps": per_day,
        "forecasts": sliced_forecasts,
    }

    return 200, response


# Entry point for local CLI testing
if __name__ == "__main__":
    days_arg = None
    if len(sys.argv) > 1:
        try:
            days_arg = float(sys.argv[1])
        except Exception:
            days_arg = None
    
    if days_arg is None:
        days_arg = 1.0
    
    status, resp = main(days_arg)
    print(f"Status: {status}\n")
    print(json.dumps(resp, indent=2))

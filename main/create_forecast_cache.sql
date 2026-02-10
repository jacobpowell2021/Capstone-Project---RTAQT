-- Create forecast cache table to store pre-computed SARIMAX forecasts
-- This allows HTTP endpoint to return multi-day forecasts instantly without live modeling

CREATE TABLE IF NOT EXISTS iotdatatable_forecast_cache (
    CacheID INT PRIMARY KEY IDENTITY(1,1),
    ForecastComputedAt DATETIME DEFAULT GETUTCDATE(),
    DaysAhead INT NOT NULL,  -- Number of days forecast for (1, 2, 3, ..., 7)
    StepIndex INT NOT NULL,  -- 0-based index within the forecast (0-95 for day 1, 96-191 for day 2, etc.)
    Temperature FLOAT,
    Humidity FLOAT,
    FlammableGases FLOAT,
    TVOC FLOAT,
    CO FLOAT,
    UNIQUE (ForecastComputedAt, DaysAhead, StepIndex)
);

-- Index for fast lookups by date and days_ahead
CREATE INDEX IF NOT EXISTS idx_forecast_cache_date_days 
ON iotdatatable_forecast_cache(ForecastComputedAt DESC, DaysAhead);

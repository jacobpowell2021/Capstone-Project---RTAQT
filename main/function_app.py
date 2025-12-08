import logging
import azure.functions as func
import datetime
import json
from predictionCode import main as prediction_main
from predictionCodeHTTPTrigger import main as http_prediction_main
from dataPullHTTPTrigger import http_main as data_pull_http_main

app = func.FunctionApp()

@app.timer_trigger(schedule="0 0 5 * * *", arg_name="myTimer", run_on_startup=False,
              use_monitor=False) 
def timer_trigger(myTimer: func.TimerRequest) -> None:
    if myTimer.past_due:
        logging.info('The timer is past due!')
    logging.info('Python timer trigger function executed.')

    try:
        prediction_main()
        logging.info("Prediction function executed successfully.")
    except Exception as e:
        logging.error(f"Error executing prediction function: {e}")
    

@app.route(route="http_trigger", methods=[func.HttpMethod.GET, func.HttpMethod.POST], auth_level=func.AuthLevel.ANONYMOUS)
def http_trigger(req: func.HttpRequest) -> func.HttpResponse:
    logging.info('Python HTTP trigger function processed a request.')

    """Accepts 'days' from query string or JSON body, calls predictioncodehttptrigger.main(days), and returns the resulting JSON to the caller."""
    try:
        # 1) Read 'days' from query string (GET /api/predict?days=7.5)
        days_value = req.params.get("days")
 
        # 2) If not in query string, read from JSON body (POST with {"days": 7.5})
        if days_value is None:
            try:
                body = req.get_json()
            except ValueError:
                body = None
            if body and "days" in body:
                days_value = body["days"]
 
        # 3) Validate and convert
        if days_value is None:
            return func.HttpResponse(
                json.dumps({"error": "Missing required parameter 'days'"}),
                status_code=400,
                mimetype="application/json",
            )
 
        try:
            days_float = float(days_value)
        except (TypeError, ValueError):
            return func.HttpResponse(
                json.dumps({"error": "Invalid 'days' value. Must be a number."}),
                status_code=400,
                mimetype="application/json",
            )
 
        # 4) Call your prediction logic
        result_payload = http_prediction_main(days_float)
 
        # 5) Return JSON back to caller
        return func.HttpResponse(
            json.dumps(result_payload, default=str),
            status_code=200,
            mimetype="application/json",
        )
 
    except Exception:
        logging.exception("Unhandled exception in http_trigger")
        return func.HttpResponse(
            json.dumps({"error": "Internal server error"}),
            status_code=500,
            mimetype="application/json",
        )

@app.route(route="data_pull_http_trigger", auth_level=func.AuthLevel.ANONYMOUS)
def data_pull_http_trigger(req: func.HttpRequest) -> func.HttpResponse:
    logging.info('Python HTTP trigger function processed a request.')
    try:
        # Delegate to the data-pull helper which returns (status_code, body_dict)
        status_code, body = data_pull_http_main(req)

        return func.HttpResponse(
            json.dumps(body, default=str),
            status_code=status_code,
            mimetype="application/json",
        )
    except Exception:
        logging.exception("Unhandled exception in data_pull_http_trigger")
        return func.HttpResponse(
            json.dumps({"error": "Internal server error"}),
            status_code=500,
            mimetype="application/json",
        )
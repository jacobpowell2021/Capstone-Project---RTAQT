import logging
import azure.functions as func
import datetime
from predictionCode import main as prediction_main

app = func.FunctionApp()

@app.timer_trigger(schedule="0 0 0 * * *", arg_name="myTimer", run_on_startup=False,
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
    
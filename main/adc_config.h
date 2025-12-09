#pragma once

#include "adc_config.h"
#include "esp_err.h"
#include "esp_log.h"
#include "esp_adc/adc_oneshot.h"
#include "esp_adc/adc_cali.h"


extern adc_oneshot_unit_handle_t adc1_handle;
extern adc_cali_handle_t adc1_cali_handle;
extern adc_channel_t MQ2;
extern adc_channel_t MQ7;

void init_adc(void);

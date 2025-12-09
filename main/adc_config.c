#include "adc_config.h"
#include "esp_err.h"
#include "esp_log.h"
#include "esp_adc/adc_oneshot.h"
#include "esp_adc/adc_cali.h"
#include "esp_adc/adc_cali_scheme.h"


static const char *TAG = "ADC_CONFIG";

adc_oneshot_unit_handle_t adc1_handle; // create handle
adc_channel_t MQ2 = ADC_CHANNEL_0; // define channels 
adc_channel_t MQ7 = ADC_CHANNEL_3;

adc_cali_handle_t adc1_cali_handle = NULL;

void init_adc(void) {

    // adc init and config 
    adc_oneshot_unit_init_cfg_t init_config1 = {
        .unit_id = ADC_UNIT_1, // select adc unit 1 
        .ulp_mode = ADC_ULP_MODE_DISABLE, // disable low power mode 
    };
    ESP_ERROR_CHECK(adc_oneshot_new_unit(&init_config1, &adc1_handle));

    adc_oneshot_chan_cfg_t config = {
        .bitwidth = ADC_BITWIDTH_DEFAULT, // 12 bit bitwidth 
        .atten = ADC_ATTEN_DB_11, // 11dB attenuation
    };
    ESP_ERROR_CHECK(adc_oneshot_config_channel(adc1_handle, MQ2, &config)); // configure channel for MQ2

    ESP_ERROR_CHECK(adc_oneshot_config_channel(adc1_handle, MQ7, &config));

    // adc calibration 
    adc_cali_line_fitting_config_t cali_config = {
        .unit_id = ADC_UNIT_1, // calibrate unit 1 
        .atten = ADC_ATTEN_DB_11, // 11dB attenuation
        .bitwidth = ADC_BITWIDTH_DEFAULT, // 12 bit bitwidth
    };
    ESP_ERROR_CHECK(adc_cali_create_scheme_line_fitting(&cali_config, &adc1_cali_handle));
}
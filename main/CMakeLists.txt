# Copyright (c) Microsoft Corporation. All rights reserved.
# SPDX-License-Identifier: MIT

# Include directories for main component
set(COMPONENT_INCLUDE_DIRS
    .          # main folder
    ../config  # keep old config includes
)

# Register the main component with ESP-IDF
idf_component_register(
    SRCS
        "azure_iot_freertos_esp32_main.c"
        "sample_azure_iot_pnp.c"
        "sample_azure_iot_pnp_simulated_data.c"
        "adc_config.c"
        "i2c_config.c"
    INCLUDE_DIRS
        ${COMPONENT_INCLUDE_DIRS}  # now only valid directories
    REQUIRES
        esp_event
        esp_wifi
        freertos
        nvs_flash
        coreMQTT
        azure-sdk-for-c
        azure-iot-middleware-freertos
        sample-azure-iot
        driver
        esp_adc
        # esp_driver_i2c
        #esp_driver_i2c       # for gpio.h
        # esp_adc_cal     # uncomment if added via IDF Component Manager
)
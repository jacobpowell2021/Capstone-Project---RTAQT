/*
 * SPDX-FileCopyrightText: 2010-2022 Espressif Systems (Shanghai) CO LTD
 *
 * SPDX-License-Identifier: CC0-1.0
 */

#include <stdio.h>
#include <inttypes.h>
#include "sdkconfig.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_chip_info.h"
#include "esp_flash.h"
#include "esp_system.h"

// non default includes
#include "esp_wifi.h"
#include "esp_mac.h"

#include "esp_event.h"
#include "esp_log.h"
#include "nvs_flash.h"

#include "lwip/err.h"
#include "lwip/sys.h"

#include "driver/i2c_master.h"
#include "driver/i2c_slave.h"

#include "driver/spi_master.h"

#include "driver/gpio.h"
#include "driver/adc.h"

#include "string.h"
#include "lwip/sockets.h"

#include <stdarg.h>
#include "lwip/netdb.h"

#include "string.h"

char message[500] = "Hello World from ESP32!\r\n";

void telnet_server_task()
{
    int server_sock = socket(AF_INET, SOCK_STREAM, 0);
    if (server_sock < 0) { // if error then stop
        return;
    }

    struct sockaddr_in server_addr = { //configure sever details
        .sin_family = AF_INET,
        .sin_port = htons(23),
        .sin_addr.s_addr = INADDR_ANY
    };

    struct sockaddr_in client_addr;
    socklen_t addr_length = sizeof(client_addr);

    int client_sock = accept(server_sock, (struct sockaddr *)&client_addr, &addr_length);

    while (1) { // infinite print loop
        send(client_sock, message, strlen(message), 0);
        vTaskDelay(pdMS_TO_TICKS(1000));
    }
}



// I2C DEFINITIONS //
#define I2C_MASTER_SCL_IO 4
#define I2C_MASTER_SDA_IO 5

// GPIO DEFINITIONS //
#define notCHG_GPIO 34
#define notPG_GPIO 32
#define V0_GPIO 36
#define SHTC3_ctrl 12
#define ENS_ctrl 27








#define IoT_HUB_CONNECTION_STRING "HostName=RTAQTCapstoneProject.azure-devices.net;DeviceId=ESP32;SharedAccessKey=JR13N1/dSgMgEhOYsOrnc+sqZ4k4SWdaUf7aY0ksG3Y="
#define DEVICE_ID "ESP32"



// WIFI FUNCTIONS //

#define NETWORK_NAME "TAMU_Iot"

#define EXAMPLE_ESP_WIFI_SSID      "TAMU_IoT"
#define EXAMPLE_ESP_WIFI_PASS      ""
#define EXAMPLE_ESP_MAXIMUM_RETRY  3

#if CONFIG_ESP_WPA3_SAE_PWE_HUNT_AND_PECK
#define ESP_WIFI_SAE_MODE WPA3_SAE_PWE_HUNT_AND_PECK
#define EXAMPLE_H2E_IDENTIFIER ""
#elif CONFIG_ESP_WPA3_SAE_PWE_HASH_TO_ELEMENT
#define ESP_WIFI_SAE_MODE WPA3_SAE_PWE_HASH_TO_ELEMENT
#define EXAMPLE_H2E_IDENTIFIER CONFIG_ESP_WIFI_PW_ID
#elif CONFIG_ESP_WPA3_SAE_PWE_BOTH
#define ESP_WIFI_SAE_MODE WPA3_SAE_PWE_BOTH
#define EXAMPLE_H2E_IDENTIFIER CONFIG_ESP_WIFI_PW_ID
#endif
#if CONFIG_ESP_WIFI_AUTH_OPEN
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_OPEN
#elif CONFIG_ESP_WIFI_AUTH_WEP
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WEP
#elif CONFIG_ESP_WIFI_AUTH_WPA_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WPA_PSK
#elif CONFIG_ESP_WIFI_AUTH_WPA2_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WPA2_PSK
#elif CONFIG_ESP_WIFI_AUTH_WPA_WPA2_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WPA_WPA2_PSK
#elif CONFIG_ESP_WIFI_AUTH_WPA3_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WPA3_PSK
#elif CONFIG_ESP_WIFI_AUTH_WPA2_WPA3_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WPA2_WPA3_PSK
#elif CONFIG_ESP_WIFI_AUTH_WAPI_PSK
#define ESP_WIFI_SCAN_AUTH_MODE_THRESHOLD WIFI_AUTH_WAPI_PSK
#endif


/* FreeRTOS event group to signal when we are connected*/
static EventGroupHandle_t s_wifi_event_group;

/* The event group allows multiple bits for each event, but we only care about two events:
 * - we are connected to the AP with an IP
 * - we failed to connect after the maximum amount of retries */
#define WIFI_CONNECTED_BIT BIT0
#define WIFI_FAIL_BIT      BIT1

static const char *TAG = "wifi station";

static int s_retry_num = 0;




static void event_handler(void* arg, esp_event_base_t event_base,
                                int32_t event_id, void* event_data)
{
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        if (s_retry_num < EXAMPLE_ESP_MAXIMUM_RETRY) {
            esp_wifi_connect();
            s_retry_num++;
            ESP_LOGI(TAG, "retry to connect to the AP");
        } else {
            xEventGroupSetBits(s_wifi_event_group, WIFI_FAIL_BIT);
        }
        ESP_LOGI(TAG,"connect to the AP fail");
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t* event = (ip_event_got_ip_t*) event_data;
        ESP_LOGI(TAG, "got ip:" IPSTR, IP2STR(&event->ip_info.ip));
        s_retry_num = 0;
        xEventGroupSetBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
    }
}

void wifi_init_sta(void)
{
    s_wifi_event_group = xEventGroupCreate();

    ESP_ERROR_CHECK(esp_netif_init());

    ESP_ERROR_CHECK(esp_event_loop_create_default());
    esp_netif_create_default_wifi_sta();

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));

    esp_event_handler_instance_t instance_any_id;
    esp_event_handler_instance_t instance_got_ip;
    ESP_ERROR_CHECK(esp_event_handler_instance_register(WIFI_EVENT,
                                                        ESP_EVENT_ANY_ID,
                                                        &event_handler,
                                                        NULL,
                                                        &instance_any_id));
    ESP_ERROR_CHECK(esp_event_handler_instance_register(IP_EVENT,
                                                        IP_EVENT_STA_GOT_IP,
                                                        &event_handler,
                                                        NULL,
                                                        &instance_got_ip));

    wifi_config_t wifi_config = {
        .sta = {
            .ssid = EXAMPLE_ESP_WIFI_SSID,
            .password = EXAMPLE_ESP_WIFI_PASS,
            /* Authmode threshold resets to WPA2 as default if password matches WPA2 standards (password len => 8).
             * If you want to connect the device to deprecated WEP/WPA networks, Please set the threshold value
             * to WIFI_AUTH_WEP/WIFI_AUTH_WPA_PSK and set the password with length and format matching to
             * WIFI_AUTH_WEP/WIFI_AUTH_WPA_PSK standards.
             */
        },
    };
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA) );
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config) );
    ESP_ERROR_CHECK(esp_wifi_start() );

    ESP_LOGI(TAG, "wifi_init_sta finished.");

    /* Waiting until either the connection is established (WIFI_CONNECTED_BIT) or connection failed for the maximum
     * number of re-tries (WIFI_FAIL_BIT). The bits are set by event_handler() (see above) */
    EventBits_t bits = xEventGroupWaitBits(s_wifi_event_group,
            WIFI_CONNECTED_BIT | WIFI_FAIL_BIT,
            pdFALSE,
            pdFALSE,
            portMAX_DELAY);

    /* xEventGroupWaitBits() returns the bits before the call returned, hence we can test which event actually
     * happened. */
    if (bits & WIFI_CONNECTED_BIT) {
        ESP_LOGI(TAG, "connected to ap SSID:%s password:%s",
                 EXAMPLE_ESP_WIFI_SSID, EXAMPLE_ESP_WIFI_PASS);
    } else if (bits & WIFI_FAIL_BIT) {
        ESP_LOGI(TAG, "Failed to connect to SSID:%s, password:%s",
                 EXAMPLE_ESP_WIFI_SSID, EXAMPLE_ESP_WIFI_PASS);
    } else {
        ESP_LOGE(TAG, "UNEXPECTED EVENT");
    }
}
// WIFI FUNCTIONS //



esp_err_t read_SHTC3(float *temperature, float *humidity, i2c_master_dev_handle_t SHTC3_dev_handle) {
    // initial sleep
    uint8_t sleep_command[2] = {0xB0, 0x98};
    esp_err_t err = i2c_master_transmit(SHTC3_dev_handle, sleep_command, 2, pdMS_TO_TICKS(200));
    if (err != ESP_OK) return err;

    // wakeup 
    uint8_t wakeup_command[2] = {0x35, 0x17};
    err = i2c_master_transmit(SHTC3_dev_handle, wakeup_command, 2, pdMS_TO_TICKS(200));
    if (err != ESP_OK) return err;

    // ask for measurement 
    uint8_t measurement_ask_command[2] = {0x7C, 0xA2};
    err = i2c_master_transmit(SHTC3_dev_handle, measurement_ask_command, 2, pdMS_TO_TICKS(200));
    if (err != ESP_OK) return err;

    // read measurement
    uint8_t measurement_data[6];
    err = i2c_master_receive(SHTC3_dev_handle, measurement_data, 6, pdMS_TO_TICKS(200));
    if (err != ESP_OK) return err;

    // convert data 
    uint16_t raw_temperature = (measurement_data[0] << 8) | measurement_data[1]; // bytes to int
    *temperature = -45 + 175 * (raw_temperature) / 65535.0; // int to float 
    uint16_t raw_humidity = (measurement_data[3] << 8) | measurement_data[4];
    *humidity = 100 * (raw_humidity) / 65535.0;

    // sleep 
    err = i2c_master_transmit(SHTC3_dev_handle, sleep_command, 2, pdMS_TO_TICKS(200));
    if (err != ESP_OK) return err;
    return ESP_OK;
}

void calculate_depleted_charge(float *timeInterval, float *battery_charge, float *load_current) {

    int raw_current = 0;
    raw_current = adc1_get_raw(ADC1_CHANNEL_0); // read from ADC
    
    *load_current = (((float)raw_current / 4095) * 3.85); // convert raw int to float

    float depletedCharge = *load_current * (*timeInterval) / 1000; // calculate depleted charge

    *battery_charge = *battery_charge - depletedCharge; 

    return;
}

void calculate_added_charge(float *timeInterval, float *battery_charge, float average_charge_current) {

    float addedCharge = average_charge_current * (*timeInterval) / 1000; // calculate addec charge

    *battery_charge = *battery_charge + addedCharge; 

    return;
}








void app_main(void)
{
    gpio_set_level(SHTC3_ctrl, 0);
    // print MAC address to terminal
    uint8_t mac[6];
    esp_read_mac(mac, ESP_MAC_WIFI_STA);

    printf("MAC Address: %02X:%02X:%02X:%02X:%02X:%02X\n",
    mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);


    // WIFI SETUP //

    //Initialize NVS
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);
    
    ESP_LOGI(TAG, "ESP_WIFI_MODE_STA");
    wifi_init_sta();

    // WIFI SETUP //





    /* Print chip information */
    esp_chip_info_t chip_info;
    uint32_t flash_size;
    esp_chip_info(&chip_info);
    // printf("This is %s chip with %d CPU core(s), %s%s%s%s, ",
    //        CONFIG_IDF_TARGET,
    //        chip_info.cores,
    //        (chip_info.features & CHIP_FEATURE_WIFI_BGN) ? "WiFi/" : "",
    //        (chip_info.features & CHIP_FEATURE_BT) ? "BT" : "",
    //        (chip_info.features & CHIP_FEATURE_BLE) ? "BLE" : "",
    //        (chip_info.features & CHIP_FEATURE_IEEE802154) ? ", 802.15.4 (Zigbee/Thread)" : "");

    unsigned major_rev = chip_info.revision / 100;
    unsigned minor_rev = chip_info.revision % 100;
    // printf("silicon revision v%d.%d, ", major_rev, minor_rev);
    // if(esp_flash_get_size(NULL, &flash_size) != ESP_OK) {
    //     printf("Get flash size failed");
    //     return;
    // }

    // printf("%" PRIu32 "MB %s flash\n", flash_size / (uint32_t)(1024 * 1024),
    //        (chip_info.features & CHIP_FEATURE_EMB_FLASH) ? "embedded" : "external");

    // printf("Minimum free heap size: %" PRIu32 " bytes\n", esp_get_minimum_free_heap_size());

    // for (int i = 10; i >= 0; i--) {
    //    printf("Restarting in %d seconds...\n", i);
    //    vTaskDelay(1000 / portTICK_PERIOD_MS);
    //}
    //printf("Restarting now.\n");
    fflush(stdout);
    // esp_restart();

    // I2C Configuration // 
    i2c_master_bus_config_t i2c_mst_config = {
        .clk_source = I2C_CLK_SRC_DEFAULT,
        .i2c_port = I2C_NUM_0,
        .scl_io_num = I2C_MASTER_SCL_IO, // set SCL pin
        .sda_io_num = I2C_MASTER_SDA_IO,// set SDA pin 
        .glitch_ignore_cnt = 7,
        .flags.enable_internal_pullup = true,
    };
    
    i2c_master_bus_handle_t bus_handle;
    ESP_ERROR_CHECK(i2c_new_master_bus(&i2c_mst_config, &bus_handle));
    
    i2c_device_config_t SHTC3_dev_cfg = {
        .dev_addr_length = I2C_ADDR_BIT_LEN_7,
        .device_address = 0x70,
        .scl_speed_hz = 1000000, // 1Mhz clk speed
    };

    i2c_master_dev_handle_t SHTC3_dev_handle;
    ESP_ERROR_CHECK(i2c_master_bus_add_device(bus_handle, &SHTC3_dev_cfg, &SHTC3_dev_handle));


    // GPIO config 
    gpio_set_direction(V0_GPIO, GPIO_MODE_INPUT);
    adc1_config_width(ADC_WIDTH_BIT_12);
    adc1_config_channel_atten(V0_GPIO, ADC_ATTEN_DB_11);
    
    gpio_set_direction(notCHG_GPIO, GPIO_MODE_INPUT);
    gpio_set_direction(notPG_GPIO, GPIO_MODE_INPUT);

    //gpio_set_pull_mode(notCHG_GPIO, GPIO_PULLUP_ONLY);
    //gpio_set_pull_mode(notPG_GPIO, GPIO_PULLUP_ONLY);


    // main loop

    float temperature = 0;
    float humidity = 0;
    float timeInterval = 5000;
    float battery_charge = 9000;
    float total_charge = battery_charge;
    float load_current = 0;

    int battery_not_charging = 0;
    int charger_not_powered = 0;

    struct sockaddr_in dest_addr = {
        .sin_addr.s_addr = inet_addr("10.247.29.171"), // your PC
        .sin_family = AF_INET,
        .sin_port = htons(8080)
    };

    int sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
    char msg[] = "Hello world!\n";

   
    xTaskCreate(telnet_server_task, "telnet_server_task", 4096, NULL, 5, NULL);

    char power_status[30] = "charger connected";
    char charge_status[30] = "battery charging";








    for (int i = 0; i < 10000; i++) {

        sendto(sock, msg, strlen(msg), 0, (struct sockaddr *)&dest_addr, sizeof(dest_addr));

        battery_not_charging = gpio_get_level(notPG_GPIO);
        charger_not_powered = gpio_get_level(notCHG_GPIO);


        if ((battery_not_charging == 1) && (charger_not_powered == 1)) {
            
        }

        if (battery_not_charging == 1) {
            strcpy(power_status, "charger not connected");
        }
        else {
            strcpy(power_status, "charger connected");
            
        }

        if (charger_not_powered == 1) {
            strcpy(charge_status, "battery not charging");
        }
        else {
            strcpy(charge_status, "battery charging");
            
        }


        computeBatteryCharge(&timeInterval, &battery_charge, &load_current);

        esp_err_t err = read_SHTC3(&temperature, &humidity, SHTC3_dev_handle);

        if (err != ESP_OK) {
            ESP_LOGE(TAG, "I2C read failed: %s", esp_err_to_name(err));
        }

        printf("Temperature = %f\n", temperature);
        printf("5 second passed\n");

        printf("Cycle: %d\n", i);
        // printf("Power not good = %d\n", charger_not_powered);
        // printf("Not charging = %d\n", battery_not_charging);
        printf("Battery Charge (C) = %f\n", battery_charge);
        printf("Load current (A) %f\n", load_current);

        float BatteryLife = (battery_charge / total_charge) * 100;
        printf("Battery Life Percent %f\n", BatteryLife);

        sprintf(message, "Load current = %.2f A, %s,  %s, Battery Life = %.2f, Temperature = %.2f\r\n", load_current, power_status, charge_status, BatteryLife, temperature);

        

        vTaskDelay(timeInterval / portTICK_PERIOD_MS);

    }

    vTaskDelete(NULL);

}

    
#include "esp_log.h"
#include "esp_err.h"
#include "driver/i2c.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

#define I2C_MASTER_SDA_IO          16          // GPIO for SDA
#define I2C_MASTER_SCL_IO          17          // GPIO for SCL
#define I2C_MASTER_NUM             I2C_NUM_0   // I2C port number
#define I2C_MASTER_FREQ_HZ         100000      // Clock frequency 100kHz
#define I2C_MASTER_TX_BUF_DISABLE  0           // Master doesn’t need TX buffer
#define I2C_MASTER_RX_BUF_DISABLE  0           // Master doesn’t need RX buffer

void i2c_master_init(void)
{
    i2c_config_t conf = {
        .mode = I2C_MODE_MASTER,
        .sda_io_num = I2C_MASTER_SDA_IO,
        .scl_io_num = I2C_MASTER_SCL_IO,
        .sda_pullup_en = GPIO_PULLUP_ENABLE,
        .scl_pullup_en = GPIO_PULLUP_ENABLE,
        .master.clk_speed = I2C_MASTER_FREQ_HZ
    };

    esp_err_t ret;

    ret = i2c_param_config(I2C_MASTER_NUM, &conf);
    if (ret != ESP_OK) {
        ESP_LOGE("I2C", "i2c_param_config failed: %s", esp_err_to_name(ret));
    }

    ret = i2c_driver_install(I2C_MASTER_NUM, conf.mode,
                             I2C_MASTER_RX_BUF_DISABLE,
                             I2C_MASTER_TX_BUF_DISABLE, 0);
    if (ret != ESP_OK) {
        ESP_LOGE("I2C", "i2c_driver_install failed: %s", esp_err_to_name(ret));
    }
}

#define SENSOR_ADDR 0x38  // Replace with your sensor’s I2C address

static const char *TAG = "SENSOR";

/**
 * @brief Read temperature and humidity from the sensor
 * 
 * @param temperature Pointer to float to store temperature
 * @param humidity Pointer to float to store humidity
 * @return ESP_OK if successful, else an ESP error code
 */

esp_err_t i2c_master_write_slave(uint8_t slave_addr, uint8_t *data_wr, size_t size)
{
    i2c_cmd_handle_t cmd = i2c_cmd_link_create();
    i2c_master_start(cmd);
    i2c_master_write_byte(cmd, (slave_addr << 1) | I2C_MASTER_WRITE, true);
    i2c_master_write(cmd, data_wr, size, true);
    i2c_master_stop(cmd);
    esp_err_t ret = i2c_master_cmd_begin(I2C_MASTER_NUM, cmd, pdMS_TO_TICKS(1000));
    i2c_cmd_link_delete(cmd);
    return ret;
}

esp_err_t i2c_master_read_slave(uint8_t slave_addr, uint8_t *data_rd, size_t size)
{
    i2c_cmd_handle_t cmd = i2c_cmd_link_create();
    i2c_master_start(cmd);
    i2c_master_write_byte(cmd, (slave_addr << 1) | I2C_MASTER_READ, true);

    if (size > 1) {
        i2c_master_read(cmd, data_rd, size - 1, I2C_MASTER_ACK);
    }
    i2c_master_read_byte(cmd, data_rd + size - 1, I2C_MASTER_NACK);

    i2c_master_stop(cmd);
    esp_err_t ret = i2c_master_cmd_begin(I2C_MASTER_NUM, cmd, pdMS_TO_TICKS(1000));
    i2c_cmd_link_delete(cmd);
    return ret;
}

esp_err_t sensor_read(float *temperature, float *humidity)
{
    esp_err_t ret;
    uint8_t status;

    // ESP_LOGI(TAG, "Starting sensor_read()...");

    // Step 0: Wait after power-on (user ensures power-on delay before calling)
    vTaskDelay(pdMS_TO_TICKS(100));

    // ESP_LOGI(TAG, "Step 1: Checking calibration status...");

    // Step 1: Check calibration status
    ret = i2c_master_write_slave(SENSOR_ADDR, (uint8_t[]){0x71}, 1);
    if (ret != ESP_OK) {
        // ESP_LOGE(TAG, "Step 1: Write 0x71 failed: %s", esp_err_to_name(ret));
        return ret;
    }

    ret = i2c_master_read_slave(SENSOR_ADDR, &status, 1);
    if (ret != ESP_OK) {
        // ESP_LOGE(TAG, "Step 1: Read status failed: %s", esp_err_to_name(ret));
        return ret;
    }

    if ((status & 0x18) != 0x18) {
        // ESP_LOGW(TAG, "Sensor not calibrated. User must initialize registers 0x1B, 0x1C, 0x1E.");
        // Initialization routine not included; follow sensor datasheet instructions
        return ESP_FAIL;
    }

    // ESP_LOGI(TAG, "Step 2: Triggering measurement...");

    // Step 2: Trigger measurement
    vTaskDelay(pdMS_TO_TICKS(10));
    ret = i2c_master_write_slave(SENSOR_ADDR, (uint8_t[]){0xAC, 0x33, 0x00}, 3);
    if (ret != ESP_OK) {
        // ESP_LOGE(TAG, "Step 2: Trigger write failed: %s", esp_err_to_name(ret));
        return ret;
    }

    // ESP_LOGI(TAG, "Step 3: Waiting for measurement...");
    // Step 3: Wait 80ms for measurement to complete
    vTaskDelay(pdMS_TO_TICKS(80));

    // Poll status bit 7 until measurement is done
    do {
        ret = i2c_master_write_slave(SENSOR_ADDR, (uint8_t[]){0x71}, 1);
        if (ret != ESP_OK) {
            // ESP_LOGE(TAG, "Step 3: Status write failed: %s", esp_err_to_name(ret));
            return ret;
        }
        ret = i2c_master_read_slave(SENSOR_ADDR, &status, 1);
        if (ret != ESP_OK) {
            // ESP_LOGE(TAG, "Step 3: Status read failed: %s", esp_err_to_name(ret));
            return ret;
        }
    } while (status & 0x80);  // bit 7 = 1 -> measurement not ready




    ESP_LOGI(TAG, "Step 4: Reading measurement data...");

    // Step 4: Read 6 measurement bytes
    uint8_t data[8];
    ret = i2c_master_read_slave(SENSOR_ADDR, data, 6);
    if (ret != ESP_OK) {
        // ESP_LOGE(TAG, "Step 4: Data read failed: %s", esp_err_to_name(ret));
        return ret;
    }

    // Optional: read 7th byte CRC (not implemented here)
    // uint8_t crc;
    // i2c_master_read_slave(SENSOR_ADDR, &crc, 1);

    // Step 5: Convert raw data to temperature and humidity
    uint32_t raw_humidity = ((data[1] << 12) | (data[2] << 4) | (data[3] >> 4)) & 0xFFFFF;
    uint32_t raw_temperature = (((data[3] & 0x0F) << 16) | (data[4] << 8) | data[5]) & 0xFFFFF;

    *humidity = raw_humidity / (float)(1 << 20) * 100.0f;      // %RH
    *temperature = raw_temperature / (float)(1 << 20) * 200.0f - 50.0f; // °C

    return ESP_OK;
}

#define BQ27210_ADDR 0x55  // 7-bit I2C address for bq27210

static const char *TAG_RSOC = "BQ27210_RSOC";

esp_err_t bq27210_get_rsoc(uint8_t *percent)
{
    esp_err_t ret;
    uint8_t reg = 0x0B;   // RSOC register
    uint8_t data[2] = {0};

    // ESP_LOGI(TAG_RSOC, "Starting RSOC read...");

    // Step 1: Set internal register pointer to 0x0B
    ret = i2c_master_write_slave(BQ27210_ADDR, &reg, 1);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG_RSOC, "Failed to write register pointer 0x0B: %s", esp_err_to_name(ret));
        return ret;
    }
    ESP_LOGI(TAG_RSOC, "Register pointer set to 0x0B");

    // Step 2: Perform incremental read (2 bytes: LSB, MSB)
    ret = i2c_master_read_slave(BQ27210_ADDR, data, 2);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG_RSOC, "Failed to read RSOC bytes: %s", esp_err_to_name(ret));
        return ret;
    }
    ESP_LOGI(TAG_RSOC, "Raw RSOC bytes: LSB=0x%02X, MSB=0x%02X", data[0], data[1]);

    // Step 3: Combine into 16-bit value (little endian)
    uint16_t raw_rsoc = data[0] | (data[1] << 8);
    ESP_LOGI(TAG_RSOC, "Raw RSOC value (16-bit) = %d", raw_rsoc);

    // Step 4: RSOC is already in percent units
    *percent = (uint8_t)raw_rsoc;
    ESP_LOGI(TAG_RSOC, "Relative State-of-Charge = %d %%", *percent);

    return ESP_OK;
}


void i2c_scan(void)
{
    i2c_port_t i2c_num = I2C_NUM_0;
    esp_err_t espRc;
    printf("Scanning I2C bus...\n");

    for (uint8_t addr = 1; addr < 127; addr++) {
        i2c_cmd_handle_t cmd = i2c_cmd_link_create();
        i2c_master_start(cmd);
        i2c_master_write_byte(cmd, (addr << 1) | I2C_MASTER_WRITE, true);
        i2c_master_stop(cmd);
        espRc = i2c_master_cmd_begin(i2c_num, cmd, pdMS_TO_TICKS(100));
        i2c_cmd_link_delete(cmd);

        if (espRc == ESP_OK) {
            printf("Found I2C device at 0x%02X\n", addr);
        }
    }
}

#define TVOC_SENSOR_ADDR  0x1A

esp_err_t read_tvoc(float *tvoc_ppb)
{
    esp_err_t ret;
    uint8_t reg = 0x00;   // TVOC data register
    uint8_t buf[5];       // 4 data bytes + CRC

    // Step 1: Write register address (0x00)
    ret = i2c_master_write_slave(TVOC_SENSOR_ADDR, &reg, 1);
    if (ret != ESP_OK) {
        // ESP_LOGE(TAG, "Failed to write register: %s", esp_err_to_name(ret));
        return ret;
    }

    // Step 2: Read 5 bytes back
    ret = i2c_master_read_slave(TVOC_SENSOR_ADDR, buf, 5);
    if (ret != ESP_OK) {
        // ESP_LOGE(TAG, "Failed to read data: %s", esp_err_to_name(ret));
        return ret;
    }

    // Step 3: Combine 4 data bytes into uint32
    *tvoc_ppb = ((uint32_t)buf[0] << 24) |
                ((uint32_t)buf[1] << 16) |
                ((uint32_t)buf[2] << 8)  |
                ((uint32_t)buf[3]);

    // Step 4: CRC check (not implemented here, but buf[4] is the CRC byte)
    ESP_LOGI(TAG, "Raw TVOC: %0.2f ppb", *tvoc_ppb);

    return ESP_OK;
}


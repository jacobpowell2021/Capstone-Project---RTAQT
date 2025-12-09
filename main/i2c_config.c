#include "esp_log.h"
#include "esp_err.h"
#include "driver/i2c.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
     
#define I2C_MASTER_NUM I2C_NUM_0

void i2c_master_init(void)
{
    i2c_config_t conf = {
        .mode = I2C_MODE_MASTER, // the esp is the master device 
        .sda_io_num = 16, // SDA connected to GPIO 16
        .scl_io_num = 17, // SCL connected to GPIO 17
        .sda_pullup_en = GPIO_PULLUP_ENABLE, // enable  pullup resistors
        .scl_pullup_en = GPIO_PULLUP_ENABLE, //  internal pullup resistors
        .master.clk_speed = 30000 // set I2C frequency
    };

    esp_err_t ret;
    ret = i2c_param_config(I2C_MASTER_NUM, &conf); // config i2c parameters
    if (ret != ESP_OK) {
        ESP_LOGE("I2C", "parameters not configured: %s", esp_err_to_name(ret));
    }

    ret = i2c_driver_install(I2C_MASTER_NUM, conf.mode, 0, 0, 0); // instal i2c driver
    if (ret != ESP_OK) {
        ESP_LOGE("I2C", "driver not installed: %s", esp_err_to_name(ret));
    }
}

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
    i2c_cmd_handle_t cmd = i2c_cmd_link_create(); // create command link
    i2c_master_start(cmd);
    i2c_master_write_byte(cmd, (slave_addr << 1) | I2C_MASTER_WRITE, true); // send slave address with write bit 
    i2c_master_write(cmd, data_wr, size, true); // write data to slave
    i2c_master_stop(cmd);
    esp_err_t ret = i2c_master_cmd_begin(I2C_MASTER_NUM, cmd, pdMS_TO_TICKS(1000)); // execute
    i2c_cmd_link_delete(cmd);
    return ret;
}

esp_err_t i2c_master_read_slave(uint8_t slave_addr, uint8_t *data_rd, size_t size)
{
    i2c_cmd_handle_t cmd = i2c_cmd_link_create(); // create command link
    i2c_master_start(cmd);
    i2c_master_write_byte(cmd, (slave_addr << 1) | I2C_MASTER_READ, true); // send slave address with read bit

    if (size > 1) {
        i2c_master_read(cmd, data_rd, size - 1, I2C_MASTER_ACK); // read all but last byte
    }
    i2c_master_read_byte(cmd, data_rd + size - 1, I2C_MASTER_NACK); // read last byte

    i2c_master_stop(cmd);
    esp_err_t ret = i2c_master_cmd_begin(I2C_MASTER_NUM, cmd, pdMS_TO_TICKS(1000)); // execute
    i2c_cmd_link_delete(cmd);
    return ret;
}


#define TH_SENSOR_ADDR 0x38  
esp_err_t read_TH(float *temperature, float *humidity)
{
    esp_err_t ret;
    uint8_t data[8];

    ret = i2c_master_write_slave(TH_SENSOR_ADDR, (uint8_t[]){0xAC, 0x33, 0x00}, 3); // trigger measurement
    if (ret != ESP_OK) {
        return ret;
    }

    ret = i2c_master_read_slave(TH_SENSOR_ADDR, data, 6); // read 6 bytes 
    if (ret != ESP_OK) {
        return ret;
    }

    uint32_t raw_humidity = ((data[1] << 12) | (data[2] << 4) | (data[3] >> 4)) & 0xFFFFF; // combine 20 bit RH 
    uint32_t raw_temperature = (((data[3] & 0x0F) << 16) | (data[4] << 8) | data[5]) & 0xFFFFF; // combine 20 bit Temp

    *humidity = raw_humidity / (float)(1 << 20) * 100.0f; // RH calculation 
    *temperature = raw_temperature / (float)(1 << 20) * 200.0f - 50.0f; // Temperature caluclation

    return ESP_OK;
}

#define BATMON_ADDR 0x36
esp_err_t read_VCELL(float* battery_voltage) {

    esp_err_t ret;
    uint8_t vcell_reg = 0x02; // vcell register
    uint8_t data[2] = {0};

    ret = i2c_master_write_slave(BATMON_ADDR, &vcell_reg, 1); // select vcell register
    if (ret != ESP_OK) {
        return ret;
    }

    ret = i2c_master_read_slave(BATMON_ADDR, data, 2); // read 2 bytes
    if (ret != ESP_OK) {
        return ret;
    }

    uint16_t raw_vcell = ((uint16_t)data[0] << 8) | data[1]; // combine bytes 
    *battery_voltage = (float)raw_vcell * 78.125 * 0.000001; // 78.125 µV per LSBit

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
    uint8_t TVOC_reg = 0x00; // TVOC register
    uint8_t data[5];      

    ret = i2c_master_write_slave(TVOC_SENSOR_ADDR, &TVOC_reg, 1); // select TVOC register
    if (ret != ESP_OK) {
        return ret;
    }

    ret = i2c_master_read_slave(TVOC_SENSOR_ADDR, data, 5); // read 5 bytes 
    if (ret != ESP_OK) {
        return ret;
    }

    *tvoc_ppb = ((uint32_t)data[0] << 24) | // combine 4 bytes into TVOC value 
                ((uint32_t)data[1] << 16) |
                ((uint32_t)data[2] << 8)  |
                ((uint32_t)data[3]);

    return ESP_OK;
}






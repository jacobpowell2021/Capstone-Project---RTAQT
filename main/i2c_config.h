#ifndef I2C_CONFIG_H
#define I2C_CONFIG_H

#include "driver/i2c.h"
#include "esp_err.h"

esp_err_t read_TH(float *temperature, float *humidity);

esp_err_t i2c_master_init(void);

esp_err_t i2c_master_write_slave(uint8_t slave_addr, uint8_t *data_wr, size_t size);

esp_err_t i2c_master_read_slave(uint8_t slave_addr, uint8_t *data_rd, size_t size);

#define BATMON_ADDR 0x36

esp_err_t read_VCELL(float *battery_voltage);

void i2c_scan(void);

esp_err_t read_tvoc(float *tvoc_ppb);



#endif // I2C_CONFIG_H
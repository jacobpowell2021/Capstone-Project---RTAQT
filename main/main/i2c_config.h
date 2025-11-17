#ifndef I2C_CONFIG_H
#define I2C_CONFIG_H

#include "driver/i2c.h"
#include "esp_err.h"

esp_err_t i2c_master_write_slave(uint8_t slave_addr, uint8_t *data_wr, size_t size);
esp_err_t i2c_master_read_slave(uint8_t slave_addr, uint8_t *data_rd, size_t size);

esp_err_t sensor_read(float *temperature, float *humidity);

// I2C master configuration
#define I2C_MASTER_SDA_IO          16          // SDA pin
#define I2C_MASTER_SCL_IO          17          // SCL pin
#define I2C_MASTER_NUM             I2C_NUM_0   // I2C port number
#define I2C_MASTER_FREQ_HZ         30000      // I2C clock frequency
#define I2C_MASTER_TX_BUF_DISABLE  0
#define I2C_MASTER_RX_BUF_DISABLE  0

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Initialize the I2C master driver
 *
 * @return ESP_OK on success, otherwise an error code
 */
esp_err_t i2c_master_init(void);

/**
 * @brief Write data to an I2C slave device
 *
 * @param slave_addr I2C slave address
 * @param data_wr Pointer to data to write
 * @param size Number of bytes to write
 * @return ESP_OK on success, otherwise an error code
 */
esp_err_t i2c_master_write_slave(uint8_t slave_addr, uint8_t *data_wr, size_t size);

/**
 * @brief Read data from an I2C slave device
 *
 * @param slave_addr I2C slave address
 * @param data_rd Pointer to buffer to store read data
 * @param size Number of bytes to read
 * @return ESP_OK on success, otherwise an error code
 */
esp_err_t i2c_master_read_slave(uint8_t slave_addr, uint8_t *data_rd, size_t size);

/**
 * @file battery_monitor.h
 * @brief I2C driver interface for the battery monitor (0x36)
 *
 * This interface allows reading the battery state-of-charge (SOC)
 * from a smart fuel gauge IC using the standard read-data protocol:
 * 
 * Sequence: 
 *  S → SAddr(W) → MAddr → Sr → SAddr(R) → Data0 → Data1 → P
 * 
 * Register 0x04 = SOC (2 bytes, 1%/256 LSB)
 */

/** @brief 7-bit I2C address for the battery monitor */
#define BATMON_ADDR 0x36

/** @brief SOC register address */
#define BATMON_REG_SOC 0x04

/**
 * @brief Read battery relative state of charge (SOC)
 *
 * Reads the SOC register (0x04) from the battery monitor IC
 * and converts the result into a percentage value.
 *
 * @param[out] percent Pointer to float variable that receives the SOC (%)
 * @return esp_err_t   ESP_OK on success, or I2C/communication error
 */
esp_err_t rsoc(float *percent);

esp_err_t VCELL(float *battery_voltage);

esp_err_t set_vreset_register(float vreset_voltage, bool disable_comparator);

/**
 * @brief Scan the I2C bus for connected devices and log their addresses
 */
void i2c_scan(void);

// Function prototype to read TVOC concentration (ppb)
esp_err_t read_tvoc(float *tvoc_ppb);

esp_err_t set_RCOMP();




#ifdef __cplusplus
}
#endif

#endif // I2C_CONFIG_H
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
#define I2C_MASTER_FREQ_HZ         100000      // I2C clock frequency
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

#ifdef __cplusplus
}
#endif

#endif // I2C_CONFIG_H
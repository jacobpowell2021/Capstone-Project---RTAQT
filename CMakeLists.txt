idf_component_register(SRCS "hello_world_main.c"
                    PRIV_REQUIRES spi_flash
                    PRIV_REQUIRES esp_wifi
                    PRIV_REQUIRES nvs_flash
                    PRIV_REQUIRES esp_driver_i2c
                    INCLUDE_DIRS ""
                    INCLUDE_DIRS "."
                    REQUIRES driver)

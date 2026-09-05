package com.mplad.fraud_detection;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MpladBackendApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(MpladBackendApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MpladBackendApplication.class, args);
    }

    /**
     * Opens one connection at startup so local configuration problems are visible
     * immediately. This does not read or change any database tables.
     */
    @Bean
    CommandLineRunner verifyDatabaseConnection(DataSource dataSource) {
        return arguments -> {
            try (Connection connection = dataSource.getConnection()) {
                LOGGER.info(
                        "Connected to MySQL database '{}' on {}.",
                        connection.getCatalog(),
                        connection.getMetaData().getURL()
                );
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not connect to the configured MySQL database.", exception);
            }
        };
    }
}

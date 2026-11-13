package com.javacodeex.kafka.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the order persistence and Kafka publishing application.
 */
@SpringBootApplication
public class KafkaOrderServiceApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(KafkaOrderServiceApplication.class, args);
    }
}

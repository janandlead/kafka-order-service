package com.javacodeex.kafka.order.config;

import com.javacodeex.kafka.order.entity.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Configures Kafka publishing for string keys and order payloads.
 */
@Configuration
public class KafkaProducerConfig {

    /**
     * Creates the template used to send orders through the configured producer factory.
     *
     * @param producerFactory factory supplying Kafka producers and their serialization settings
     * @return template for publishing order records
     */
    @Bean
    public KafkaTemplate<String, Order> kafkaTemplate(
            ProducerFactory<String, Order> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}

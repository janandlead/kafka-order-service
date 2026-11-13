package com.javacodeex.kafka.order.producer;

import com.javacodeex.kafka.order.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes orders to the configured Kafka topic and logs asynchronous send outcomes.
 */
@Component
public class OrderKafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderKafkaProducer.class);
    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final String topic;

    /**
     * Creates a producer for the topic configured by {@code app.kafka.topic}.
     *
     * @param kafkaTemplate template used to send order records
     * @param topic destination topic name
     */
    public OrderKafkaProducer(KafkaTemplate<String, Order> kafkaTemplate,
                              @Value("${app.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * Initiates a send using the persisted order's identifier as the record key.
     * Does not wait for broker acknowledgment; completion failures are logged.
     * Exceptions thrown while initiating the send propagate to the caller.
     *
     * @param order order to publish, with a non-null database identifier
     */
    public void publish(Order order) {
        CompletableFuture<SendResult<String, Order>> future =
                kafkaTemplate.send(topic, order.getId().toString(), order);
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Failed to publish order {} to Kafka topic {}",
                        order.getId(), topic, exception);
            } else {
                log.info("Published order {} to topic {}, partition {}, offset {}",
                        order.getId(), topic, result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}

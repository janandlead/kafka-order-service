package com.javacodeex.kafka.order.service;

import com.javacodeex.kafka.order.dto.OrderRequest;
import com.javacodeex.kafka.order.dto.OrderResponse;
import com.javacodeex.kafka.order.entity.Order;
import com.javacodeex.kafka.order.producer.OrderKafkaProducer;
import com.javacodeex.kafka.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * Coordinates order persistence and asynchronous Kafka publication.
 */
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderKafkaProducer orderKafkaProducer;

    /**
     * Saves an order with status {@code CREATED} and the current local timestamp,
     * then initiates publication before the database transaction completes.
     * Database persistence and Kafka delivery are not atomic: an asynchronous
     * send failure does not roll back the database transaction.
     *
     * @param request validated product, quantity, and price details
     * @return details of the saved order, without waiting for Kafka acknowledgment
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Order order = new Order(request.getProductCode(), request.getProductName(),
                request.getQuantity(), request.getPrice(), "CREATED", LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        orderKafkaProducer.publish(savedOrder);

        return new OrderResponse(savedOrder.getId(), savedOrder.getProductCode(),
                savedOrder.getProductName(), savedOrder.getQuantity(), savedOrder.getPrice(),
                savedOrder.getStatus());
    }
}

package com.javacodeex.kafka.order.repository;

import com.javacodeex.kafka.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides JPA persistence operations for orders identified by {@link Long} keys.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}

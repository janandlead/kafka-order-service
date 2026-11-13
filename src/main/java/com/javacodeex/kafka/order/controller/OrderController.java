package com.javacodeex.kafka.order.controller;

import com.javacodeex.kafka.order.dto.OrderRequest;
import com.javacodeex.kafka.order.dto.OrderResponse;
import com.javacodeex.kafka.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

/**
 * Exposes the HTTP endpoint for creating orders and initiating Kafka publication.
 */
@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /**
     * Handles {@code POST /publish}, returning HTTP 201 when order creation succeeds.
     * The response does not confirm Kafka broker acknowledgment.
     *
     * @param request order details validated by Spring before this method is invoked
     * @return the saved order's identifier, product details, and status
     */
    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }
}

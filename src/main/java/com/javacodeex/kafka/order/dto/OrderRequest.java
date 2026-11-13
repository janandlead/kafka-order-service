package com.javacodeex.kafka.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * Input for creating an order. Product code and name must be nonblank,
 * quantity must be at least one, and price must be at least 0.01.
 * Quantity and price are required.
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {
    @NotBlank private String productCode;
    @NotBlank private String productName;
    @NotNull @Min(1) private Integer quantity;
    @NotNull @DecimalMin(value = "0.01") private BigDecimal price;

}

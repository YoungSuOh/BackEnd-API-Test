package com.example.orderapi.dto;

import com.example.orderapi.domain.OrderItem;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        int quantity,
        long unitPrice,
        long lineAmount
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineAmount()
        );
    }
}

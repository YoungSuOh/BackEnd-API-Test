package com.example.orderapi.dto;

import com.example.orderapi.domain.Order;
import com.example.orderapi.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        OrderStatus status,
        long totalAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
        );
    }
}

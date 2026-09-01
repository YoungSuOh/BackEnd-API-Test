package com.example.orderapi.dto;

import com.example.orderapi.domain.Product;

public record ProductResponse(
        Long id,
        String name,
        long price,
        int stock
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(), product.getStock());
    }
}

package com.example.orderapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderCreateRequest(
        @NotBlank String customerName,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
}

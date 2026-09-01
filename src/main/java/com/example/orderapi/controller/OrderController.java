package com.example.orderapi.controller;

import com.example.orderapi.domain.OrderStatus;
import com.example.orderapi.dto.OrderCreateRequest;
import com.example.orderapi.dto.OrderResponse;
import com.example.orderapi.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 주문 생성
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    // 단건 주문 조회
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    // 주문 목록 조회 (상태 필터 + 페이징)
    @GetMapping
    public Page<OrderResponse> listOrders(@RequestParam(required = false) OrderStatus status, Pageable pageable) {
        return orderService.listOrders(status, pageable);
    }

    // 주문 결제 처리
    @PostMapping("/{id}/pay")
    public OrderResponse pay(@PathVariable Long id) {
        return orderService.pay(id);
    }
}

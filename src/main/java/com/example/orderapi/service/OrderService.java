package com.example.orderapi.service;

import com.example.orderapi.domain.Order;
import com.example.orderapi.domain.OrderItem;
import com.example.orderapi.domain.OrderStatus;
import com.example.orderapi.domain.Product;
import com.example.orderapi.dto.OrderCreateRequest;
import com.example.orderapi.dto.OrderItemRequest;
import com.example.orderapi.dto.OrderResponse;
import com.example.orderapi.exception.BusinessException;
import com.example.orderapi.repository.OrderRepository;
import com.example.orderapi.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    // 상품 조회 -> 재고 차감 -> 주문 항목 생성 -> 저장
    public OrderResponse createOrder(OrderCreateRequest request) {
        Order order = new Order(request.customerName());
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> BusinessException.notFound("상품을 찾을 수 없습니다. id=" + itemRequest.productId()));
            product.decreaseStock(itemRequest.quantity());
            order.addItem(new OrderItem(product, itemRequest.quantity()));
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return OrderResponse.from(findOrder(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(OrderStatus status, Pageable pageable) {
        Page<Order> orders = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        return orders.map(OrderResponse::from);
    }

    // 결제 처리 후 주문 상태를 PAID로 변경
    public OrderResponse pay(Long id) {
        Order order = findOrder(id);
        // ponytail: no real PG integrated yet, charge always succeeds.
        // Swap for a real HTTP client (RestClient/WebClient) when one is added.
        order.pay();
        return OrderResponse.from(order);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다. id=" + id));
    }
}

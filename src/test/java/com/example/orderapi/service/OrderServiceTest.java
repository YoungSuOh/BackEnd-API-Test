package com.example.orderapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.orderapi.domain.Order;
import com.example.orderapi.domain.OrderStatus;
import com.example.orderapi.domain.Product;
import com.example.orderapi.dto.OrderCreateRequest;
import com.example.orderapi.dto.OrderItemRequest;
import com.example.orderapi.dto.OrderResponse;
import com.example.orderapi.exception.BusinessException;
import com.example.orderapi.repository.OrderRepository;
import com.example.orderapi.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_computesSnapshotPriceAndDecreasesStock() {
        Product product = new Product(1L, "Keyboard", 45000L, 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrder(
                new OrderCreateRequest("paul", List.of(new OrderItemRequest(1L, 2))));

        assertThat(response.totalAmount()).isEqualTo(90000L);
        assertThat(product.getStock()).isEqualTo(8);
        assertThat(response.items().get(0).unitPrice()).isEqualTo(45000L);
    }

    @Test
    void createOrder_rejectsWhenStockInsufficient() {
        Product product = new Product(1L, "Keyboard", 45000L, 1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrder(
                new OrderCreateRequest("paul", List.of(new OrderItemRequest(1L, 2)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void pay_onlyAllowedFromCreatedStatus() {
        var order = new Order("paul");
        order.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.pay(1L);

        assertThatThrownBy(() -> orderService.pay(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getOrder_throwsNotFoundWhenMissing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void pay_throwsNotFoundWhenMissing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.pay(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createOrder_throwsNotFoundWhenProductMissing() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(
                new OrderCreateRequest("paul", List.of(new OrderItemRequest(1L, 1)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void listOrders_delegatesToStatusQueryWhenStatusGiven() {
        Order order = new Order("paul");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findByStatus(OrderStatus.CREATED, pageable)).thenReturn(page);

        Page<OrderResponse> result = orderService.listOrders(OrderStatus.CREATED, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listOrders_usesFindAllWhenStatusIsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(new Order("paul")));
        when(orderRepository.findAll(pageable)).thenReturn(page);

        Page<OrderResponse> result = orderService.listOrders(null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }
}

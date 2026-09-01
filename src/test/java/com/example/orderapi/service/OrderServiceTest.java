package com.example.orderapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
        var order = new com.example.orderapi.domain.Order("paul");
        order.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.pay(1L);

        assertThatThrownBy(() -> orderService.pay(1L))
                .isInstanceOf(BusinessException.class);
    }
}

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

    // 주문 생성 시 상품의 "현재" 단가를 스냅샷으로 찍어 총액을 계산하고, 재고를 차감하는지 확인한다.
    @Test
    void createOrder_computesSnapshotPriceAndDecreasesStock() {
        // given: 재고 10개, 단가 45000원인 상품을 조회하면 반환하도록 스텁하고,
        // save()는 전달받은 엔티티를 그대로 반환하도록 스텁한다
        Product product = new Product(1L, "Keyboard", 45000L, 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when: 해당 상품 2개를 담아 주문을 생성하면
        OrderResponse response = orderService.createOrder(
                new OrderCreateRequest("paul", List.of(new OrderItemRequest(1L, 2))));

        // then: 총액은 45000 * 2 = 90000원,
        // 재고는 10 - 2 = 8개로 차감되어야 하며,
        // 응답 아이템의 단가는 주문 시점 상품가(45000원)가 스냅샷되어야 한다
        assertThat(response.totalAmount()).isEqualTo(90000L);
        assertThat(product.getStock()).isEqualTo(8);
        assertThat(response.items().get(0).unitPrice()).isEqualTo(45000L);
    }

    // 재고보다 많은 수량을 주문하면 주문 생성 자체가 거부되는지 확인한다.
    @Test
    void createOrder_rejectsWhenStockInsufficient() {
        // given: 재고가 1개뿐인 상품
        Product product = new Product(1L, "Keyboard", 45000L, 1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // when & then: 2개를 주문하려고 하면 재고 부족으로 BusinessException이 발생해야 한다
        assertThatThrownBy(() -> orderService.createOrder(
                new OrderCreateRequest("paul", List.of(new OrderItemRequest(1L, 2)))))
                .isInstanceOf(BusinessException.class);
    }

    // 결제는 CREATED 상태에서만 가능하며, 같은 주문을 두 번 결제하면 두 번째 호출이 실패하는지 확인한다.
    @Test
    void pay_onlyAllowedFromCreatedStatus() {
        // given: 아직 결제되지 않은(CREATED) 주문
        var order = new Order("paul");
        order.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // when: 첫 번째 결제는 정상 처리되고
        orderService.pay(1L);

        // then: 같은 주문에 대한 두 번째 결제 시도는 예외가 발생해야 한다(중복 결제 방지)
        assertThatThrownBy(() -> orderService.pay(1L))
                .isInstanceOf(BusinessException.class);
    }

    // 존재하지 않는 주문 id로 단건 조회 시 404에 해당하는 예외가 발생하는지 확인한다.
    @Test
    void getOrder_throwsNotFoundWhenMissing() {
        // given: 리포지토리가 해당 id로 아무것도 찾지 못하도록 스텁
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then: 조회 시 BusinessException(Not Found)이 발생해야 한다
        assertThatThrownBy(() -> orderService.getOrder(1L))
                .isInstanceOf(BusinessException.class);
    }

    // 존재하지 않는 주문 id로 결제 시도 시에도 마찬가지로 404에 해당하는 예외가 발생하는지 확인한다.
    @Test
    void pay_throwsNotFoundWhenMissing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.pay(1L))
                .isInstanceOf(BusinessException.class);
    }

    // 주문에 담으려는 상품 자체가 존재하지 않으면 주문 생성이 실패하는지 확인한다.
    @Test
    void createOrder_throwsNotFoundWhenProductMissing() {
        // given: 상품 조회가 항상 빈 값을 반환하도록 스텁(즉, 존재하지 않는 상품)
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then: 해당 상품으로 주문을 생성하려 하면 BusinessException이 발생해야 한다
        assertThatThrownBy(() -> orderService.createOrder(
                new OrderCreateRequest("paul", List.of(new OrderItemRequest(1L, 1)))))
                .isInstanceOf(BusinessException.class);
    }

    // 상태(status) 조건이 주어지면 상태별 조회 메서드로 위임하는지 확인한다.
    @Test
    void listOrders_delegatesToStatusQueryWhenStatusGiven() {
        // given: CREATED 상태로 조회하면 주문 1건이 담긴 페이지를 반환하도록 스텁
        Order order = new Order("paul");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findByStatus(OrderStatus.CREATED, pageable)).thenReturn(page);

        // when: status=CREATED 조건으로 목록을 조회하면
        Page<OrderResponse> result = orderService.listOrders(OrderStatus.CREATED, pageable);

        // then: findByStatus 결과가 그대로(1건) 반환되어야 한다
        assertThat(result.getContent()).hasSize(1);
    }

    // status가 null이면(필터 없음) 전체 조회(findAll)로 위임하는지 확인한다.
    @Test
    void listOrders_usesFindAllWhenStatusIsNull() {
        // given: 전체 조회 시 주문 1건이 담긴 페이지를 반환하도록 스텁
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(new Order("paul")));
        when(orderRepository.findAll(pageable)).thenReturn(page);

        // when: status를 null로 넘겨 목록을 조회하면
        Page<OrderResponse> result = orderService.listOrders(null, pageable);

        // then: findAll 결과가 그대로(1건) 반환되어야 한다
        assertThat(result.getContent()).hasSize(1);
    }
}

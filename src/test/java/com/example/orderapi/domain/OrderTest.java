package com.example.orderapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orderapi.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

// Order 도메인 객체 자체의 규칙(금액 합산, 상태 전이)을 순수 단위 테스트로 검증한다.
// 리포지토리나 스프링 컨텍스트 없이 객체만 생성해서 검증하므로 빠르고 독립적이다.
class OrderTest {

    // 주문에 아이템을 추가하면 총액이 (단가 * 수량)만큼 누적되는지 확인한다.
    @Test
    void addItem_accumulatesTotalAmount() {
        // given: 신규 주문과, 개당 45000원짜리 상품(재고 10개)
        Order order = new Order("paul");
        Product product = new Product(1L, "Keyboard", 45000L, 10);

        // when: 해당 상품을 2개 담은 주문 아이템을 추가한다
        order.addItem(new OrderItem(product, 2));

        // then: 총액은 45000 * 2 = 90000원이고, 아이템은 1건만 존재해야 한다
        assertThat(order.getTotalAmount()).isEqualTo(90000L);
        assertThat(order.getItems()).hasSize(1);
    }

    // 신규 생성된(CREATED) 주문이 결제되면 상태가 PAID로 바뀌는지 확인한다.
    @Test
    void pay_transitionsFromCreatedToPaid() {
        // given: 방금 생성되어 아직 결제되지 않은 주문
        Order order = new Order("paul");

        // when: 결제를 진행하면
        order.pay();

        // then: 상태가 PAID로 전이된다
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    // 이미 결제된 주문을 다시 결제하려고 하면 예외가 발생하는지 확인한다(중복 결제 방지).
    @Test
    void pay_rejectsWhenNotCreated() {
        // given: 이미 한 번 결제되어 PAID 상태인 주문
        Order order = new Order("paul");
        order.pay();

        // when & then: 다시 결제를 시도하면 BusinessException이 발생하고,
        // HTTP 상태 코드는 409 CONFLICT여야 한다(잘못된 상태 전이이므로).
        assertThatThrownBy(order::pay)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}

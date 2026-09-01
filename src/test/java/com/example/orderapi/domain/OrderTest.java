package com.example.orderapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orderapi.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class OrderTest {

    @Test
    void addItem_accumulatesTotalAmount() {
        Order order = new Order("paul");
        Product product = new Product(1L, "Keyboard", 45000L, 10);

        order.addItem(new OrderItem(product, 2));

        assertThat(order.getTotalAmount()).isEqualTo(90000L);
        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    void pay_transitionsFromCreatedToPaid() {
        Order order = new Order("paul");

        order.pay();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void pay_rejectsWhenNotCreated() {
        Order order = new Order("paul");
        order.pay();

        assertThatThrownBy(order::pay)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}

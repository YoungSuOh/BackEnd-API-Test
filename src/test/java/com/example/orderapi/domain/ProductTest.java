package com.example.orderapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orderapi.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

// Product 도메인의 재고 차감 로직(정상 차감 / 재고 부족 시 거절)을 검증한다.
class ProductTest {

    // 재고가 충분할 때 정상적으로 차감되는지 확인한다.
    @Test
    void decreaseStock_reducesStockWhenEnough() {
        // given: 재고 10개인 상품
        Product product = new Product(1L, "Keyboard", 45000L, 10);

        // when: 3개를 차감하면
        product.decreaseStock(3);

        // then: 남은 재고는 10 - 3 = 7개여야 한다
        assertThat(product.getStock()).isEqualTo(7);
    }

    // 재고보다 많은 수량을 차감하려고 하면 예외가 발생하는지 확인한다(재고 부족 방어).
    @Test
    void decreaseStock_rejectsWhenInsufficient() {
        // given: 재고가 2개뿐인 상품
        Product product = new Product(1L, "Keyboard", 45000L, 2);

        // when & then: 3개를 차감하려고 하면 BusinessException이 발생하고,
        // HTTP 상태 코드는 409 CONFLICT여야 한다(재고 부족은 클라이언트 요청과 서버 상태의 충돌이므로).
        assertThatThrownBy(() -> product.decreaseStock(3))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}

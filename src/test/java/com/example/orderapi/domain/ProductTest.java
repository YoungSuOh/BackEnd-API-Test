package com.example.orderapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orderapi.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ProductTest {

    @Test
    void decreaseStock_reducesStockWhenEnough() {
        Product product = new Product(1L, "Keyboard", 45000L, 10);

        product.decreaseStock(3);

        assertThat(product.getStock()).isEqualTo(7);
    }

    @Test
    void decreaseStock_rejectsWhenInsufficient() {
        Product product = new Product(1L, "Keyboard", 45000L, 2);

        assertThatThrownBy(() -> product.decreaseStock(3))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}

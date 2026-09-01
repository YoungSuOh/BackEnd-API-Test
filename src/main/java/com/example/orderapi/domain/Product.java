package com.example.orderapi.domain;

import com.example.orderapi.exception.BusinessException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long price;

    private Integer stock;

    // 재고 부족 시 예외, 아니면 차감
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw BusinessException.conflict("재고가 부족합니다. 상품 id=" + id);
        }
        this.stock -= quantity;
    }
}

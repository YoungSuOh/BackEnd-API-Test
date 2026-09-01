package com.example.orderapi.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.example.orderapi.exception.BusinessException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Long totalAmount;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order(String customerName) {
        this.customerName = customerName;
        this.status = OrderStatus.CREATED;
        this.createdAt = LocalDateTime.now();
        this.totalAmount = 0L;
    }

    // 주문 항목 추가 + 총액 누적
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        totalAmount += item.getLineAmount();
    }

    // CREATED 상태에서만 결제 가능
    public void pay() {
        if (this.status != OrderStatus.CREATED) {
            throw BusinessException.conflict("생성(CREATED) 상태의 주문만 결제할 수 있습니다. 현재 상태=" + status);
        }
        this.status = OrderStatus.PAID;
    }
}

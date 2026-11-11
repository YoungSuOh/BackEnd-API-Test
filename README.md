# Order API

## 부하테스트 시 발생할 수 있는 문제점

### 1. N+1 쿼리

주문 목록/상세를 DTO로 변환하는 과정에서 지연 로딩된 연관 엔티티에 접근한다.

```java
// OrderResponse.java
public static OrderResponse from(Order order) {
    return new OrderResponse(
            ...
            order.getItems().stream().map(OrderItemResponse::from).toList() // items: 주문 1건당 쿼리 1번
    );
}
```

```java
// OrderItemResponse.java
public static OrderItemResponse from(OrderItem item) {
    return new OrderItemResponse(
            item.getId(),
            item.getProduct().getId(),   // product: 아이템 1건당 쿼리 1번
            item.getProduct().getName(),
            ...
    );
}
```

```java
// OrderItem.java
@ManyToOne // 기본 fetch = EAGER
@JoinColumn(name = "product_id")
private Product product;
```

`Order.items`는 기본 LAZY, `OrderItem.product`는 기본 EAGER라서 주문 목록을 조회하면 `1(주문 목록) + N(주문별 items) + M(아이템별 product)` 쿼리가 발생한다. 동시 요청이 늘어날수록 쿼리 수가 배로 늘어 DB 커넥션 점유 시간이 길어지고, 부하테스트에서는 처리량 저하·커넥션 풀 고갈로 나타난다.

개선 방향: `OrderItem.product`를 `fetch = LAZY`로 명시하고, `Product`와 `Order.items`에 `@BatchSize`를 붙여 개별 select를 `IN (...)` 배치 조회로 묶는다.

### 2. 재고 차감 경쟁 조건 (Lost Update)

```java
// Product.java
public void decreaseStock(int quantity) {
    if (this.stock < quantity) {
        throw BusinessException.conflict("재고가 부족합니다. 상품 id=" + id);
    }
    this.stock -= quantity; // 인메모리 검사 후 감소, UPDATE는 커밋 시점에 발생
}
```

재고 검증과 차감이 인메모리에서 이루어지고 `@Version` 같은 락이 없다. 같은 상품에 대한 주문 요청이 동시에 들어오면 두 트랜잭션이 같은 `stock` 값을 읽고 각자 차감한 뒤 커밋하므로, 실제보다 재고가 더 줄거나(오버셀링) 음수가 될 수 있다. 응답 코드에는 드러나지 않고 DB 값 검증에서만 확인되는 문제라 부하테스트에서 동시성 시나리오를 넣지 않으면 놓치기 쉽다.

개선 방향: `stock = stock - :qty WHERE id = :id AND stock >= :qty` 형태의 원자적 조건부 UPDATE로 교체하고, 영향받은 row가 0이면 재고 부족으로 처리한다.

### 3. Open-in-View + SQL 로깅

```yaml
# application.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

`spring.jpa.open-in-view`를 명시적으로 끄지 않아 기본값 `true`가 적용되어, DB 커넥션이 컨트롤러 응답 직전까지 열려 있다. 1번 N+1 문제와 겹치면 커넥션 점유 시간이 더 길어진다. 또한 `show-sql`/`format_sql`이 켜져 있어 요청마다 SQL 로깅 오버헤드가 발생하고, 이는 부하테스트 결과(처리량/지연)를 실제보다 나쁘게 왜곡시킨다.

개선 방향: `spring.jpa.open-in-view: false` 설정, 운영/부하테스트 프로파일에서는 `show-sql`을 끈다.

### 4. 상품 목록 페이징 없음

```java
// ProductController.java
@GetMapping
public List<ProductResponse> listProducts() {
    return productRepository.findAll().stream().map(ProductResponse::from).toList();
}
```

상품 전체를 페이징 없이 한 번에 조회한다. 상품 수가 늘어나면 응답 크기와 메모리 사용량이 함께 커져, 부하테스트에서 가장 먼저 느려지는 엔드포인트가 될 가능성이 높다.

개선 방향: `Pageable`을 받아 `findAll(pageable)`로 변경한다.

### 5. 페이지 크기 상한 없음

```java
// OrderController.java
@GetMapping
public Page<OrderResponse> listOrders(@RequestParam(required = false) OrderStatus status, Pageable pageable) {
    return orderService.listOrders(status, pageable);
}
```

`Pageable`을 그대로 바인딩해서 `size` 파라미터에 상한이 없다. `?size=100000` 같은 요청이 대량 조회를 유발할 수 있다.

개선 방향: `spring.data.web.pageable.max-page-size` 설정으로 상한을 둔다.

### 6. `Order.status` 컬럼에 인덱스 없음

```java
// OrderRepository.java
Page<Order> findByStatus(OrderStatus status, Pageable pageable);
```

목록 API의 핵심 필터 조건인데 `Order.status`에 인덱스가 없어 데이터가 쌓이면 풀스캔이 발생한다.

개선 방향: `@Table(indexes = @Index(columnList = "status"))`를 `Order` 엔티티에 추가한다.

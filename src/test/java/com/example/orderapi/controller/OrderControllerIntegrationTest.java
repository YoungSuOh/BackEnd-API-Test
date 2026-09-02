package com.example.orderapi.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// data.sql로 상품 1(재고 100), 2(재고 200), 3(재고 50)이 미리 채워져 있다고 가정한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    // 정상적인 주문 생성 요청이 201 Created와 함께 계산된 총액/상태를 응답하는지 확인한다.
    @Test
    void createOrder_returnsCreatedWithTotalAmount() throws Exception {
        // given: 재고가 100개인 상품 1을 2개 담은 주문 요청 본문
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("customerName", "paul", "items",
                        java.util.List.of(java.util.Map.of("productId", 1, "quantity", 2))));

        // when: 주문 생성 API를 호출하면
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                // then: 201 Created, 고객명은 그대로, 총액은 (단가*수량)=90000원, 상태는 CREATED여야 한다
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("paul"))
                .andExpect(jsonPath("$.totalAmount").value(90000))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    // 재고보다 많은 수량을 주문하면 409 Conflict와 한글 에러 메시지("재고" 포함)를 반환하는지 확인한다.
    @Test
    void createOrder_returnsConflictWithKoreanMessageWhenStockInsufficient() throws Exception {
        // given: 재고가 50개뿐인 상품 3을 999개 담은(재고 초과) 주문 요청 본문
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("customerName", "paul", "items",
                        java.util.List.of(java.util.Map.of("productId", 3, "quantity", 999))));

        // when & then: 주문 생성 시 409 Conflict와 "재고"를 포함한 한글 메시지가 반환되어야 한다
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("재고")));
    }

    // 고객명이 빈 문자열이면(Bean Validation 위반) 400 Bad Request로 거절되는지 확인한다.
    @Test
    void createOrder_returnsBadRequestWhenCustomerNameBlank() throws Exception {
        // given: customerName이 빈 문자열인 요청 본문
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("customerName", "", "items",
                        java.util.List.of(java.util.Map.of("productId", 1, "quantity", 1))));

        // when & then: 요청 유효성 검증에 실패하여 400 Bad Request가 반환되어야 한다
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // 존재하지 않는 주문 id를 조회하면 404 Not Found와 한글 에러 메시지를 반환하는지 확인한다.
    @Test
    void getOrder_returnsNotFoundWithKoreanMessage() throws Exception {
        // when & then: DB에 있을 수 없는 큰 id로 조회하면 404와 "주문을 찾을 수 없습니다" 메시지가 반환되어야 한다
        mockMvc.perform(get("/api/orders/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("주문을 찾을 수 없습니다")));
    }

    // 정상 결제 후 같은 주문을 다시 결제하면 두 번째 요청이 409 Conflict로 거절되는지 E2E로 확인한다.
    @Test
    void payOrder_thenPayAgainReturnsConflict() throws Exception {
        // given: 재고가 200개인 상품 2를 1개 담아 주문을 생성하고, 응답에서 생성된 주문 id를 추출한다
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("customerName", "paul", "items",
                        java.util.List.of(java.util.Map.of("productId", 2, "quantity", 1))));

        String response = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("id").asLong();

        // when: 첫 번째 결제 요청은 정상 처리되어 상태가 PAID로 바뀌어야 하고
        mockMvc.perform(post("/api/orders/" + orderId + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // then: 같은 주문에 대한 두 번째 결제 요청은 409 Conflict로 거절되어야 한다(중복 결제 방지)
        mockMvc.perform(post("/api/orders/" + orderId + "/pay"))
                .andExpect(status().isConflict());
    }
}

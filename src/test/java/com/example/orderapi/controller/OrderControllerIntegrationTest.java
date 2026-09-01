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

    @Test
    void createOrder_returnsCreatedWithTotalAmount() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("customerName", "paul", "items",
                        java.util.List.of(java.util.Map.of("productId", 1, "quantity", 2))));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("paul"))
                .andExpect(jsonPath("$.totalAmount").value(90000))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createOrder_returnsConflictWithKoreanMessageWhenStockInsufficient() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("customerName", "paul", "items",
                        java.util.List.of(java.util.Map.of("productId", 3, "quantity", 999))));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("재고")));
    }

    @Test
    void createOrder_returnsBadRequestWhenCustomerNameBlank() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("customerName", "", "items",
                        java.util.List.of(java.util.Map.of("productId", 1, "quantity", 1))));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_returnsNotFoundWithKoreanMessage() throws Exception {
        mockMvc.perform(get("/api/orders/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("주문을 찾을 수 없습니다")));
    }

    @Test
    void payOrder_thenPayAgainReturnsConflict() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("customerName", "paul", "items",
                        java.util.List.of(java.util.Map.of("productId", 2, "quantity", 1))));

        String response = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/orders/" + orderId + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(post("/api/orders/" + orderId + "/pay"))
                .andExpect(status().isConflict());
    }
}

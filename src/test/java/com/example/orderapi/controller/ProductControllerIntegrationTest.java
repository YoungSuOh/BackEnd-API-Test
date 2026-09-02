package com.example.orderapi.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// data.sql로 상품 1(Keyboard), 2(Mouse), 3(Monitor)이 미리 채워져 있다고 가정한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // data.sql로 시딩된 상품 3건이 목록 조회 API에 그대로 노출되는지 확인한다.
    @Test
    void listProducts_returnsSeededProducts() throws Exception {
        // when: 전체 상품 목록을 조회하면
        mockMvc.perform(get("/api/products"))
                // then: 200 OK와 함께 시딩된 3개 상품이, id 순서대로 첫 번째는 Keyboard여야 한다
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Keyboard"));
    }
}

package com.example.orderapi.controller;

import com.example.orderapi.dto.ProductResponse;
import com.example.orderapi.repository.ProductRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 상품 목록 조회
    @GetMapping
    public List<ProductResponse> listProducts() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }
}

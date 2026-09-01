package com.example.orderapi.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusiness_usesStatusAndMessageFromException() {
        BusinessException exception = BusinessException.notFound("주문을 찾을 수 없습니다. id=1");

        ResponseEntity<ErrorResponse> response = handler.handleBusiness(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("주문을 찾을 수 없습니다. id=1");
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void handleBadRequest_returns400() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(new IllegalArgumentException("잘못된 값"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("잘못된 값");
    }
}

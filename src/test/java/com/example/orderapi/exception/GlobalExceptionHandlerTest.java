package com.example.orderapi.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// 전역 예외 핸들러(@ControllerAdvice)가 예외 종류에 맞는 HTTP 상태/응답 본문을 만드는지 검증한다.
// 컨트롤러나 서블릿 컨텍스트 없이 핸들러 메서드를 직접 호출하는 단위 테스트다.
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // BusinessException이 가진 상태 코드와 메시지를 그대로 응답에 반영하는지 확인한다.
    @Test
    void handleBusiness_usesStatusAndMessageFromException() {
        // given: "찾을 수 없음" 상황을 나타내는 BusinessException(404 매핑)
        BusinessException exception = BusinessException.notFound("주문을 찾을 수 없습니다. id=1");

        // when: 핸들러가 해당 예외를 처리하면
        ResponseEntity<ErrorResponse> response = handler.handleBusiness(exception);

        // then: 응답 상태는 404이고, 본문에는 예외 메시지와 status 값(404)이 그대로 담겨야 한다
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("주문을 찾을 수 없습니다. id=1");
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    // IllegalArgumentException처럼 예상치 못한 입력값 오류는 일괄적으로 400으로 변환되는지 확인한다.
    @Test
    void handleBadRequest_returns400() {
        // when: 잘못된 인자로 인한 IllegalArgumentException을 핸들러가 처리하면
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(new IllegalArgumentException("잘못된 값"));

        // then: 응답 상태는 400이고, 원래 예외 메시지를 포함해야 한다
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("잘못된 값");
    }
}

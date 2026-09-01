package com.example.orderapi.exception;

import org.springframework.http.HttpStatus;

// 도메인 규칙 위반 시 던지는 예외. HTTP 상태와 한글 상세 메시지를 함께 갖는다.
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}

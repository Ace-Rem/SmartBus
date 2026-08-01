package com.smartbus.backend.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super("BAD_REQUEST", message);
    }

    public BadRequestException(String errorCode, String message) {
        super(errorCode, message);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}

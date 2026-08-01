package com.smartbus.backend.exception;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}

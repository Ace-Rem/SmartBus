package com.smartbus.backend.exception;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}

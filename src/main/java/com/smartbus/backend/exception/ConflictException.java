package com.smartbus.backend.exception;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}

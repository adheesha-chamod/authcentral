package com.adheesha.app.exception;

import lombok.Getter;

@Getter
public class KeycloakException extends RuntimeException {

    private final int statusCode;

    public KeycloakException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}

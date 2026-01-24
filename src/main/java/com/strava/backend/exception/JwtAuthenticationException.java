package com.strava.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class JwtAuthenticationException extends RuntimeException {

    private final HttpStatus status;

    public JwtAuthenticationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}

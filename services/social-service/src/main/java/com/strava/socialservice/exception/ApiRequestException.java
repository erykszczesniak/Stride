package com.strava.socialservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Status-carrying business exception, ported from main. Rendered by
 * GlobalExceptionHandler as an RFC-7807 ProblemDetail with a "message"
 * property (branch error contract).
 */
@Getter
public class ApiRequestException extends RuntimeException {

    private final HttpStatus status;

    public ApiRequestException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}

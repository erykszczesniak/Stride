package com.strava.userservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Carries an arbitrary HTTP status, mirroring the monolith's exception of the same name.
 * Rendered by {@link GlobalExceptionHandler} as a ProblemDetail with a {@code message}
 * property so the frontend can read {@code err.response.data.message}.
 */
@Getter
public class ApiRequestException extends RuntimeException {

    private final HttpStatus status;

    public ApiRequestException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}

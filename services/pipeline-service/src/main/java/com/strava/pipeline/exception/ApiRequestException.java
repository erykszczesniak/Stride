package com.strava.pipeline.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Status-carrying business exception, ported from main. On /api/v1 it is
 * rendered by {@link GlobalExceptionHandler} as an RFC-7807 ProblemDetail with
 * a "message" property (branch error contract); on /pipeline/v1 the
 * controller-local handler renders the pipeline contract's
 * {@code {"code","message"}} body instead.
 */
@Getter
public class ApiRequestException extends RuntimeException {

    private final HttpStatus status;

    public ApiRequestException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}

package com.strava.webbff.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Malformed report dates ({@code dateFrom}/{@code dateTo} not yyyy-MM-dd). */
    @ExceptionHandler(DateTimeParseException.class)
    public ProblemDetail handleDateParse(DateTimeParseException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid date: " + e.getParsedString());
    }

    /** Unknown activityType and friends — {@code ActivityType.valueOf} like main. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) throws Exception {
        // Let Spring Security's translation produce 401/403 and the framework's
        // own ErrorResponse exceptions (404 for unmapped paths, 405, ...) keep
        // their status — swallowing them here would turn them into 500s.
        if (e instanceof AccessDeniedException || e instanceof AuthenticationException
                || e instanceof ErrorResponse) {
            throw e;
        }
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * RFC-7807 body plus a duplicate "message" property — the frontend reads
     * err.response.data.message, exactly like against the monolith.
     */
    private static ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("message", detail);
        return problem;
    }
}

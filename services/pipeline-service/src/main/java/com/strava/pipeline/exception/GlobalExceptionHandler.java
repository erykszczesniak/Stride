package com.strava.pipeline.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service-convention error shape for the /api/v1 surface (token management).
 * /pipeline/v1/** never reaches this advice — PipelineController declares a
 * local handler whose {@code {"code","message"}} body takes precedence.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiRequestException.class)
    public ProblemDetail handleApiRequest(ApiRequestException e) {
        return problem(HttpStatus.valueOf(e.getStatus().value()), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
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

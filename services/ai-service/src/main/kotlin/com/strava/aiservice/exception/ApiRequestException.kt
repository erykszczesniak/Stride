package com.strava.aiservice.exception

import org.springframework.http.HttpStatus

/**
 * Carries an HTTP status with the message — port of the monolith's
 * exception/ApiRequestException. Rendered by GlobalExceptionHandler as an
 * RFC-7807 ProblemDetail with the branch-wide extra `message` property.
 */
class ApiRequestException(message: String, val status: HttpStatus) : RuntimeException(message)

package com.strava.aiservice.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Domain errors carry their own status: 404 unknown/foreign activity, 503 unconfigured, 502 Claude failure. */
    @ExceptionHandler(ApiRequestException::class)
    fun handleApiRequest(e: ApiRequestException): ProblemDetail =
        problem(e.status, e.message ?: "Request failed")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ProblemDetail {
        val fieldErrors = linkedMapOf<String, String?>()
        e.bindingResult.fieldErrors.forEach { fieldErrors[it.field] = it.defaultMessage }
        val pd = problem(HttpStatus.BAD_REQUEST, "Validation failed")
        pd.setProperty("fieldErrors", fieldErrors)
        return pd
    }

    /**
     * Anything unmapped becomes a 500 with the monolith's generic message — main's
     * ApiExceptionHandler does the same for the RuntimeException a failed Groq call throws.
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ProblemDetail {
        log.error("Unexpected error", e)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
    }

    /**
     * RFC-7807 body plus a `message` property — the frontend reads
     * `err.response.data.message` (branch-wide convention).
     */
    private fun problem(status: HttpStatus, detail: String): ProblemDetail {
        val pd = ProblemDetail.forStatusAndDetail(status, detail)
        pd.setProperty("message", detail)
        return pd
    }
}

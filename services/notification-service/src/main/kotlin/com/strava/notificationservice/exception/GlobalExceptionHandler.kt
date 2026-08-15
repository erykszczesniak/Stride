package com.strava.notificationservice.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(e: NotFoundException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, e.message ?: "Not found")

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(e: ForbiddenException): ProblemDetail =
        problem(HttpStatus.FORBIDDEN, e.message ?: "Access denied")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ProblemDetail {
        val fieldErrors = linkedMapOf<String, String?>()
        e.bindingResult.fieldErrors.forEach { fieldErrors[it.field] = it.defaultMessage }
        val pd = problem(HttpStatus.BAD_REQUEST, "Validation failed")
        pd.setProperty("fieldErrors", fieldErrors)
        return pd
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

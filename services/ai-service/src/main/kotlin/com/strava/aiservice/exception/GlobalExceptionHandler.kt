package com.strava.aiservice.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ProblemDetail {
        val fieldErrors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed")
        problem.setProperty("fieldErrors", fieldErrors)
        return problem
    }
}

package com.strava.userservice.exception;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}

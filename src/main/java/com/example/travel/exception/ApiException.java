package com.example.travel.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatusCode status;
    private final String message;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public ApiException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public ApiException(String message, HttpStatus status, Throwable exception) {
        super(message, exception);
        this.status = status;
        this.message = message;
    }
}
package com.example.travel.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 리소스를 찾을 수 없는 경우
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException exception) {
        ErrorDetails errorDetails = new ErrorDetails(new Date(), exception.getMessage(), "RESOURCE_NOT_FOUND");
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    // 잘못된 요청인 경우
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDetails> handleBadRequestException(BadRequestException exception) {
        ErrorDetails errorDetails = new ErrorDetails(new Date(), exception.getMessage(), "BAD_REQUEST");
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // API 호출 예외
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorDetails> handleApiException(ApiException exception) {
        ErrorDetails errorDetails = new ErrorDetails(new Date(), exception.getMessage(), "API_ERROR");
        return new ResponseEntity<>(errorDetails, exception.getStatus());
    }

    // 외부 API 호출 예외
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorDetails> handleRestClientException(RestClientException exception) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                "외부 API 호출 중 오류가 발생했습니다: " + exception.getMessage(),
                "EXTERNAL_API_ERROR");
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 유효성 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // 기타 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception exception) {
        ErrorDetails errorDetails = new ErrorDetails(
                new Date(),
                "처리 중 오류가 발생했습니다: " + exception.getMessage(),
                "INTERNAL_SERVER_ERROR");
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 에러 상세 정보 클래스
    private static class ErrorDetails {
        @JsonProperty("timestamp")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private final Date timestamp;

        @JsonProperty("message")
        private final String message;

        @JsonProperty("errorCode")
        private final String errorCode;

        public ErrorDetails(Date timestamp, String message, String errorCode) {
            this.timestamp = timestamp;
            this.message = message;
            this.errorCode = errorCode;
        }
    }
}
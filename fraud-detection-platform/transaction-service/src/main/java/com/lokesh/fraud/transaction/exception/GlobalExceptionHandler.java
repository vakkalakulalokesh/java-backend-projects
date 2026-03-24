package com.lokesh.fraud.transaction.exception;

import com.lokesh.fraud.common.exception.FraudDetectionException;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorBody.of(HttpStatus.BAD_REQUEST.value(), message));
    }

    @ExceptionHandler(FraudDetectionException.class)
    public ResponseEntity<ErrorBody> handleFraud(FraudDetectionException ex) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(ErrorBody.of(status.value(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorBody.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error"));
    }

    @Data
    @Builder
    public static class ErrorBody {
        private Instant timestamp;
        private int status;
        private String message;

        static ErrorBody of(int status, String message) {
            return ErrorBody.builder()
                    .timestamp(Instant.now())
                    .status(status)
                    .message(message)
                    .build();
        }
    }
}

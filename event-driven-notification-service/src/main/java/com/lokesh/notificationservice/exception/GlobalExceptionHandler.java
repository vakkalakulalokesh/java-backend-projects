package com.lokesh.notificationservice.exception;

import com.lokesh.notificationservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {
        int status = ex.getStatusCode().value();
        ErrorResponse body = ErrorResponse.of(
                status,
                ex.getStatusCode().toString(),
                ex.getReason() != null ? ex.getReason() : "",
                request.getRequestURI(),
                List.of());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ErrorResponse> handleNotification(
            NotificationException ex,
            HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more fields are invalid",
                request.getRequestURI(),
                details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                request.getRequestURI(),
                List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

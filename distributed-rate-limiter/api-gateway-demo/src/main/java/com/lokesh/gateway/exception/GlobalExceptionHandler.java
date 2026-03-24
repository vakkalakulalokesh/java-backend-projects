package com.lokesh.gateway.exception;

import com.lokesh.gateway.dto.ErrorResponse;
import com.lokesh.ratelimiter.aspect.RateLimitAspect;
import com.lokesh.ratelimiter.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> rateLimited(RateLimitExceededException ex, HttpServletRequest request, HttpServletResponse response) {
        RateLimitAspect.writeHeaders(response, ex.getResult());
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generic(Exception ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Unexpected error",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

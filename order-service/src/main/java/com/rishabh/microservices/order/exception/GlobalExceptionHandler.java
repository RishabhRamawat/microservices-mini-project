package com.rishabh.microservices.order.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<String> handleNotFound(FeignException.NotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.contentUTF8());
    }

    @ExceptionHandler(FeignException.BadRequest.class)
    public ResponseEntity<String> handleBadRequest(FeignException.BadRequest ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.contentUTF8());
    }

    @ExceptionHandler(FeignException.Conflict.class)
    public ResponseEntity<String> handleConflict(FeignException.Conflict ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.contentUTF8());
    }

    // Unexpected downstream/network failures are surfaced as 502; no internal detail is forwarded.
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<String> handleFeignException(FeignException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Inventory service is unavailable. Please try again later.");
    }
}

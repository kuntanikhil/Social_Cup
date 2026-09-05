package com.socialcup.barista;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.socialcup.barista")
public class BaristaExceptionHandler {

    @ExceptionHandler(BaristaApiException.class)
    public ResponseEntity<BaristaFailureResponse> handle(
            BaristaApiException exception
    ) {
        return ResponseEntity.status(exception.getStatus())
                .body(BaristaFailureResponse.of(exception.getReason()));
    }
}

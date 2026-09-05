package com.socialcup.barista;

import org.springframework.http.HttpStatus;

public class BaristaApiException extends RuntimeException {

    private final HttpStatus status;
    private final BaristaFailureReason reason;

    public BaristaApiException(HttpStatus status, BaristaFailureReason reason) {
        super(reason.name());
        this.status = status;
        this.reason = reason;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public BaristaFailureReason getReason() {
        return reason;
    }
}

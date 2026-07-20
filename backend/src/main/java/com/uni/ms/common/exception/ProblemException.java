package com.uni.ms.common.exception;

import org.springframework.http.HttpStatus;

public class ProblemException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;

    public ProblemException(HttpStatus status, ErrorCode code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getCode() {
        return code;
    }
}

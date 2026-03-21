package com.example.exam.common.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    OK(0, HttpStatus.OK),
    VALIDATION_FAILED(40001, HttpStatus.BAD_REQUEST),
    BAD_REQUEST(40000, HttpStatus.BAD_REQUEST),
    PARAM_OUT_OF_RANGE(40002, HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, HttpStatus.NOT_FOUND),
    CONFLICT(40900, HttpStatus.CONFLICT),
    DUPLICATE(40901, HttpStatus.CONFLICT),
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final HttpStatus httpStatus;
}

package com.demo.common.api;

import lombok.Getter;

/**
 * 业务异常：用于业务条件不满足时主动抛出，由 GlobalExceptionHandler 转 Result.fail。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }
}

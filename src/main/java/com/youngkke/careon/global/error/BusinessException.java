package com.youngkke.careon.global.error;

import lombok.Getter;

/** 명세서의 비즈니스 에러 케이스를 표현하는 커스텀 예외. */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

package com.youngkke.careon.global.validation;

/** 여러 DTO에서 공통으로 쓰는 검증 정규식 모음. */
public final class ValidationPatterns {

    /** 영문+숫자 포함 8~20자 (특수문자 허용). */
    public static final String PASSWORD =
            "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]{8,20}$";

    private ValidationPatterns() {
    }
}

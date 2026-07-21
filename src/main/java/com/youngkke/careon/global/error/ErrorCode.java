package com.youngkke.careon.global.error;

import org.springframework.http.HttpStatus;

/**
 * API 명세서에 정의된 에러 케이스들. 새로운 엔드포인트를 구현할 때마다 필요한 항목을 추가한다.
 */
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),

    // Carer
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "재로그인이 필요합니다."),
    RESET_LINK_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 링크입니다. 다시 시도해주세요."),

    // Policy (회원가입 시 관심 제도 유형 참조용 - 명세서엔 없는, 데이터 무결성을 위해 추가한 에러)
    POLICY_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 제도 유형입니다."),

    // Policy
    MISSING_INTEREST_TYPE_IDS(HttpStatus.BAD_REQUEST, "조회 조건 (interestTypeIds) 이 필요합니다."),
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 제도입니다."),
    SAVED_POLICY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 저장한 제도입니다."),
    SAVED_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 저장 항목입니다."),

    // Todo
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 투두 항목입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}

package com.youngkke.careon.global.error;

import org.springframework.http.HttpStatus;

/**
 * API 명세서에 정의된 에러 케이스들. 새로운 엔드포인트를 구현할 때마다 필요한 항목을 추가한다.
 */
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "값이 올바르지 않습니다."),
    MISSING_INPUT_VALUE(HttpStatus.BAD_REQUEST, "값이 누락되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),

    // Carer
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "재로그인이 필요합니다."),
    RESET_LINK_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 링크입니다. 다시 시도해주세요."),
    CARER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    CARED_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 돌봄 대상자입니다."),
    INCOME_SIGNAL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 소득 추론 근거입니다."),

    // Policy
    POLICY_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 제도 유형입니다."),
    MISSING_INTEREST_TYPE_SELECTION(HttpStatus.BAD_REQUEST, "관심 제도 유형을 1개 이상 선택해주세요."),
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 제도입니다."),
    SAVED_POLICY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 저장한 제도입니다."),
    SAVED_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 저장 항목입니다."),
    MATCHED_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 매칭 제도입니다."),

    // Agency / Document
    AGENCY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 기관입니다."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 서류입니다."),
    DOCUMENT_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 서류 이력입니다."),

    // Todo
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 투두 항목입니다."),

    // Care task (워치 정기 안부·복약·할 일)
    CARE_TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 할 일입니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),

    // Wear (워치 연동)
    WEAR_PAIRING_CODE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 연결 코드입니다."),
    WEAR_DEVICE_NOT_FOUND(HttpStatus.UNAUTHORIZED, "워치 인증이 필요합니다."),
    WEAR_DEVICE_DISCONNECTED(HttpStatus.UNAUTHORIZED, "보호자가 워치 연결을 해제했습니다."),
    WEAR_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "워치 재인증이 필요합니다."),
    IDEMPOTENCY_KEY_MISSING(HttpStatus.BAD_REQUEST, "Idempotency-Key 헤더가 필요합니다."),
    EMERGENCY_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 긴급 이벤트입니다."),
    SAFE_ZONE_NOT_FOUND(HttpStatus.NOT_FOUND, "설정된 안심 구역이 없습니다."),
    SAFE_ZONE_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 안심 구역 이탈 이벤트입니다."),
    WEAR_DEVICE_NOT_PAIRED(HttpStatus.NOT_FOUND, "연결된 워치가 없습니다.");

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

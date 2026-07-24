package com.youngkke.careon.domain.policy;

/**
 * 제도(policy)의 신청 기간 유형.
 * FIXED: 신청 마감일이 정해져 있음 (application_deadline 존재).
 * ALWAYS_OPEN: 상시 신청 제도. 마감일 없음이 확인됨.
 * UNKNOWN: application_deadline이 null이지만, 상시 신청인지 마감일 정보가 누락된 것인지
 *          아직 데이터 파이프라인/운영에서 확인되지 않은 상태 (기본값).
 */
public enum ApplicationPeriodType {
    FIXED,
    ALWAYS_OPEN,
    UNKNOWN
}

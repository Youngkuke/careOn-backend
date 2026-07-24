package com.youngkke.careon.domain.policy;

/**
 * 저장한 제도(saved_policy)의 신청 상태.
 * PREPARING: 아직 신청 여부에 "예"로 응답하지 않은 상태 (서류 준비 중).
 * APPLIED: 모바일에서 "이 제도를 신청하셨나요?"에 "예"로 응답한 상태.
 */
public enum ApplicationStatus {
    PREPARING,
    APPLIED
}

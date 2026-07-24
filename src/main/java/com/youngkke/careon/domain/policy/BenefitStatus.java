package com.youngkke.careon.domain.policy;

/**
 * 저장한 제도(saved_policy)의 수혜 여부.
 * PENDING: 아직 결과를 모름 (기본값).
 * RECEIVED: 수혜받음.
 * NOT_RECEIVED: 수혜받지 못함.
 */
public enum BenefitStatus {
    PENDING,
    RECEIVED,
    NOT_RECEIVED
}

package com.youngkke.careon.domain.policy.dto;

/** 저장한 제도의 수혜 여부 수정 응답. */
public record BenefitStatusResponse(
        Integer savedPolicyId, String benefitStatus, String benefitCheckedAt, String message) {}

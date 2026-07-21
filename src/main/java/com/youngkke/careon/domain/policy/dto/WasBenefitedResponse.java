package com.youngkke.careon.domain.policy.dto;

/** 매칭 제도 수혜 여부 수정 응답. */
public record WasBenefitedResponse(Integer matchedPolicyId, Boolean wasBenefited, String message) {}

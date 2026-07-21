package com.youngkke.careon.domain.policy.dto;

/** 신청 여부 응답 결과. */
public record SavedPolicyAppliedResponse(Integer savedPolicyId, Boolean applied, String message) {}

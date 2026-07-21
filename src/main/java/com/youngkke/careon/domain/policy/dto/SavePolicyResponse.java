package com.youngkke.careon.domain.policy.dto;

/** 제도 저장 응답. */
public record SavePolicyResponse(Integer savedPolicyId, Integer policyId, Boolean applied, String message) {}

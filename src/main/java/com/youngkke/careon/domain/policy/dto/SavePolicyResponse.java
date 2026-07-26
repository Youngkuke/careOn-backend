package com.youngkke.careon.domain.policy.dto;

/** 제도 저장 응답. 저장한 대상에 따라 policyId 또는 servId 중 하나만 값이 있다. */
public record SavePolicyResponse(
        Integer savedPolicyId, Integer policyId, String servId, Boolean applied, String message) {}

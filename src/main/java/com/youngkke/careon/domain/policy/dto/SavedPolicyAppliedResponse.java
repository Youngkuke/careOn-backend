package com.youngkke.careon.domain.policy.dto;

import com.youngkke.careon.domain.policy.ApplicationStatus;

/** 신청 여부 응답 결과. */
public record SavedPolicyAppliedResponse(
        Integer savedPolicyId,
        boolean isApplied,
        ApplicationStatus applicationStatus,
        String appliedAt,
        String message) {}

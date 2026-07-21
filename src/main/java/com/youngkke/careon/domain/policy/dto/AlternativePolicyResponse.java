package com.youngkke.careon.domain.policy.dto;

import java.util.List;

/** 대안 복지 조회 응답 항목. (category=GENERAL 고정) */
public record AlternativePolicyResponse(
        Integer policyId,
        String policyName,
        Integer agencyId,
        String agencyName,
        String summary,
        String supportPeriod,
        String applicationDeadline,
        String link,
        String category,
        List<PolicyTypeSummary> policyTypes) {}

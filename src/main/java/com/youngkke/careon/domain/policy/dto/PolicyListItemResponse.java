package com.youngkke.careon.domain.policy.dto;

import java.util.List;

/** 제도 목록 조회 응답 항목. */
public record PolicyListItemResponse(
        Integer policyId,
        String policyName,
        Integer agencyId,
        String agencyName,
        String summary,
        String supportPeriod,
        String applicationDeadline,
        String category,
        List<PolicyTypeSummary> policyTypes) {}

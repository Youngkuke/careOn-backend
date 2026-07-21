package com.youngkke.careon.domain.policy.dto;

import com.youngkke.careon.domain.policy.Policy;

/** 대안 복지 조회 응답 항목. */
public record AlternativePolicyResponse(
        Integer policyId, String name, String organization, String content, String sourceUrl) {

    public static AlternativePolicyResponse from(Policy policy) {
        return new AlternativePolicyResponse(
                policy.getPolicyId(),
                policy.getPolicyName(),
                policy.getAgency().getAgencyName(),
                policy.getSummary(),
                policy.getLink());
    }
}

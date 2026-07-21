package com.youngkke.careon.domain.policy.dto;

import java.util.List;

/** 기관 목록/상세 조회 응답. */
public record AgencyResponse(Integer agencyId, String agencyName, List<AgencyPolicyItem> policies) {

    public record AgencyPolicyItem(Integer policyId, String policyName) {}

    /** 목록 조회용 (제도 목록 없이). */
    public static AgencyResponse ofSummary(Integer agencyId, String agencyName) {
        return new AgencyResponse(agencyId, agencyName, null);
    }
}

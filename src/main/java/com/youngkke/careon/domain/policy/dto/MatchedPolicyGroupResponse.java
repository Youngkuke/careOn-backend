package com.youngkke.careon.domain.policy.dto;

import java.util.List;

/** 맞춤 지원 제도 목록 조회 응답. 유형별로 그룹핑되며, 같은 제도는 대표 유형 그룹 1곳에만 포함된다. */
public record MatchedPolicyGroupResponse(
        Integer policyTypeId, String typeName, List<MatchedPolicyItem> policies) {

    public record MatchedPolicyItem(
            Integer matchedPolicyId,
            Integer policyId,
            String matchGroup,
            Boolean wasBenefited,
            String policyName,
            Integer agencyId,
            String agencyName,
            String summary,
            String supportPeriod,
            String applicationDeadline,
            String link,
            List<PolicyTypeSummary> policyTypes) {}
}
